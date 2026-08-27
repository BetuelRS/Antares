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
import pt.antares.app.core.confecao.LeitorDeConfecao
import pt.antares.app.core.confecao.MetodoDeConfecao
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
import pt.antares.app.core.nutrition.microsDeJson
import pt.antares.app.core.nutrition.Nutrients

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
    private val confecao: LeitorDeConfecao,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()
    private val json = Json { ignoreUnknownKeys = true }

    private fun microsOf(food: FoodEntity): Map<String, Double> = microsDeJson(food.microsJson)

    fun observeSummaries(): Flow<List<RecipeSummary>> =
        recipeDao.observeAll().map { recipes ->
            val out = ArrayList<RecipeSummary>(recipes.size)
            for (r in recipes) {
                val rows = ingredientRows(r.id)
                out += RecipeSummary(r, nutritionOf(rows, r.yieldGrams, r.metodo), rows.size)
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
        nutritionOf(ingredientRows(recipeId), recipe?.yieldGrams, recipe?.metodo)
    }

    suspend fun createRecipe(name: String, yieldGrams: Double?, servings: Int? = null): String = withContext(io) {
        val id = Ids.newUuid()
        recipeDao.upsert(
            RecipeEntity(
                id = id,
                name = name.trim(),
                yieldGrams = yieldGrams,
                servings = servings,
                updatedAt = now(),
            ),
        )
        id
    }

    suspend fun updateRecipe(id: String, name: String, yieldGrams: Double?, servings: Int? = null) = withContext(io) {
        val existing = recipeDao.byId(id) ?: return@withContext
        recipeDao.upsert(
            existing.copy(
                name = name.trim(),
                yieldGrams = yieldGrams,
                servings = servings,
                updatedAt = now(),
            ),
        )
    }

    /**
     * Escolhe — ou tira — o método com que o prato foi cozinhado.
     *
     * Fica em campo próprio e não junto com o nome e o peso porque se muda noutro sítio do
     * ecrã e noutro momento: o nome escreve-se ao criar, o método escolhe-se ao olhar para
     * os números.
     */
    suspend fun updateMetodo(id: String, metodo: String?) = withContext(io) {
        val existing = recipeDao.byId(id) ?: return@withContext
        recipeDao.upsert(existing.copy(metodo = metodo, updatedAt = now()))
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
        ingredientDao.upsert(ingredient.copy(grams = grams, updatedAt = now()))
    }

    suspend fun removeIngredient(ingredient: RecipeIngredientEntity) = withContext(io) {
        ingredientDao.softDelete(ingredient.id, now())
    }

    suspend fun restoreIngredient(ingredientId: String) = withContext(io) {
        ingredientDao.restore(ingredientId, now())
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
        val n = nutritionOf(ingredientRows(recipeId), recipe.yieldGrams, recipe.metodo)

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
            microsJson = buildMap {
                putAll(n.microsPer100)
                n.fiberPer100?.let { put(Nutrients.FIBER, it) }
                n.sodiumMgPer100?.let { put(Nutrients.SODIUM, it) }
            }.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
            servingName = null,
            servingGrams = recipe.yieldGrams,
            updatedAt = now(),
        )
        diaryRepository.logFood(asFood, grams, slot, epochDay)
    }

    suspend fun nutritionFrom(
        rows: List<IngredientRow>,
        yieldGrams: Double?,
        metodo: String? = null,
    ): RecipeNutrition = nutritionOf(rows, yieldGrams, metodo)

    private suspend fun ingredientRows(recipeId: String): List<IngredientRow> {
        val list = ingredientDao.forRecipe(recipeId)
        return list.map { IngredientRow(it, foodDao.byId(it.foodId)) }
    }

    /**
     * A nutrição da receita, com a retenção de cada ingrediente quando há método escolhido.
     *
     * **A tabela só se lê quando alguém escolheu um método.** Sem método não há retenção
     * nenhuma a aplicar, e ler o ficheiro para devolver mapas vazios era trabalho a mais em
     * cada linha da lista de receitas.
     */
    private suspend fun nutritionOf(
        rows: List<IngredientRow>,
        yieldGrams: Double?,
        metodo: String?,
    ): RecipeNutrition {
        val tabela = metodo?.let { confecao.tabela() }

        val ingredients = rows.mapNotNull { row ->
            // Ingrediente cujo alimento desapareceu do catálogo é saltado: a receita
            // continua a somar o resto, com menos, em vez de deixar de ter valores.
            val f = row.food ?: return@mapNotNull null

            // Um ingrediente sem família — o azeite, o sal, um alimento criado à mão — não
            // tem linha na tabela, e fica sem retenção. É o que a tabela diz: nada se sabe
            // sobre ele, e inventar um factor médio era pior do que não ter nenhum.
            val retencoes = if (metodo != null && tabela != null) {
                tabela.linha(f.familia, metodo)?.retencoes.orEmpty()
            } else {
                emptyMap()
            }

            IngredientNutrition(
                kcalPer100 = f.kcal,
                proteinPer100 = f.proteinG,
                carbsPer100 = f.carbsG,
                fatPer100 = f.fatG,
                grams = row.ingredient.grams,
                sugarsPer100 = f.sugarsG,
                satFatPer100 = f.satFatG,
                fiberPer100 = microsOf(f)[Nutrients.FIBER],
                sodiumMgPer100 = microsOf(f)[Nutrients.SODIUM],
                microsPer100 = microsOf(f),
                retencoes = retencoes,
            )
        }
        return RecipeCalc.compute(ingredients, yieldGrams)
    }

    /**
     * Os métodos que fazem sentido para esta receita: os que **alguma** família presente
     * conhece.
     *
     * Uma receita mistura famílias — carne, legumes, cereais —, e cada uma conhece os seus.
     * Oferecer só a intersecção deixava um cozido de carne com legumes sem «cozido», porque
     * as tabelas não publicam rendimento de cozer legumes. A união é o que o cozinheiro
     * reconhece: cozeu-se o prato, e cada ingrediente perde o que a tabela dele diz.
     */
    suspend fun metodosPara(recipeId: String): List<MetodoDeConfecao> {
        val familias = ingredientRows(recipeId).mapNotNull { it.food?.familia }.toSet()
        if (familias.isEmpty()) return emptyList()
        val tabela = confecao.tabela()
        val ordem = tabela.metodos.map { it.id }
        return familias.flatMap { tabela.metodosDe(it) }
            .distinctBy { it.id }
            .sortedBy { ordem.indexOf(it.id) }
    }

    /**
     * O peso final que as tabelas prevêem, para quem não pôs a panela na balança.
     *
     * **Não se grava sozinho, e não entra em conta nenhuma.** É uma sugestão que o ecrã
     * mostra ao lado do campo vazio, e que a pessoa aceita ou ignora: um peso final é uma
     * medição do prato dela, e a tabela é uma mediana de pratos que não são este.
     *
     * Devolve nulo se menos de [RecipeCalc.MIN_COVERAGE] do peso tiver rendimento publicado
     * — abaixo disso a soma descreve parte do tacho e passaria por descrever o tacho todo.
     */
    suspend fun pesoFinalSugerido(recipeId: String, metodo: String?): Double? {
        if (metodo == null) return null
        val tabela = confecao.tabela()
        val rows = ingredientRows(recipeId)

        var comRendimento = 0.0
        var total = 0.0
        var previsto = 0.0

        for (row in rows) {
            val gramas = row.ingredient.grams
            total += gramas
            val rendimento = row.food?.familia?.let { tabela.linha(it, metodo)?.rendimento }
            if (rendimento != null) {
                comRendimento += gramas
                previsto += gramas * rendimento
            } else {
                // Sem rendimento publicado, o ingrediente entra com o peso que tem. É o
                // que a app já assumia em toda a receita, e aqui fica explícito.
                previsto += gramas
            }
        }

        if (total <= 0.0) return null
        if (comRendimento / total < RecipeCalc.MIN_COVERAGE) return null
        return previsto
    }
}
