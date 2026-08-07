package pt.antares.app.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FtsQueryTest {

    @Test
    fun `remove stopwords e prefixa cada token`() {

        assertEquals("cafe* leite*", FtsQuery.build("café com leite"))
    }

    @Test
    fun `remove acentos e minusculiza`() {
        assertEquals("frango*", FtsQuery.build("Frango"))
        assertEquals("pao*", FtsQuery.build("pão"))
    }

    @Test
    fun `separadores nao alfanumericos tambem dividem`() {
        assertEquals("arroz* feijao*", FtsQuery.build("arroz, feijão"))
    }

    @Test
    fun `so stopwords cai para os tokens crus`() {

        assertEquals("com*", FtsQuery.build("com"))
    }

    @Test
    fun `vazio ou muito curto devolve vazio`() {
        assertEquals("", FtsQuery.build(""))
        assertEquals("", FtsQuery.build("a"))
        assertEquals("", FtsQuery.build("   "))
    }

    @Test
    fun `ignora tokens de uma letra mas mantem os validos`() {
        assertEquals("ovo*", FtsQuery.build("o ovo"))
    }

    @Test
    fun `tokens devolve termos normalizados sem stopwords`() {
        assertEquals(listOf("cafe", "leite"), FtsQuery.tokens("Café com leite"))
    }

    @Test
    fun `tokens vazio quando so ha ruido`() {
        assertEquals(emptyList(), FtsQuery.tokens("   "))
    }

    @Test
    fun `tokens so com stopwords cai para os crus`() {
        assertEquals(listOf("com"), FtsQuery.tokens("com"))
    }
}
