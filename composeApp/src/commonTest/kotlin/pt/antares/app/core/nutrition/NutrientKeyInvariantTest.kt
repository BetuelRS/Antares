package pt.antares.app.core.nutrition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutrientKeyInvariantTest {

    @Test
    fun `toda a chave canonica declara a unidade no sufixo`() {
        for (key in Nutrients.ALL) {
            assertTrue(
                key.endsWith("_mg") || key.endsWith("_ug") || key.endsWith("_g"),
                "$key não diz a unidade — e a unidade é lida do sufixo em toda a app",
            )
            assertTrue(Nutrients.unitOf(key).isNotEmpty(), "$key sem unidade legível")
        }
    }

    @Test
    fun `nao ha chaves repetidas entre grupos`() {
        val groups = listOf(
            "vitaminas" to Nutrients.VITAMINS,
            "minerais" to Nutrients.MINERALS,
            "outros" to Nutrients.OTHERS,
        )
        val seen = mutableMapOf<String, String>()
        for ((name, keys) in groups) {
            for (k in keys) {
                val previous = seen.put(k, name)
                assertEquals(null, previous, "$k está em '$previous' e em '$name' — apareceria duas vezes")
            }
        }
    }

    @Test
    fun `toda a chave canonica tem nome tradutivel`() {

        for (key in Nutrients.ALL) {
            assertTrue(
                microLabelRes(key) != fallbackLabel,
                "$key não tem nome próprio em MicroLabels — apareceria com o nome errado",
            )
        }
    }

    @Test
    fun `o painel cobre todas as chaves canonicas`() {

        val shown = (Nutrients.VITAMINS + Nutrients.MINERALS + Nutrients.OTHERS + Nutrients.LABEL).toSet()
        for (key in Nutrients.ALL) {
            assertTrue(key in shown, "$key não pertence a nenhuma secção do painel")
        }
    }

    @Test
    fun `normalize recusa o que nao e canonico`() {
        val out = Nutrients.normalize(mapOf("calcium" to 100.0, "calcium_mg" to 120.0))
        assertEquals(mapOf("calcium_mg" to 120.0), out, "só a chave canónica sobrevive")
    }

    private val fallbackLabel = microLabelRes("__isto_nao_existe__")
}
