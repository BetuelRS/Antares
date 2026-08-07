package pt.antares.app.feature.fooddata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchMissRuleTest {

    @Test
    fun `duas letras ainda e alguem a escrever`() {
        assertNull(SearchMissRule.normalize("fr"))
        assertNull(SearchMissRule.normalize("a"))
        assertNull(SearchMissRule.normalize(""))
    }

    @Test
    fun `tres letras ja e uma pesquisa`() {
        assertEquals("pao", SearchMissRule.normalize("pão"))
    }

    @Test
    fun `maiusculas, acentos e espacos dao a mesma entrada`() {

        val esperado = SearchMissRule.normalize("frango")
        assertEquals(esperado, SearchMissRule.normalize("Frango"))
        assertEquals(esperado, SearchMissRule.normalize("  FRANGO  "))
    }

    @Test
    fun `um codigo de barras escrito a mao nao e um alimento em falta`() {
        assertNull(SearchMissRule.normalize("5601234567890"))
        assertNull(SearchMissRule.normalize("560 123 456"))
    }

    @Test
    fun `texto colado por engano nao entra`() {
        val enorme = "a".repeat(SearchMissRule.MAX_QUERY_LENGTH + 1)
        assertNull(SearchMissRule.normalize(enorme))
    }

    @Test
    fun `sem resultados em lado nenhum e uma falha`() {
        assertTrue(SearchMissRule.shouldRecord("bacalhau à brás", localHits = 0, onlineHits = 0))
    }

    @Test
    fun `se o catalogo local encontrou, nao ha falha nenhuma`() {
        assertFalse(SearchMissRule.shouldRecord("frango", localHits = 3, onlineHits = 0))
    }

    @Test
    fun `se a Open Food Facts encontrou, tambem nao ha falha`() {

        assertFalse(SearchMissRule.shouldRecord("nutella", localHits = 0, onlineHits = 5))
    }

    @Test
    fun `sem rede nao se regista nada`() {

        assertFalse(SearchMissRule.shouldRecord("bacalhau", localHits = 0, onlineHits = null))
    }

    @Test
    fun `uma pesquisa por acabar nao se regista mesmo sem resultados`() {
        assertFalse(SearchMissRule.shouldRecord("ba", localHits = 0, onlineHits = 0))
    }

    @Test
    fun `varrimento - nunca se regista quando ha resultados`() {
        var casos = 0
        for (local in 0..3) {
            for (online in listOf(null, 0, 1, 7)) {
                val registou = SearchMissRule.shouldRecord("bacalhau", local, online)
                val deviaRegistar = local == 0 && online == 0
                assertEquals(deviaRegistar, registou, "local=$local online=$online")
                casos++
            }
        }
        assertTrue(casos == 16)
    }
}
