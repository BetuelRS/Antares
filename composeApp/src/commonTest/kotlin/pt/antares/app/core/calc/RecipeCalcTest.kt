package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecipeCalcTest {

    private val ingredients = listOf(
        IngredientNutrition(kcalPer100 = 350, proteinPer100 = 7.0, carbsPer100 = 77.0, fatPer100 = 1.0, grams = 100.0),
        IngredientNutrition(kcalPer100 = 165, proteinPer100 = 31.0, carbsPer100 = 0.0, fatPer100 = 3.6, grams = 200.0),
        IngredientNutrition(kcalPer100 = 900, proteinPer100 = 0.0, carbsPer100 = 0.0, fatPer100 = 100.0, grams = 20.0),
    )

    @Test
    fun `totais somam os ingredientes escalados`() {
        val r = RecipeCalc.compute(ingredients, yieldGrams = null)

        assertEquals(860, r.totalKcal)

        assertEquals(69.0, r.totalProteinG, 1e-9)

        assertEquals(77.0, r.totalCarbsG, 1e-9)

        assertEquals(28.2, r.totalFatG, 1e-9)
    }

    @Test
    fun `sem yield o per-100g usa a soma crua`() {
        val r = RecipeCalc.compute(ingredients, yieldGrams = null)

        assertEquals(320.0, r.basisGrams, 1e-9)

        assertEquals(269, r.kcalPer100)
        assertEquals(21.5625, r.proteinPer100, 1e-9)
    }

    @Test
    fun `yield confecionado altera a base do per-100g`() {

        val r = RecipeCalc.compute(ingredients, yieldGrams = 280.0)
        assertEquals(280.0, r.basisGrams, 1e-9)

        assertEquals(307, r.kcalPer100)
    }

    @Test
    fun `receita vazia nao rebenta`() {
        val r = RecipeCalc.compute(emptyList(), yieldGrams = null)
        assertEquals(0, r.totalKcal)
        assertEquals(0, r.kcalPer100)
        assertEquals(0.0, r.basisGrams, 1e-9)
    }

    private fun ing(grams: Double, micros: Map<String, Double>) = IngredientNutrition(
        kcalPer100 = 100, proteinPer100 = 5.0, carbsPer100 = 10.0, fatPer100 = 2.0,
        grams = grams, microsPer100 = micros,
    )

    @Test
    fun `micros somam-se e normalizam para 100 g do produto final`() {
        val r = RecipeCalc.compute(
            listOf(
                ing(100.0, mapOf("iron_mg" to 2.0)),
                ing(100.0, mapOf("iron_mg" to 4.0)),
            ),
            yieldGrams = null,
        )

        assertEquals(3.0, r.microsPer100["iron_mg"]!!, 1e-9)
    }

    @Test
    fun `micro declarado por pouca massa da receita e descartado`() {

        val r = RecipeCalc.compute(
            listOf(
                ing(180.0, mapOf("iron_mg" to 1.0)),
                ing(20.0, mapOf("iron_mg" to 1.0, "zinc_mg" to 9.0)),
            ),
            yieldGrams = null,
        )
        assertTrue("zinc_mg" !in r.microsPer100, "zinco com 10% de cobertura não devia entrar")
        assertTrue("iron_mg" in r.microsPer100, "ferro com 100% de cobertura devia entrar")
    }

    @Test
    fun `yield concentra os micros tal como as calorias`() {
        val r = RecipeCalc.compute(
            listOf(ing(200.0, mapOf("iron_mg" to 3.0))),
            yieldGrams = 100.0,
        )

        assertEquals(6.0, r.microsPer100["iron_mg"]!!, 1e-9)
    }

    @Test
    fun `secundarios seguem a mesma regra de cobertura`() {
        val r = RecipeCalc.compute(
            listOf(
                IngredientNutrition(100, 5.0, 10.0, 2.0, 100.0, fiberPer100 = 4.0, sodiumMgPer100 = 10.0),
                IngredientNutrition(100, 5.0, 10.0, 2.0, 100.0, fiberPer100 = 2.0),
            ),
            yieldGrams = null,
        )
        assertEquals(3.0, r.fiberPer100!!, 1e-9)
        assertEquals(null, r.sodiumMgPer100)
    }
}
