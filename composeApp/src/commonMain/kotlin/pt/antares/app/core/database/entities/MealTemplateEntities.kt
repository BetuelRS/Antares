package pt.antares.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.MealSlot

@Serializable
@Entity(tableName = "meal_template")
data class MealTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slot: MealSlot,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(
    tableName = "meal_template_item",
    indices = [Index("templateId"), Index("foodId")],
)
/**
 * Item de uma refeição guardada. Copia a nutrição como o `food_log`, e não por referência
 * como a receita: aplicar o modelo tem de dar o mesmo resultado de sempre, e é uma cópia
 * de um registo que se faz.
 */
data class MealTemplateItemEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val foodId: String?,
    val nameSnapshot: String,
    val quantityGrams: Double,
    val kcalSnapshot: Int,
    val proteinSnapshot: Double,
    val carbsSnapshot: Double,
    val fatSnapshot: Double,

    val microsPer100Json: String?,

    @ColumnInfo(defaultValue = "0") val isLiquid: Boolean = false,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)
