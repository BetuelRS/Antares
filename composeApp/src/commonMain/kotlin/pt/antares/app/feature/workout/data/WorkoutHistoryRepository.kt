package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import pt.antares.app.core.calc.FrequenciaDeTreino
import pt.antares.app.core.calc.MuscleVolumeInput
import pt.antares.app.core.calc.OneRepMax
import pt.antares.app.core.calc.RecordesPorTreino
import pt.antares.app.core.calc.SerieDeTreino
import pt.antares.app.core.calc.SeriesPorMusculo
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.weekStartEpochDay
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

/**
 * Um recorde, com o dia em que aconteceu.
 *
 * O dia é o defeito concreto 4 da `estudo/areas/10`: sem ele, um recorde de 2024 aparece
 * igual a um de ontem, e a lista deixa de dizer onde houve progresso.
 */
data class ExerciseRecord(
    val name: String,
    val oneRm: Double,
    val epochDay: Long,
)

/**
 * Quantas séries um músculo levou, quantas isso dá por semana, e o volume que elas somaram.
 *
 * **A barra mede séries e não volume**, que é o que o esboço 10 desenha: o volume não é
 * comparável entre grupos musculares — um dia de pernas tem sempre mais do que um de braços —
 * e como barra fazia a alternância do plano parecer desequilíbrio. O volume fica na mesma
 * linha porque a `estudo/areas/10` lhe chama «a estatística certa» para saber se o treino
 * está equilibrado; o que muda é qual dos dois números manda no comprimento.
 *
 * [porSemana] é nulo quando o período escolhido é mais curto do que uma semana: a faixa de
 * referência é semanal, e esticar um dia até lá era inventar seis dias.
 */
data class MusculoNaSemana(
    val musculo: String,
    val series: Int,
    val porSemana: Int?,
    val volume: Double,
)

/**
 * O que o ecrã de estatísticas mostra, para o período escolhido.
 *
 * As séries e o volume saem da **mesma leitura** — a consulta traz peso, repetições e os
 * músculos de cada série —, e por isso contar as duas coisas não custa uma segunda ida à base.
 */
