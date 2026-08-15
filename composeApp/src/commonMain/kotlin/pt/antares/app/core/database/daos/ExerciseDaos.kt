package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.ExerciseLogEntity

@Dao
interface ExerciseLogDao {

    @Upsert
    suspend fun upsert(log: ExerciseLogEntity)

    @Query("UPDATE exercise_log SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM exercise_log WHERE id = :id AND deleted = 0")
    suspend fun byId(id: String): ExerciseLogEntity?

    // Encontra a linha de calorias que um treino ou corrida gerou, para a atualizar quando
    // a origem muda em vez de acumular duas contagens do mesmo esforço.
    @Query("SELECT * FROM exercise_log WHERE refId = :refId AND deleted = 0 LIMIT 1")
    suspend fun byRef(refId: String): ExerciseLogEntity?

    @Query("SELECT * FROM exercise_log WHERE deleted = 0 AND epochDay = :epochDay ORDER BY updatedAt ASC")
    fun observeDay(epochDay: Long): Flow<List<ExerciseLogEntity>>

    @Query("SELECT COALESCE(SUM(kcal), 0) FROM exercise_log WHERE deleted = 0 AND epochDay = :epochDay")
    fun observeDayKcal(epochDay: Long): Flow<Int>

    @Query("SELECT * FROM exercise_log WHERE deleted = 0 AND epochDay BETWEEN :from AND :to")
    suspend fun logsInRange(from: Long, to: Long): List<ExerciseLogEntity>

    // Sem filtrar apagados, e de propósito: um treino importado e depois apagado à mão não
    // pode voltar na sincronização seguinte com o Health Connect.
    @Query("SELECT refId FROM exercise_log WHERE origin = 'HEALTH_CONNECT' AND refId IS NOT NULL")
    suspend fun importedRefs(): List<String>

    @Query("SELECT * FROM exercise_log WHERE deleted = 0")
    suspend fun exportRows(): List<ExerciseLogEntity>
}
