package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "goal_history",
    indices = [Index(value = ["setOnEpochDay"])],
)
data class GoalHistoryEntity(
    @PrimaryKey val id: String,
    val targetKg: Double,
    val setOnEpochDay: Long,

    val startWeightKg: Double? = null,

    val reachedOnEpochDay: Long? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(tableName = "search_miss", indices = [Index(value = ["query"], unique = true)])
data class SearchMissEntity(
    @PrimaryKey val query: String,
    val count: Int = 1,
    val lastSeenEpochDay: Long,
)

@Serializable
@Entity(tableName = "progress_photo", indices = [Index(value = ["epochDay"])])
data class ProgressPhotoEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,

    val localPath: String,

    val weightKgSnapshot: Double? = null,
    val note: String? = null,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "cycle_log", indices = [Index(value = ["startEpochDay"], unique = true)])
data class CycleEntity(
    @PrimaryKey val id: String,
    val startEpochDay: Long,

    val endEpochDay: Long? = null,
    val note: String? = null,
    val createdAt: Long,
)
