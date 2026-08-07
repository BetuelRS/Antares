package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "coach_report",
    indices = [Index(value = ["weekStartEpochDay"], unique = true)],
)
data class CoachReportEntity(
    @PrimaryKey val id: String,

    val weekStartEpochDay: Long,

    val winsJson: String,
    val observationsJson: String,
    val adjustmentsJson: String,
    val focus: String,

    val aggregateJson: String,

    val proposedKcal: Int? = null,
    val previousKcal: Int? = null,
    val observedTdee: Int? = null,

    val proposalAccepted: Boolean? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)
