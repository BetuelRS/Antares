package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus

data class MuscleVolumeRow(
    val weightKg: Double,
    val reps: Int,
    val primaryMuscles: String,
    val startedAt: Long,
)

data class ExerciseSessionAgg(
    val sessionId: String,
    val startedAt: Long,
    val volume: Double,
    val topWeight: Double,
)

data class SessionVolumeRow(val sessionId: String, val volume: Double)

data class ExerciseSetRow(val exerciseId: String, val weightKg: Double, val reps: Int)

@Dao
interface RoutineDao {

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Upsert
    suspend fun upsertItem(item: RoutineItemEntity)

    @Upsert
    suspend fun upsertItems(items: List<RoutineItemEntity>)

    @Query("SELECT * FROM routine WHERE deleted = 0 ORDER BY position")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine WHERE id = :id AND deleted = 0")
    suspend fun routineById(id: String): RoutineEntity?

    // Sem filtro de apagados: quem está no ecrã de uma rotina e a apaga precisa de ver o
    // fluxo emitir a linha marcada, para fechar o ecrã em vez de ficar num nulo súbito.
    @Query("SELECT * FROM routine WHERE id = :id")
    fun observeRoutine(id: String): Flow<RoutineEntity?>

    @Query("SELECT * FROM routine_item WHERE id = :id")
    suspend fun itemById(id: String): RoutineItemEntity?

    @Query("SELECT * FROM routine_item WHERE routineId = :routineId AND deleted = 0 ORDER BY position")
    fun observeItems(routineId: String): Flow<List<RoutineItemEntity>>

    @Query("SELECT * FROM routine_item WHERE routineId = :routineId AND deleted = 0 ORDER BY position")
    suspend fun itemsOf(routineId: String): List<RoutineItemEntity>

