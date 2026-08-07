package pt.antares.app.core.nutrition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NutrientsTest {

    @Test
    fun `chaves da Open Food Facts caem nas canonicas`() {

        assertEquals(Nutrients.CALCIUM, Nutrients.canonical("calcium"))
        assertEquals(Nutrients.IRON, Nutrients.canonical("iron"))
        assertEquals(Nutrients.POTASSIUM, Nutrients.canonical("potassium"))
        assertEquals(Nutrients.VIT_C, Nutrients.canonical("vitaminC"))
    }

    @Test
    fun `pontuacao e maiusculas nao interessam`() {
        assertEquals(Nutrients.VIT_C, Nutrients.canonical("vitamin-c"))
        assertEquals(Nutrients.VIT_C, Nutrients.canonical("Vitamin_C"))
        assertEquals(Nutrients.VIT_B12, Nutrients.canonical("vitamin b12"))
    }

    @Test
    fun `normalizar e idempotente`() {
        val once = Nutrients.normalize(mapOf("calcium" to 120.0))
        val twice = Nutrients.normalize(once)
        assertEquals(once, twice)
        assertEquals(mapOf(Nutrients.CALCIUM to 120.0), once)
    }

    @Test
    fun `zeros e valores invalidos saem`() {

        val out = Nutrients.normalize(
            mapOf(
                "calcium" to 0.0,
                "iron" to -3.0,
                "zinc" to null,
                "potassium" to Double.NaN,
                "magnesium" to 40.0,
            ),
        )
        assertEquals(mapOf(Nutrients.MAGNESIUM to 40.0), out)
    }

    @Test
    fun `chave desconhecida e descartada, nunca adivinhada`() {
        assertNull(Nutrients.canonical("unobtainium"))
        assertTrue(Nutrients.normalize(mapOf("unobtainium" to 9.0)).isEmpty())
    }

    @Test
    fun `colisao de alias fica com o valor medido`() {

        val out = Nutrients.normalize(mapOf("calcium" to 0.0, "calcium_mg" to 200.0))
        assertEquals(200.0, out[Nutrients.CALCIUM])
    }

    @Test
    fun `merge preenche buracos sem sobrepor a fonte principal`() {

        val ciqual = mapOf(Nutrients.VIT_C to 30.0, Nutrients.CALCIUM to 100.0)
        val usda = mapOf(Nutrients.VIT_C to 999.0, Nutrients.VIT_E to 2.5)
        val out = Nutrients.merge(primary = ciqual, fallback = usda)

        assertEquals(30.0, out[Nutrients.VIT_C], "a fonte principal manda")
        assertEquals(2.5, out[Nutrients.VIT_E], "o buraco é preenchido")
        assertEquals(100.0, out[Nutrients.CALCIUM])
    }

    @Test
    fun `unidade vem do sufixo da chave`() {
        assertEquals("µg", Nutrients.unitOf(Nutrients.VIT_B12))
        assertEquals("mg", Nutrients.unitOf(Nutrients.CALCIUM))
        assertEquals("", Nutrients.unitOf("qualquer_coisa"))
    }

    @Test
    fun `todas as canonicas tem unidade reconhecida`() {
        Nutrients.ALL.forEach { key ->
            assertTrue(Nutrients.unitOf(key).isNotEmpty(), "sem unidade: $key")
            assertEquals(key, Nutrients.canonical(key), "não é idempotente: $key")
        }
    }
}
