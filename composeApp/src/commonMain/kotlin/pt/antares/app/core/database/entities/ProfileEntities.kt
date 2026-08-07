package pt.antares.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.calc.BmrFormula
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.EnergyUnit
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.WeightSource
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.UnitSystem

@Serializable
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = SINGLETON_ID,
    val sex: Sex,
    val birthEpochDay: Long,
    val heightCm: Int,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,

    val goalRateKcal: Int,
    val macroStrategy: MacroStrategy,
    val customProteinG: Int?,
    val customCarbsG: Int?,
    val customFatG: Int?,

    val exerciseAddBack: Boolean = true,

    @ColumnInfo(defaultValue = "NULL") val goalWeightKg: Double? = null,

    @ColumnInfo(defaultValue = "NULL") val bodyFatPct: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val bodyFatSource: BodyFatSource? = null,

    @ColumnInfo(defaultValue = "NULL") val waistCm: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val neckCm: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val hipCm: Double? = null,

    @ColumnInfo(defaultValue = "NULL") val bmrFormulaOverride: BmrFormula? = null,

    @ColumnInfo(defaultValue = "NULL") val goalBodyFatPct: Double? = null,

    @ColumnInfo(defaultValue = "NULL") val heightConfirmedEpochDay: Long? = null,

    @ColumnInfo(defaultValue = "NULL") val trendWindowDays: Int? = null,

    @ColumnInfo(defaultValue = "NULL") val lifeStage: LifeStage? = null,

    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val energyUnit: EnergyUnit = EnergyUnit.KCAL,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
) {
    companion object {
        const val SINGLETON_ID = "profile"
    }
}

@Serializable
@Entity(
    tableName = "weight_log",
    indices = [Index(value = ["epochDay"], unique = true)],
)
data class WeightLogEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val weightKg: Double,
    val note: String?,

    @ColumnInfo(defaultValue = "MANUAL")
    val source: WeightSource = WeightSource.MANUAL,

    val sourceRef: String? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(
    tableName = "daily_target_override",
    indices = [Index(value = ["epochDay"], unique = true)],
)
data class DailyTargetOverrideEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,

    val source: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(
    tableName = "body_measurement_log",
    indices = [Index(value = ["epochDay"], unique = true)],
)
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val bodyFatPct: Double? = null,

    val bodyFatSource: BodyFatSource? = null,
    val waistCm: Double? = null,
    val neckCm: Double? = null,
    val hipCm: Double? = null,

    @ColumnInfo(defaultValue = "NULL") val armCm: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val thighCm: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val chestCm: Double? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
) {

    val isEmpty: Boolean
        get() = bodyFatPct == null && waistCm == null && neckCm == null && hipCm == null &&
            armCm == null && thighCm == null && chestCm == null
}
