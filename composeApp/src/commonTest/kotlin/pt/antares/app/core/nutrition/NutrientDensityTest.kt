package pt.antares.app.core.nutrition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutrientDensityTest {

    private val ironDrv = 11.0

    @Test
    fun `alimento pobre no nutriente nao entra na lista`() {
        val foods = listOf(Triple("agua", "Água", 1))
        val micros = mapOf("agua" to mapOf("iron_mg" to 0.001))
        assertTrue(NutrientDensity.rank(foods, micros, "iron_mg", ironDrv).isEmpty())
    }

    @Test
    fun `alimento sem o nutriente e ignorado, nao vale zero`() {
        val foods = listOf(Triple("azeite", "Azeite", 900))
        val micros = mapOf("azeite" to mapOf("vitE_mg" to 14.0))
        assertTrue(NutrientDensity.rank(foods, micros, "iron_mg", ironDrv).isEmpty())
    }

    @Test
    fun `sem referencia nao se ordena nada`() {
        val foods = listOf(Triple("x", "X", 100))
        val micros = mapOf("x" to mapOf("iron_mg" to 5.0))
        assertTrue(NutrientDensity.rank(foods, micros, "iron_mg", 0.0).isEmpty())
    }

    @Test
    fun `alimento sem calorias nao rebenta a divisao`() {
        val foods = listOf(Triple("cha", "Chá", 0))
        val micros = mapOf("cha" to mapOf("iron_mg" to 5.0))
        assertTrue(NutrientDensity.rank(foods, micros, "iron_mg", ironDrv).isEmpty())
    }
    @Test
    fun `temperos e algas secas nao sao resposta para o ferro`() {

        val foods = listOf(
            Triple("tomilho", "Tomilho seco", 276),
            Triple("alga", "Alga seca", 130),
            Triple("figado", "Fígado de vaca", 135),
            Triple("lentilhas", "Lentilhas cozidas", 116),
        )
        val micros = mapOf(
            "tomilho" to mapOf("iron_mg" to 124.0),
            "alga" to mapOf("iron_mg" to 205.0),
            "figado" to mapOf("iron_mg" to 6.5),
            "lentilhas" to mapOf("iron_mg" to 3.3),
        )
        val ranked = NutrientDensity.rank(foods, micros, "iron_mg", ironDrv)
        val ids = ranked.map { it.foodId }

        assertTrue("tomilho" !in ids, "tomilho seco não é resposta para o ferro")
        assertTrue("alga" !in ids, "alga seca também não")
        assertEquals(listOf("figado", "lentilhas"), ids, "sobra comida a sério, pela densidade")
    }

}
