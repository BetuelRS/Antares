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
    // Peso depois de cozinhar. Nulo usa a soma dos ingredientes — ver [RecipeCalc.compute],
    // onde a água evaporada concentra a nutrição por 100 g.
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
/**
 * Um ingrediente aponta para o alimento em vez de lhe copiar a nutrição: ao contrário do
 * diário, uma receita deve refletir a correção feita no alimento.
 */
data class RecipeIngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val foodId: String,
    val grams: Double,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)
