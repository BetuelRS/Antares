package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recipe")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val yieldGrams: Double?,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Serializable
@Entity(
    tableName = "recipe_ingredient",
    indices = [Index("recipeId"), Index("foodId")],
)
data class RecipeIngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val foodId: String,
    val grams: Double,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)
