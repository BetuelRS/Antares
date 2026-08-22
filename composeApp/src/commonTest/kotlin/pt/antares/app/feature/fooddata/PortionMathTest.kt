package pt.antares.app.feature.fooddata

import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.FoodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PortionMathTest {

    private fun food(kcal: Int, p: Double, c: Double, f: Double, servingGrams: Double? = null) = FoodEntity(
        id = "x",
        source = FoodSource.SEED,
        sourceRef = null,
        namePt = "Teste",
        nameEn = "Test",
        brand = null,
        kcal = kcal,
        proteinG = p,
        carbsG = c,
        sugarsG = null,
        fatG = f,
        satFatG = null,
        microsJson = null,
        servingName = servingGrams?.let { "porção" },
        servingGrams = servingGrams,
        updatedAt = 0L,
    )

    @Test
    fun `100g devolve o valor per-100g`() {
        val s = PortionState(food = food(kcal = 52, p = 0.3, c = 14.0, f = 0.2), quantityText = "100")
        assertEquals(52, s.previewKcal)
        assertEquals(0.3, s.previewP, 1e-9)
        assertEquals(14.0, s.previewC, 1e-9)
        assertEquals(0.2, s.previewF, 1e-9)
    }

    @Test
    fun `escala linear com a quantidade`() {
        val s = PortionState(food = food(kcal = 52, p = 0.3, c = 14.0, f = 0.2), quantityText = "250")
        assertEquals(130, s.previewKcal)
        assertEquals(0.75, s.previewP, 1e-9)
        assertEquals(35.0, s.previewC, 1e-9)
        assertEquals(0.5, s.previewF, 1e-9)
    }

    @Test
    fun `virgula decimal aceite`() {
        val s = PortionState(food = food(kcal = 100, p = 10.0, c = 0.0, f = 0.0), quantityText = "12,5")
        assertEquals(13, s.previewKcal)
        assertEquals(1.25, s.previewP, 1e-9)
    }

    @Test
    fun `quantidade invalida nao produz gramas`() {
        assertNull(PortionState(quantityText = "abc").quantityGrams)
        assertNull(PortionState(quantityText = "0").quantityGrams)
        assertNull(PortionState(quantityText = "6000").quantityGrams)
        assertEquals(150.5, PortionState(quantityText = "150,5").quantityGrams)
    }

    @Test
    fun `sem alimento carregado o preview e zero`() {
        val s = PortionState(food = null, quantityText = "200")
        assertEquals(0, s.previewKcal)
        assertEquals(0.0, s.previewP, 1e-9)
    }
}
