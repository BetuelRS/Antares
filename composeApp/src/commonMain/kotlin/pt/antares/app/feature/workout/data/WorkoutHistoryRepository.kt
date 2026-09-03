package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import pt.antares.app.core.calc.MuscleVolumeInput
import pt.antares.app.core.calc.OneRepMax
import pt.antares.app.core.calc.RecordesPorTreino
import pt.antares.app.core.calc.SerieDeTreino
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.daos.RoutineDao
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.daos.WorkoutSetDao
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus

data class SessionSummary(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val volume: Double,

    /** Nulo num treino livre, que não nasceu de rotina nenhuma. */
    val routineId: String? = null,
    val nomeDaRotina: String? = null,

    val durationMin: Int = 0,
    val series: Int = 0,

    /**
     * Se algum exercício deste treino bateu o seu melhor **até àquele dia**. Calculado, e não
     * guardado — ver o `RecordesPorTreino`.
     */
    val temRecorde: Boolean = false,
)

/** Uma rotina que aparece no histórico, para o filtro. */
data class RoutineOption(val id: String, val name: String)

data class SessionBreakdown(
    val startedAt: Long,
    val nomeDaRotina: String?,
    val durationMin: Int,
    val volume: Double,
    val series: Int,
    val exercises: List<BreakdownExercise>,
)

data class BreakdownExercise(
    val id: String,
    val name: String,
    val sets: List<WorkoutSetEntity>,
)

data class ExerciseRecord(val name: String, val oneRm: Double)

