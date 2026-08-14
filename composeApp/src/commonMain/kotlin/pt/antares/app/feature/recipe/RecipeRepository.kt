package pt.antares.app.feature.recipe

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import pt.antares.app.core.calc.IngredientNutrition
import pt.antares.app.core.calc.RecipeCalc
import pt.antares.app.core.calc.RecipeNutrition
import pt.antares.app.core.database.daos.FoodDao
import pt.antares.app.core.database.daos.RecipeDao
import pt.antares.app.core.database.daos.RecipeIngredientDao
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.RecipeEntity
import pt.antares.app.core.database.entities.RecipeIngredientEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.Ids
import pt.antares.app.feature.diary.DiaryRepository
import kotlin.math.roundToInt

data class IngredientRow(
    val ingredient: RecipeIngredientEntity,
    val food: FoodEntity?,
)

data class RecipeSummary(
    val recipe: RecipeEntity,
    val nutrition: RecipeNutrition,
    val ingredientCount: Int,
)

/**
 * Receitas. A nutrição nunca é gravada: recalcula-se sempre a partir dos ingredientes, e é
 * isso que faz corrigir um alimento corrigir todas as receitas que o usam — ao contrário
 * do diário, onde o registo guarda uma cópia.
 */
class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val ingredientDao: RecipeIngredientDao,
    private val foodDao: FoodDao,
    private val diaryRepository: DiaryRepository,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()
    private val json = Json { ignoreUnknownKeys = true }

    private fun microsOf(food: FoodEntity): Map<String, Double> =
        food.microsJson?.let {
            runCatching { json.decodeFromString<Map<String, Double>>(it) }.getOrNull()
        } ?: emptyMap()

    fun observeSummaries(): Flow<List<RecipeSummary>> =
        recipeDao.observeAll().map { recipes ->
            val out = ArrayList<RecipeSummary>(recipes.size)
            for (r in recipes) {
                val rows = ingredientRows(r.id)
                out += RecipeSummary(r, nutritionOf(rows, r.yieldGrams), rows.size)
            }
            out
        }

    fun observeIngredientRows(recipeId: String): Flow<List<IngredientRow>> =
        ingredientDao.observeForRecipe(recipeId).map { list ->
            val out = ArrayList<IngredientRow>(list.size)
            for (i in list) out += IngredientRow(i, foodDao.byId(i.foodId))
            out
        }

    suspend fun recipeById(id: String): RecipeEntity? = withContext(io) { recipeDao.byId(id) }

    suspend fun nutrition(recipeId: String): RecipeNutrition = withContext(io) {
        val recipe = recipeDao.byId(recipeId)
        nutritionOf(ingredientRows(recipeId), recipe?.yieldGrams)
    }

    suspend fun createRecipe(name: String, yieldGrams: Double?): String = withContext(io) {
        val id = Ids.newUuid()
        recipeDao.upsert(RecipeEntity(id = id, name = name.trim(), yieldGrams = yieldGrams, updatedAt = now()))
        id
    }

    suspend fun updateRecipe(id: String, name: String, yieldGrams: Double?) = withContext(io) {
        val existing = recipeDao.byId(id) ?: return@withContext
        recipeDao.upsert(existing.copy(name = name.trim(), yieldGrams = yieldGrams, updatedAt = now(), dirty = true))
    }

    suspend fun addIngredient(recipeId: String, foodId: String, grams: Double) = withContext(io) {
        ingredientDao.upsert(
            RecipeIngredientEntity(
                id = Ids.newUuid(),
                recipeId = recipeId,
                foodId = foodId,
                grams = grams,
                updatedAt = now(),
            ),
        )
    }

    suspend fun updateIngredientGrams(ingredient: RecipeIngredientEntity, grams: Double) = withContext(io) {
        ingredientDao.upsert(ingredient.copy(grams = grams, updatedAt = now(), dirty = true))
    }

    suspend fun removeIngredient(ingredient: RecipeIngredientEntity) = withContext(io) {
        ingredientDao.softDelete(ingredient.id, now())
    }

    suspend fun deleteRecipe(id: String) = withContext(io) {
        recipeDao.softDelete(id, now())
    }

    /**
     * Regista uma porção da receita no diário. A receita é convertida num alimento
     * temporário que nunca é gravado no catálogo: serve só para o registo copiar dele os
     * seus valores, e a partir daí o diário é dono do que ficou lá.
     *
     * É isto que reconcilia as duas regras: a receita continua viva e a recalcular-se,
     * mas o que já foi comido fica congelado como qualquer outro registo.
     */
    suspend fun logRecipe(recipeId: String, grams: Double, slot: MealSlot, epochDay: Long) = withContext(io) {
        val recipe = recipeDao.byId(recipeId) ?: return@withContext
        val n = nutritionOf(ingredientRows(recipeId), recipe.yieldGrams)

        val asFood = FoodEntity(
            // Identificador derivado da receita, e não aleatório: o registo aponta-lhe, e
            // o prefixo distingue-o de qualquer alimento verdadeiro.
            id = "recipe_$recipeId",
            source = FoodSource.CUSTOM,
            sourceRef = null,
            namePt = recipe.name,
            nameEn = recipe.name,
            brand = null,
            kcal = n.kcalPer100,
            proteinG = n.proteinPer100,
            carbsG = n.carbsPer100,
            sugarsG = n.sugarsPer100,
            fatG = n.fatPer100,
            satFatG = n.satFatPer100,
            fiberG = n.fiberPer100,
            sodiumMg = n.sodiumMgPer100?.roundToInt(),

            microsJson = n.microsPer100.takeIf { it.isNotEmpty() }
                ?.let { json.encodeToString(it) },
            servingName = null,
            servingGrams = recipe.yieldGrams,
            updatedAt = now(),
        )
        diaryRepository.logFood(asFood, grams, slot, epochDay)
    }

    fun nutritionFrom(rows: List<IngredientRow>, yieldGrams: Double?): RecipeNutrition =
        nutritionOf(rows, yieldGrams)

    private suspend fun ingredientRows(recipeId: String): List<IngredientRow> {
        val list = ingredientDao.forRecipe(recipeId)
        return list.map { IngredientRow(it, foodDao.byId(it.foodId)) }
    }

    private fun nutritionOf(rows: List<IngredientRow>, yieldGrams: Double?): RecipeNutrition {
        val ingredients = rows.mapNotNull { row ->
            // Ingrediente cujo alimento desapareceu do catálogo é saltado: a receita
            // continua a somar o resto, com menos, em vez de deixar de ter valores.
            val f = row.food ?: return@mapNotNull null
            IngredientNutrition(
                kcalPer100 = f.kcal,
                proteinPer100 = f.proteinG,
                carbsPer100 = f.carbsG,
                fatPer100 = f.fatG,
                grams = row.ingredient.grams,
                sugarsPer100 = f.sugarsG,
                satFatPer100 = f.satFatG,
                fiberPer100 = f.fiberG,
                sodiumMgPer100 = f.sodiumMg?.toDouble(),
                microsPer100 = microsOf(f),
            )
        }
        return RecipeCalc.compute(ingredients, yieldGrams)
    }
}
