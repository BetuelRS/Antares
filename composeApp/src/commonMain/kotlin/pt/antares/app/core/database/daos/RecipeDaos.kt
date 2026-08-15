package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.RecipeEntity
import pt.antares.app.core.database.entities.RecipeIngredientEntity

@Dao
interface RecipeDao {

    @Upsert
    suspend fun upsert(recipe: RecipeEntity)

    @Query("UPDATE recipe SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM recipe WHERE deleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe WHERE id = :id AND deleted = 0")
    suspend fun byId(id: String): RecipeEntity?

    @Query("SELECT * FROM recipe WHERE deleted = 0")
    suspend fun exportRows(): List<RecipeEntity>
}

@Dao
interface RecipeIngredientDao {

    @Upsert
    suspend fun upsert(ingredient: RecipeIngredientEntity)

    @Query("UPDATE recipe_ingredient SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    // Ordem de introdução, que é a ordem em que se cozinha. Não há coluna de posição:
    // reordenar ingredientes não muda a receita.
    @Query("SELECT * FROM recipe_ingredient WHERE recipeId = :recipeId AND deleted = 0 ORDER BY updatedAt ASC")
    fun observeForRecipe(recipeId: String): Flow<List<RecipeIngredientEntity>>

    @Query("SELECT * FROM recipe_ingredient WHERE recipeId = :recipeId AND deleted = 0")
    suspend fun forRecipe(recipeId: String): List<RecipeIngredientEntity>

    @Query("SELECT * FROM recipe_ingredient WHERE deleted = 0")
    suspend fun exportRows(): List<RecipeIngredientEntity>
}