data class EstatisticasDoTreino(
    val loading: Boolean = true,
    val musculos: List<MusculoNaSemana> = emptyList(),
    val volumePorSemana: List<Double> = emptyList(),
    val treinosPorSemana: List<Int> = emptyList(),
    val mediaDeTreinos: Double = 0.0,
    val treinosNoPeriodo: Int = 0,
)

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

    /**
     * As estatísticas do período escolhido.
     *
     * **A semana é a ISO em toda a parte**, como no painel de treino, no relatório do
     * treinador e na grelha do progresso. Este ecrã contava sete dias para trás a partir de
     * agora, e por isso «esta semana» queria dizer duas coisas dentro do mesmo separador.
     *
     * As séries e o volume vêm da mesma leitura: a consulta já trazia o peso, as repetições e
     * os músculos de cada série, e o que faltava era contá-las em vez de só as multiplicar.
     */
    fun observeEstatisticas(
        desdeMs: Long,
        diasDoPeriodo: Int,
        hojeEpochDay: Long,
        semanas: Int,
    ): Flow<EstatisticasDoTreino> = combine(
        setDao.observeMuscleVolumeSince(desdeMs),
        sessionDao.observeDoneStarts(),
    ) { series, iniciosMs ->

        val musculosPorSerie = series.map { ExerciseSeeder.unwrap(it.primaryMuscles) }
        val volumes = pt.antares.app.core.calc.MuscleVolume.aggregate(
            series.map {
                MuscleVolumeInput(it.weightKg, it.reps, ExerciseSeeder.unwrap(it.primaryMuscles))
            },
        )
        val musculos = SeriesPorMusculo.contar(musculosPorSerie)
            .map { (m, n) ->
                MusculoNaSemana(
                    musculo = m,
                    series = n,
                    porSemana = SeriesPorMusculo.porSemana(n, diasDoPeriodo),
                    volume = volumes[m] ?: 0.0,
                )
            }
            .sortedByDescending { it.series }

        val iniciosDia = iniciosMs.map { epochMillisToLocalDate(it).toEpochDays().toLong() }
        val treinosPorSemana = FrequenciaDeTreino.porSemana(iniciosDia, hojeEpochDay, semanas)

        // O volume por semana usa as mesmas semanas ISO do gráfico da frequência, para os dois
        // se lerem um por baixo do outro sem terem de coincidir por acaso.
        val porSemana = DoubleArray(semanas)
        val estaSemana = weekStartEpochDay(hojeEpochDay)
        val primeira = estaSemana - (semanas - 1) * DIAS_POR_SEMANA
        for (s in series) {
            val dia = epochMillisToLocalDate(s.startedAt).toEpochDays().toLong()
            val indice = ((weekStartEpochDay(dia) - primeira) / DIAS_POR_SEMANA).toInt()
            if (indice in 0 until semanas) porSemana[indice] += s.weightKg * s.reps
        }

        EstatisticasDoTreino(
            loading = false,
            musculos = musculos,
            volumePorSemana = porSemana.toList(),
            treinosPorSemana = treinosPorSemana,
            mediaDeTreinos = FrequenciaDeTreino.media(treinosPorSemana),
            // A contagem é a do **período**, e não a soma das semanas ISO que o cobrem: em
            // «Dia» as duas discordavam, e o cartão dizia «1 no período escolhido» por cima
            // de «Sem séries no período escolhido». A semana ISO é a unidade do gráfico, e
            // arredondar um dia para a semana inteira dava-lhe treinos que não são dele.
            treinosNoPeriodo = iniciosMs.count { it >= desdeMs },
        )
    }

    suspend fun exerciseVolumeSeries(exerciseId: String): List<Float> = withContext(io) {
        setDao.exerciseProgress(exerciseId).map { it.volume.toFloat() }
    }

    /**
     * O melhor 1RM estimado de cada exercício, **com o dia em que foi feito**, os mais
     * pesados primeiro. Ordenar por carga absoluta faz o agachamento e o peso morto ficarem
     * sempre no topo — é a ordem certa para um quadro de recordes, mas não diz nada sobre
     * onde houve mais progresso, e é por isso que a data passa a viajar ao lado.
     *
     * A data é a do treino em que a melhor série foi gravada. Quando o mesmo 1RM se repete,
     * fica a **primeira** vez: um recorde é quando se chegou lá, e não a última vez que se
     * empatou com ele.
     */
    suspend fun records(limit: Int = 12): List<ExerciseRecord> = withContext(io) {
        val byExercise = setDao.doneWorkingSetsByTime().groupBy { it.exerciseId }
        val bests = byExercise.mapNotNull { (exId, rows) ->
            // Exercícios cujas séries passam todas das doze repetições ficam de fora: a
            // Epley não os estima, e um 1RM inventado dali não valeria nada.
            val melhor = rows
                .mapNotNull { linha ->
                    OneRepMax.epley(linha.weightKg, linha.reps)?.let { it to linha.startedAt }
                }
                // As linhas já vêm por ordem de treino, e o `maxByOrNull` fica com a primeira
                // das iguais — que é o dia em que o recorde aconteceu.
                .maxByOrNull { it.first }
            melhor?.let { Triple(exId, it.first, it.second) }
        }
        val names = exerciseDao.namesByIds(bests.map { it.first })
            .associate { it.id to it.namePt.ifBlank { it.nameEn } }
        bests.sortedByDescending { it.second }
            .take(limit)
            .map {
                ExerciseRecord(
                    name = names[it.first] ?: it.first,
                    oneRm = it.second,
                    epochDay = epochMillisToLocalDate(it.third).toEpochDays().toLong(),
                )
            }
    }
}

// Os minutos são a unidade da duração em toda a app — a linha do histórico, o cabeçalho
// do detalhe e os últimos treinos do painel dizem-na todos assim.
private const val MS_POR_MINUTO = 60_000L

private const val DIAS_POR_SEMANA = 7L
