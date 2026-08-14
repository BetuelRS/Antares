package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import pt.antares.app.core.calc.MuscleVolumeInput
import pt.antares.app.core.calc.OneRepMax
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.daos.WorkoutSetDao
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus

data class SessionSummary(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val volume: Double,
)

data class SessionBreakdown(
    val startedAt: Long,
    val durationMin: Int,
    val volume: Double,
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
    private val io: CoroutineDispatcher,
) {

    fun observeHistory(): Flow<List<SessionSummary>> =
        sessionDao.observeByStatus(SessionStatus.DONE).mapLatest { sessions ->
            val vols = setDao.sessionVolumes().associate { it.sessionId to it.volume }
            sessions.map {
                SessionSummary(it.id, it.startedAt, it.endedAt, vols[it.id] ?: 0.0)
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
        val duration = ((session.endedAt ?: session.startedAt) - session.startedAt).let { (it / 60000L).toInt() }
        SessionBreakdown(session.startedAt, duration.coerceAtLeast(0), volume, exercises)
    }

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
