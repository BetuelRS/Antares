package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.CoachReportEntity

@Dao
interface CoachReportDao {
    @Upsert
    suspend fun upsert(report: CoachReportEntity)

    @Query("SELECT * FROM coach_report WHERE deleted = 0 ORDER BY weekStartEpochDay DESC")
    fun observeAll(): Flow<List<CoachReportEntity>>

    @Query("SELECT * FROM coach_report WHERE deleted = 0 ORDER BY weekStartEpochDay DESC LIMIT 1")
    fun observeLatest(): Flow<CoachReportEntity?>

    @Query("SELECT * FROM coach_report WHERE deleted = 0 AND id = :id")
    suspend fun byId(id: String): CoachReportEntity?

    // Vê as lápides, ao contrário das outras: o índice único na semana conta-as, e gerar o
    // relatório de uma semana antes apagada falharia contra uma linha invisível.
    @Query("SELECT * FROM coach_report WHERE weekStartEpochDay = :weekStartEpochDay")
    suspend fun byWeekForWrite(weekStartEpochDay: Long): CoachReportEntity?

    @Query(
        "UPDATE coach_report SET proposalAccepted = :accepted, updatedAt = :now " +
            "WHERE id = :id",
    )
    suspend fun setProposalAccepted(id: String, accepted: Boolean, now: Long)

    @Query("SELECT * FROM coach_report WHERE deleted = 0")
    suspend fun exportRows(): List<CoachReportEntity>
}
