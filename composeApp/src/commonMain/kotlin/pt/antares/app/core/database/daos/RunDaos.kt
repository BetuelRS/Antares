package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.database.entities.TrackPointEntity

@Dao
interface RunDao {

    @Upsert
    suspend fun upsert(run: RunEntity)

    @Query("SELECT * FROM run WHERE status = 'DONE' AND deleted = 0 ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<RunEntity>>

    @Query("SELECT * FROM run WHERE status = 'DONE' AND deleted = 0 ORDER BY startedAt ASC")
    suspend fun allDone(): List<RunEntity>

    @Query(
        "SELECT * FROM run WHERE status = 'DONE' AND deleted = 0 " +
            "AND startedAt BETWEEN :fromMs AND :toMs ORDER BY startedAt ASC",
    )
    suspend fun runsBetween(fromMs: Long, toMs: Long): List<RunEntity>

    @Query("SELECT * FROM run WHERE id = :id")
    suspend fun byId(id: String): RunEntity?

    @Query("UPDATE run SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM run WHERE deleted = 0")
    suspend fun exportRows(): List<RunEntity>
}

@Dao
interface TrackPointDao {

    @Insert
    suspend fun insert(point: TrackPointEntity)

    @Insert
    suspend fun insertAll(points: List<TrackPointEntity>)

    @Query("SELECT COUNT(*) FROM track_point WHERE runId = :runId")
    suspend fun countForRun(runId: String): Int

    @Query("DELETE FROM track_point WHERE runId = :runId")
    suspend fun deleteForRun(runId: String)
}
