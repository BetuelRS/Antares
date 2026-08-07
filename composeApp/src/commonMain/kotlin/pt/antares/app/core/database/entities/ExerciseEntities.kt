package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.ExerciseOrigin

@Serializable
@Entity(
    tableName = "exercise_log",
    indices = [Index("epochDay")],
)
data class ExerciseLogEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val origin: ExerciseOrigin,
    val label: String,
    val metId: String?,
    val met: Double?,
    val durationMin: Int,
    val kcal: Int,

    val refId: String?,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)