data class MuscleVolumeStat(val muscle: String, val volume: Double)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryRepository(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao,
    private val exerciseDao: ExerciseLibraryDao,
    private val routineDao: RoutineDao,
    private val io: CoroutineDispatcher,
) {

    /**
     * A linha do histórico tinha dois dados — a data e o volume — e dois treinos
     * completamente diferentes ficavam iguais. Passa a ter quatro, e nenhum deles é novo: o
     * `WorkoutHubRepository` já os montava na 2.20.0 para os três «últimos treinos» do
     * painel, e a consulta das séries diz de si própria que é «o quarto dado da linha do
     * histórico».
     */
    fun observeHistory(): Flow<List<SessionSummary>> =
        combine(
            sessionDao.observeByStatus(SessionStatus.DONE),
            setDao.observeSetCounts(),
        ) { sessions, contagens -> sessions to contagens }
            .mapLatest { (sessions, contagens) ->
                val vols = setDao.sessionVolumes().associate { it.sessionId to it.volume }
                // Uma consulta por facto para a lista toda, e não uma por treino: com
                // duzentos treinos gravados, o segundo caminho são centenas de idas à base
                // para desenhar uma lista que já estava desenhada.
                val series = contagens.associate { it.sessionId to it.total }
                val nomes = routineDao.allRoutineNames().associate { it.id to it.name }
                val comRecorde = RecordesPorTreino.comRecorde(
                    setDao.doneWorkingSetsByTime().map {
                        SerieDeTreino(it.sessionId, it.exerciseId, it.weightKg, it.reps)
                    },
                )
                sessions.map {
                    SessionSummary(
                        id = it.id,
                        startedAt = it.startedAt,
                        endedAt = it.endedAt,
                        volume = vols[it.id] ?: 0.0,
                        routineId = it.routineId,
                        nomeDaRotina = it.routineId?.let { id -> nomes[id] },
                        durationMin = duracaoMin(it.startedAt, it.endedAt),
                        series = series[it.id] ?: 0,
                        temRecorde = it.id in comRecorde,
                    )
                }
            }

    suspend fun breakdown(sessionId: String): SessionBreakdown? = withContext(io) {
        val session = sessionDao.sessionById(sessionId) ?: return@withContext null
        val sets = setDao.setsForSession(sessionId)
        val names = exerciseDao.namesByIds(sets.map { it.exerciseId }.distinct())
            .associate { it.id to it.namePt.ifBlank { it.nameEn } }
        val exercises = sets.groupBy { it.exerciseId }.map { (exId, exSets) ->
            BreakdownExercise(exId, names[exId] ?: exId, exSets.sortedBy { it.setIndex })
        }
        val volume = sets.filter { !it.isWarmup }.sumOf { it.weightKg * it.reps }
        SessionBreakdown(
            startedAt = session.startedAt,
            // Um nome só, e vai buscá-lo à consulta que vê as lápides: abrir um treino de há
            // três meses tem de dizer com que rotina foi feito, mesmo que ela já não exista.
            nomeDaRotina = session.routineId?.let { routineDao.routineNameById(it) },
            durationMin = duracaoMin(session.startedAt, session.endedAt),
            volume = volume,
            // Séries de trabalho, como em toda a app: o aquecimento não conta.
            series = sets.count { !it.isWarmup },
            exercises = exercises,
        )
    }

    /**
     * As rotinas que aparecem no histórico, por ordem alfabética. Só as que foram treinadas:
     * uma rotina que nunca saiu do editor não filtra nada, e um menu com opções que devolvem
     * sempre lista vazia é pior do que um menu mais curto.
     */
    suspend fun routineOptions(): List<RoutineOption> = withContext(io) {
        val usadas = sessionDao.doneRoutineIds().toSet()
        routineDao.allRoutineNames()
            .filter { it.id in usadas }
            .map { RoutineOption(it.id, it.name) }
            .sortedBy { it.name }
    }

    // Um treino por acabar não tem duração: `endedAt` nulo dá zero, e não o tempo que passou
    // desde que começou — esse é o relógio da sessão, e é outra pergunta.
    private fun duracaoMin(startedAt: Long, endedAt: Long?): Int =
        (((endedAt ?: startedAt) - startedAt) / MS_POR_MINUTO).toInt().coerceAtLeast(0)

    fun observeMuscleVolume(since: Long): Flow<List<MuscleVolumeStat>> =
        setDao.observeMuscleVolumeSince(since).mapLatest { rows ->
            val inputs = rows.map {
                MuscleVolumeInput(it.weightKg, it.reps, ExerciseSeeder.unwrap(it.primaryMuscles))
            }
            pt.antares.app.core.calc.MuscleVolume.aggregate(inputs)
                .map { (m, v) -> MuscleVolumeStat(m, v) }
                .sortedByDescending { it.volume }
        }

    suspend fun exerciseVolumeSeries(exerciseId: String): List<Float> = withContext(io) {
        setDao.exerciseProgress(exerciseId).map { it.volume.toFloat() }
    }

    /**
     * O melhor 1RM estimado de cada exercício, os mais pesados primeiro. Ordenar por carga
     * absoluta faz o agachamento e o peso morto ficarem sempre no topo — é a ordem certa
     * para um quadro de recordes, mas não diz nada sobre onde houve mais progresso.
     */
    suspend fun records(limit: Int = 12): List<ExerciseRecord> = withContext(io) {
        val byExercise = setDao.allDoneWorkingSets().groupBy { it.exerciseId }
        val bests = byExercise.mapNotNull { (exId, rows) ->
            // Exercícios cujas séries passam todas das doze repetições ficam de fora: a
            // Epley não os estima, e um 1RM inventado dali não valeria nada.
            val best = rows.mapNotNull { OneRepMax.epley(it.weightKg, it.reps) }.maxOrNull()
            best?.let { exId to it }
        }
        val names = exerciseDao.namesByIds(bests.map { it.first })
            .associate { it.id to it.namePt.ifBlank { it.nameEn } }
        bests.sortedByDescending { it.second }
            .take(limit)
            .map { ExerciseRecord(names[it.first] ?: it.first, it.second) }
    }
}

// Os minutos são a unidade da duração em toda a app — a linha do histórico, o cabeçalho
// do detalhe e os últimos treinos do painel dizem-na todos assim.
private const val MS_POR_MINUTO = 60_000L