    @Query("UPDATE routine SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteRoutine(id: String, now: Long)

    @Query("UPDATE routine_item SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteItem(id: String, now: Long)

    @Query("SELECT COUNT(*) FROM routine WHERE deleted = 0")
    suspend fun countRoutines(): Int

    @Query("SELECT * FROM routine WHERE deleted = 0")
    suspend fun exportRows(): List<RoutineEntity>

    @Query("SELECT * FROM routine_item WHERE deleted = 0")
    suspend fun exportItems(): List<RoutineItemEntity>
}

@Dao
interface WorkoutSessionDao {

    @Upsert
    suspend fun upsertSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun sessionById(id: String): WorkoutSessionEntity?

    // Um treino a decorrer de cada vez. Nada impede a base de ter dois — só o `LIMIT 1` com
    // a ordem descendente garante que o mais recente ganha se isso acontecer.
    @Query("SELECT * FROM workout_session WHERE status = 'ACTIVE' AND deleted = 0 ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_session WHERE status = 'ACTIVE' AND deleted = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSession(): WorkoutSessionEntity?

    @Query(
        "SELECT * FROM workout_session WHERE status = :status AND deleted = 0 ORDER BY startedAt DESC",
    )
    fun observeByStatus(status: SessionStatus): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session WHERE deleted = 0 AND endedAt IS NOT NULL AND endedAt >= :fromMs")
    suspend fun endedSince(fromMs: Long): List<WorkoutSessionEntity>

    @Query(
        "SELECT * FROM workout_session WHERE deleted = 0 AND status = 'DONE' " +
            "AND startedAt BETWEEN :fromMs AND :toMs",
    )
    suspend fun sessionsBetween(fromMs: Long, toMs: Long): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_session WHERE deleted = 0")
    suspend fun exportRows(): List<WorkoutSessionEntity>
}

/**
 * As consultas de agregação daqui para baixo repetem sempre a mesma cláusula:
 * `status = 'DONE'`, as duas tabelas não apagadas, e `isWarmup = 0`. É a definição de
 * trabalho contado na app — treinos por acabar e séries de aquecimento não entram em
 * volume, recordes nem progresso.
 */
@Dao
interface WorkoutSetDao {

    @Upsert
    suspend fun upsertSet(set: WorkoutSetEntity)

    // Agrupa por exercício antes do número da série: a lista do treino mostra-se por
    // exercício, e a ordem da base poupa o agrupamento em memória a cada emissão.
    @Query("SELECT * FROM workout_set WHERE sessionId = :sessionId AND deleted = 0 ORDER BY exerciseId, setIndex")
    fun observeSetsForSession(sessionId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_set WHERE sessionId = :sessionId AND deleted = 0 ORDER BY exerciseId, setIndex")
    suspend fun setsForSession(sessionId: String): List<WorkoutSetEntity>

    @Query("UPDATE workout_set SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteSet(id: String, now: Long)

    /**
     * As séries da última vez que este exercício foi feito, para aparecerem a cinzento por
     * baixo dos campos vazios. A subconsulta encontra essa sessão; excluir a atual é o que
     * impede o treino em curso de se copiar a si mesmo enquanto se escreve nele.
     */
    @Query(
        """
        SELECT * FROM workout_set
        WHERE exerciseId = :exerciseId AND deleted = 0 AND isWarmup = 0
          AND sessionId = (
            SELECT ws.id FROM workout_session ws
            JOIN workout_set s ON s.sessionId = ws.id
            WHERE ws.status = 'DONE' AND ws.deleted = 0
              AND s.exerciseId = :exerciseId AND ws.id != :currentSessionId
            ORDER BY ws.startedAt DESC LIMIT 1
          )
        ORDER BY setIndex
        """,
    )
    suspend fun ghostSets(exerciseId: String, currentSessionId: String): List<WorkoutSetEntity>

    @Query(
        """
        SELECT s.* FROM workout_set s
        JOIN workout_session ws ON s.sessionId = ws.id
        WHERE s.exerciseId = :exerciseId AND s.deleted = 0 AND s.isWarmup = 0
          AND ws.status = 'DONE' AND ws.deleted = 0 AND ws.id != :excludeSessionId
        """,
    )
    // Alimenta a deteção de recordes: exclui-se a sessão em curso para o recorde anterior
    // não incluir a série que se acabou de escrever — senão nada seria nunca recorde.
    suspend fun doneSetsForExercise(exerciseId: String, excludeSessionId: String): List<WorkoutSetEntity>

    @Query(
        """
        SELECT ws.id AS sessionId, ws.startedAt AS startedAt,
               COALESCE(SUM(s.weightKg * s.reps), 0) AS volume,
               COALESCE(MAX(s.weightKg), 0) AS topWeight
        FROM workout_session ws
        JOIN workout_set s ON s.sessionId = ws.id
        WHERE s.exerciseId = :exerciseId AND ws.status = 'DONE'
          AND ws.deleted = 0 AND s.deleted = 0 AND s.isWarmup = 0
        GROUP BY ws.id ORDER BY ws.startedAt
        """,
    )
    suspend fun exerciseProgress(exerciseId: String): List<ExerciseSessionAgg>

    // Junta-se à tabela de exercícios em vez de guardar os músculos na série: o volume por
    // músculo tem de acompanhar o catálogo, e uma cópia de anos atrás nunca se corrigiria.
    @Query(
        """
        SELECT s.weightKg AS weightKg, s.reps AS reps,
               e.primaryMuscles AS primaryMuscles, ws.startedAt AS startedAt
        FROM workout_set s
        JOIN workout_session ws ON s.sessionId = ws.id
        JOIN exercise e ON s.exerciseId = e.id
        WHERE ws.status = 'DONE' AND ws.deleted = 0 AND s.deleted = 0 AND s.isWarmup = 0
          AND ws.startedAt >= :since
        """,
    )
    fun observeMuscleVolumeSince(since: Long): Flow<List<MuscleVolumeRow>>

    @Query(
        """
        SELECT s.sessionId AS sessionId, COALESCE(SUM(s.weightKg * s.reps), 0) AS volume
        FROM workout_set s
        JOIN workout_session ws ON s.sessionId = ws.id
        WHERE ws.status = 'DONE' AND ws.deleted = 0 AND s.deleted = 0 AND s.isWarmup = 0
        GROUP BY s.sessionId
        """,
    )
    suspend fun sessionVolumes(): List<SessionVolumeRow>

    @Query(
        """
        SELECT COALESCE(SUM(s.weightKg * s.reps), 0)
        FROM workout_set s
        JOIN workout_session ws ON s.sessionId = ws.id
        WHERE ws.status = 'DONE' AND ws.deleted = 0 AND s.deleted = 0 AND s.isWarmup = 0
          AND ws.startedAt BETWEEN :fromMs AND :toMs
        """,
    )
    suspend fun volumeBetween(fromMs: Long, toMs: Long): Double

    @Query(
        """
        SELECT s.exerciseId AS exerciseId, s.weightKg AS weightKg, s.reps AS reps
        FROM workout_set s
        JOIN workout_session ws ON s.sessionId = ws.id
        WHERE ws.status = 'DONE' AND ws.deleted = 0 AND s.deleted = 0 AND s.isWarmup = 0
        """,
    )
    suspend fun allDoneWorkingSets(): List<ExerciseSetRow>

    @Query("SELECT * FROM workout_set WHERE deleted = 0")
    suspend fun exportRows(): List<WorkoutSetEntity>
}
