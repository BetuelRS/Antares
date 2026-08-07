package pt.antares.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.SessionStatus

@Serializable
@Entity(
    tableName = "exercise",
    indices = [Index("category"), Index("equipment")],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val namePt: String,

    val searchText: String,
    val category: String,
    val force: String?,
    val mechanic: String?,
    val equipment: String?,
    val level: String,

    val primaryMuscles: String,
    val secondaryMuscles: String,
    val instructionsEnJson: String,
    val instructionsPtJson: String,
    val imagesJson: String,
    val isCustom: Boolean = false,
    val verified: Boolean = false,
    val updatedAt: Long,
    val deleted: Boolean = false,

    val dirty: Boolean = false,
)

@Serializable
@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val note: String?,
    val position: Int,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(
    tableName = "routine_item",
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineItemEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val exerciseId: String,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetWeightKg: Double?,
    val restSec: Int,
    val position: Int,
    val supersetGroup: Int?,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(
    tableName = "workout_session",
    indices = [Index("status"), Index("startedAt")],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val routineId: String?,
    val note: String?,
    val status: SessionStatus,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(tableName = "routine_schedule")
data class RoutineScheduleEntity(
    @PrimaryKey val dayOfWeek: Int,
    val routineId: String,
    val updatedAt: Long,

    @ColumnInfo(defaultValue = "0")
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(
    tableName = "workout_set",
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val isWarmup: Boolean = false,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)
