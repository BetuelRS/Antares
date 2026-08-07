package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunStatus

@Serializable
@Entity(
    tableName = "run",
    indices = [Index("startedAt"), Index("status")],
)
data class RunEntity(
    @PrimaryKey val id: String,
    val type: ActivityType,
    val startedAt: Long,
    val endedAt: Long,
    val distanceM: Double,
    val movingS: Long,
    val elapsedS: Long,
    val avgPaceSecPerKm: Int,
    val kcal: Int,
    val elevGainM: Double,
    val polyline: String,
    val splitsJson: String,
    val name: String,
    val note: String,
    val status: RunStatus,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Entity(
    tableName = "track_point",
    indices = [Index("runId")],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val tMs: Long,
    val lat: Double,
    val lon: Double,
    val altM: Double?,
    val accM: Double,
    val speedMps: Double?,
)
