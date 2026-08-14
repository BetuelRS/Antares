package pt.antares.app.feature.recipe

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.RecipeEntity
import pt.antares.app.core.database.entities.RecipeIngredientEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.MealSlot
import pt.antares.app.testing.ViewModelHarness
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Registar uma receita no diário atravessa duas regras que se contradizem de propósito: a receita
 * é viva e recalcula-se sempre que um ingrediente muda, e o registo é congelado no instante em
 * que se come. A conversão entre as duas é o que se testa aqui — a aritmética da receita já tem
 * o `RecipeCalcTest`, e o que aqui pode partir é a passagem de "por 100 g do produto final" para
 * "o que esta pessoa comeu".
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecipeLogTest : ViewModelHarness() {

    private val hoje = 20_000L

    private fun repository() = RecipeRepository(
        db.recipeDao(),
        db.recipeIngredientDao(),
        db.foodDao(),
        diaryRepository(),
        dispatcher,
    )

    private suspend fun alimento(
        id: String,
        kcal: Int,
        proteinG: Double = 0.0,
        carbsG: Double = 0.0,
        fatG: Double = 0.0,
        microsJson: String? = null,
    ) = FoodEntity(
        id = id,
        source = FoodSource.CUSTOM,
        sourceRef = null,
        namePt = id,
        nameEn = id,
        brand = null,
        kcal = kcal,
        proteinG = proteinG,
        carbsG = carbsG,
        sugarsG = null,
        fatG = fatG,
        satFatG = null,
        fiberG = null,
        sodiumMg = null,
        microsJson = microsJson,
        servingName = null,
        servingGrams = null,
        updatedAt = 0L,
    ).also { db.foodDao().upsert(it) }

    private suspend fun receita(
        id: String,
        nome: String,
        yieldGrams: Double?,
        ingredientes: List<Pair<String, Double>>,
    ) {
        db.recipeDao().upsert(
            RecipeEntity(id = id, name = nome, yieldGrams = yieldGrams, updatedAt = 0L),
        )
        ingredientes.forEachIndexed { i, (foodId, gramas) ->
            db.recipeIngredientDao().upsert(
                RecipeIngredientEntity(
                    id = "$id-$i",
                    recipeId = id,
                    foodId = foodId,
                    grams = gramas,
                    updatedAt = 0L,
                ),
            )
        }
    }

    private suspend fun registosDeHoje() = db.foodLogDao().dayLogs(hoje)

    @Test
    fun `a porcao comida e a nutricao por cem gramas escalada`() = runTest(dispatcher) {
        // 200 g a 100 kcal/100 g + 100 g a 400 kcal/100 g = 600 kcal em 300 g, ou seja
        // 200 kcal por 100 g do produto final.
        alimento("arroz", kcal = 100, carbsG = 25.0)
        alimento("azeite", kcal = 400, fatG = 45.0)
        receita("r1", "Arroz com azeite", yieldGrams = null, listOf("arroz" to 200.0, "azeite" to 100.0))

        repository().logRecipe("r1", grams = 150.0, slot = MealSlot.LUNCH, epochDay = hoje)

        val registo = registosDeHoje().single()
        assertEquals(300, registo.kcalSnapshot, "150 g de uma receita de 200 kcal/100 g")
        assertEquals(150.0, registo.quantityGrams)
        assertEquals(MealSlot.LUNCH, registo.mealSlot)
    }

    @Test
    fun `o peso depois de cozinhar concentra o que se comeu`() = runTest(dispatcher) {
        alimento("massa", kcal = 350)
        // 100 g de massa seca a 350 kcal, servidas como 250 g de massa cozida: as mesmas
        // 350 kcal passam a valer 140 por cada 100 g no prato.
        receita("r1", "Massa cozida", yieldGrams = 250.0, listOf("massa" to 100.0))

        repository().logRecipe("r1", grams = 250.0, slot = MealSlot.DINNER, epochDay = hoje)

        val registo = registosDeHoje().single()
        assertEquals(350, registo.kcalSnapshot, "comer a receita inteira não deu as calorias todas")
    }

    @Test
    fun `o registo guarda o nome da receita, e nao o do primeiro ingrediente`() =
        runTest(dispatcher) {
            alimento("frango", kcal = 165, proteinG = 31.0)
            receita("r1", "Frango à Betuel", yieldGrams = null, listOf("frango" to 200.0))

            repository().logRecipe("r1", grams = 200.0, slot = MealSlot.LUNCH, epochDay = hoje)

            assertEquals("Frango à Betuel", registosDeHoje().single().nameSnapshot)
        }

    @Test
    fun `a receita nao entra no catalogo de alimentos`() = runTest(dispatcher) {
        alimento("frango", kcal = 165)
        receita("r1", "Frango", yieldGrams = null, listOf("frango" to 100.0))

        repository().logRecipe("r1", grams = 100.0, slot = MealSlot.LUNCH, epochDay = hoje)

        val registo = registosDeHoje().single()
        assertEquals("recipe_r1", registo.foodId, "o registo deixou de apontar à receita")
        assertNull(
            db.foodDao().byId("recipe_r1"),
            "a receita foi parar ao catálogo e passa a aparecer nas pesquisas",
        )
    }

    @Test
    fun `mudar a receita depois nao mexe no que ja foi comido`() = runTest(dispatcher) {
        alimento("arroz", kcal = 100)
        receita("r1", "Arroz", yieldGrams = null, listOf("arroz" to 100.0))
        val repo = repository()
        repo.logRecipe("r1", grams = 100.0, slot = MealSlot.LUNCH, epochDay = hoje)

        val antes = registosDeHoje().single().kcalSnapshot

        // Duplicar o arroz e mudar o nome: a receita passa a ser outra coisa, o almoço de
        // ontem não.
        repo.updateRecipe("r1", name = "Arroz em dobro", yieldGrams = null)
        db.recipeIngredientDao().upsert(
            RecipeIngredientEntity(
                id = "r1-extra",
                recipeId = "r1",
                foodId = "arroz",
                grams = 100.0,
                updatedAt = 1L,
            ),
        )

        val depois = registosDeHoje().single()
        assertEquals(antes, depois.kcalSnapshot, "o registo antigo mudou com a receita")
        assertEquals("Arroz", depois.nameSnapshot)
    }

    @Test
    fun `um ingrediente que desapareceu do catalogo nao apaga a receita`() = runTest(dispatcher) {
        alimento("arroz", kcal = 100)
        receita("r1", "Arroz e fantasma", yieldGrams = null, listOf("arroz" to 100.0, "nao-existe" to 100.0))

        repository().logRecipe("r1", grams = 100.0, slot = MealSlot.LUNCH, epochDay = hoje)

        val registo = registosDeHoje().single()
        // Sobra só o arroz: 100 g a 100 kcal em 100 g contabilizadas.
        assertEquals(100, registo.kcalSnapshot, "o ingrediente em falta levou a receita consigo")
    }

    @Test
    fun `registar uma receita que ja nao existe nao escreve nada`() = runTest(dispatcher) {
        repository().logRecipe("nao-existe", grams = 100.0, slot = MealSlot.LUNCH, epochDay = hoje)

        assertTrue(registosDeHoje().isEmpty(), "inventou um registo a partir do nada")
    }

    @Test
    fun `os micronutrientes acompanham a porcao`() = runTest(dispatcher) {
        alimento("espinafres", kcal = 23, microsJson = """{"iron_mg":2.7}""")
        receita("r1", "Espinafres", yieldGrams = null, listOf("espinafres" to 100.0))

        repository().logRecipe("r1", grams = 200.0, slot = MealSlot.DINNER, epochDay = hoje)

        val micros = assertNotNull(
            registosDeHoje().single().microsPer100Json,
            "a receita chegou ao diário sem micronutrientes nenhuns",
        )
        assertTrue(micros.contains("iron_mg"), "o ferro perdeu-se na conversão: $micros")
    }

    @Test
    fun `os macros seguem as calorias na mesma proporcao`() = runTest(dispatcher) {
        alimento("frango", kcal = 165, proteinG = 31.0, fatG = 3.6)
        receita("r1", "Frango", yieldGrams = null, listOf("frango" to 200.0))

        repository().logRecipe("r1", grams = 100.0, slot = MealSlot.LUNCH, epochDay = hoje)

        val registo = registosDeHoje().single()
        assertEquals(165, registo.kcalSnapshot)
        assertTrue(
            abs(registo.proteinSnapshot - 31.0) < 0.01,
            "proteína ${registo.proteinSnapshot} para 100 g de frango",
        )
    }
}
