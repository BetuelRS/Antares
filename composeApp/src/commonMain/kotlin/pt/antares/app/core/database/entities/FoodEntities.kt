package pt.antares.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot

@Serializable
@Entity(
    tableName = "foods",
    indices = [Index("lastUsedAt"), Index("isFavorite"), Index("source")],
)
data class FoodEntity(
    @PrimaryKey val id: String,
    val source: FoodSource,

    val sourceRef: String?,
    val namePt: String,
    val nameEn: String,
    val brand: String?,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val sugarsG: Double?,
    val fatG: Double,
    val satFatG: Double?,
    val fiberG: Double?,
    val sodiumMg: Int?,

    val microsJson: String?,
    val servingName: String?,
    val servingGrams: Double?,

    @ColumnInfo(defaultValue = "0") val isLiquid: Boolean = false,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0L,

    val lastAmountG: Double? = null,

    val verified: Boolean = false,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = false,
)

@Fts4(notIndexed = ["foodId"])
@Entity(tableName = "foods_fts")
data class FoodFtsEntity(
    val foodId: String,

    val searchText: String,
)

@Serializable
@Entity(
    tableName = "food_log",
    indices = [Index(value = ["epochDay", "mealSlot"]), Index("foodId")],
)
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val mealSlot: MealSlot,
    val foodId: String?,
    val nameSnapshot: String,
    val quantityGrams: Double,
    val kcalSnapshot: Int,
    val proteinSnapshot: Double,
    val carbsSnapshot: Double,
    val fatSnapshot: Double,

    val microsPer100Json: String?,
    val origin: LogOrigin = LogOrigin.MANUAL,

    @ColumnInfo(defaultValue = "0") val isLiquid: Boolean = false,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(
    tableName = "water_log",
    indices = [Index(value = ["epochDay"], unique = true)],
)
data class WaterLogEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val ml: Int,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)
