package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.calc.MetCalc
import pt.antares.app.core.database.daos.ExerciseLogDao
import pt.antares.app.core.database.daos.RoutineDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.daos.WorkoutSetDao
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.toEpochDay

class WorkoutSessionRepository(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val weightLogDao: WeightLogDao,
    private val routineDao: RoutineDao,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()

    fun observeActive(): Flow<WorkoutSessionEntity?> = sessionDao.observeActive()

    suspend fun activeSession(): WorkoutSessionEntity? = withContext(io) { sessionDao.activeSession() }

    fun observeSets(sessionId: String): Flow<List<WorkoutSetEntity>> = setDao.observeSetsForSession(sessionId)

    suspend fun sessionById(id: String): WorkoutSessionEntity? = withContext(io) { sessionDao.sessionById(id) }

    /**
     * Devolve o treino a decorrer se houver um, em vez de começar outro. É o que faz sair
     * da app a meio de um treino e voltar continuar onde se estava — e o que impede dois
     * treinos abertos ao mesmo tempo, que a base por si não proíbe.
     */
    suspend fun startOrResume(routineId: String?): String = withContext(io) {
        sessionDao.activeSession()?.let { return@withContext it.id }
        val id = Ids.newUuid()
        sessionDao.upsertSession(
            WorkoutSessionEntity(
                id = id,
                startedAt = now(),
                endedAt = null,
                routineId = routineId,
                note = null,
                status = SessionStatus.ACTIVE,
                updatedAt = now(),
            ),
        )
        id
    }

    suspend fun ghostSets(exerciseId: String, currentSessionId: String): List<WorkoutSetEntity> =
        withContext(io) { setDao.ghostSets(exerciseId, currentSessionId) }

    suspend fun putSet(
        id: String,
        sessionId: String,
        exerciseId: String,
        setIndex: Int,
        weightKg: Double,
        reps: Int,
        rpe: Double?,
        isWarmup: Boolean,
    ) = withContext(io) {
        setDao.upsertSet(
            WorkoutSetEntity(
                id = id,
                sessionId = sessionId,
                exerciseId = exerciseId,
                setIndex = setIndex,
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                isWarmup = isWarmup,
                updatedAt = now(),
            ),
        )
    }

    suspend fun deleteSet(id: String) = withContext(io) { setDao.softDeleteSet(id, now()) }

    suspend fun setsForSession(sessionId: String): List<WorkoutSetEntity> =
        withContext(io) { setDao.setsForSession(sessionId) }

    suspend fun doneSetsForExercise(exerciseId: String, excludeSessionId: String): List<WorkoutSetEntity> =
        withContext(io) { setDao.doneSetsForExercise(exerciseId, excludeSessionId) }

    /**
     * Fecha o treino e, se ele valeu alguma coisa, gera a linha de calorias do dia. São
     * dois passos e não um: o treino fica sempre fechado, mesmo que não conte para o
     * orçamento.
     */
    suspend fun finish(sessionId: String) = withContext(io) {
        val s = sessionDao.sessionById(sessionId) ?: return@withContext
        val ended = now()
        sessionDao.upsertSession(s.copy(status = SessionStatus.DONE, endedAt = ended, updatedAt = ended, dirty = true))

        // Um treino aberto por engano e fechado a seguir, ou só com aquecimento, não gera
        // calorias nenhumas — mas fica gravado, porque aconteceu.
        val durationMin = ((ended - s.startedAt) / 60000L).toInt()
        val workingSets = setDao.setsForSession(sessionId).count { !it.isWarmup }
        if (durationMin < 1 || workingSets == 0) return@withContext

        // MET fixo de 5 para musculação em geral. A app não sabe quanto se descansou entre
        // séries, e afinar isto daria uma precisão que os dados não sustentam.
        val weightKg = weightLogDao.latest()?.weightKg ?: 70.0
        val kcal = MetCalc.kcal(met = 5.0, weightKg = weightKg, durationMin = durationMin)

        val label = s.routineId?.let { routineDao.routineById(it)?.name } ?: ""
        exerciseLogDao.upsert(
            ExerciseLogEntity(
                id = Ids.newUuid(),
                // O dia é o do início e não o do fim: um treino que atravessa a meia-noite
                // pertence ao dia em que se começou a treinar.
                epochDay = epochMillisToLocalDate(s.startedAt).toEpochDay(),
                origin = ExerciseOrigin.WORKOUT,
                label = label,
                metId = null,
                met = 5.0,
                durationMin = durationMin,
                kcal = kcal,
                refId = sessionId,
                updatedAt = ended,
            ),
        )
    }

    suspend fun discard(sessionId: String) = withContext(io) {
        val s = sessionDao.sessionById(sessionId) ?: return@withContext
        sessionDao.upsertSession(s.copy(status = SessionStatus.DISCARDED, endedAt = now(), updatedAt = now(), dirty = true))
    }
}
