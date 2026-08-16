package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.RoutineScheduleEntity

@Dao
interface RoutineScheduleDao {

    @Query("SELECT * FROM routine_schedule WHERE deleted = 0 ORDER BY dayOfWeek")
    fun observeAll(): Flow<List<RoutineScheduleEntity>>

    @Query("SELECT routineId FROM routine_schedule WHERE dayOfWeek = :dayOfWeek AND deleted = 0")
    fun observeRoutineForDay(dayOfWeek: Int): Flow<String?>

    @Upsert
    suspend fun upsert(entry: RoutineScheduleEntity)

    @Query("UPDATE routine_schedule SET deleted = 1, updatedAt = :now WHERE dayOfWeek = :dayOfWeek")
    suspend fun clearDay(dayOfWeek: Int, now: Long)

    // Apagar uma rotina tem de a tirar do calendário: não há chave estrangeira, e um dia
    // apontado a uma rotina que já não existe ficaria vazio sem explicação.
    @Query("UPDATE routine_schedule SET deleted = 1, updatedAt = :now WHERE routineId = :routineId")
    suspend fun clearByRoutine(routineId: String, now: Long)

    // O inverso, para o desfazer: os dias voltam com a rotina.
    @Query("UPDATE routine_schedule SET deleted = 0, updatedAt = :now WHERE routineId = :routineId")
    suspend fun restoreByRoutine(routineId: String, now: Long)

    @Query("SELECT * FROM routine_schedule WHERE deleted = 0")
    suspend fun exportRows(): List<RoutineScheduleEntity>
}
