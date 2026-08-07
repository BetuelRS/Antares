package pt.antares.app.core.network.off

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServingParseTest {

    @Test
    fun `o campo numerico da OFF e o preferido`() {
        assertEquals(30.0, ServingParse.from("30", "1 pacote (30 g)").grams)
    }

    @Test
    fun `virgula tambem e numero`() {
        assertEquals(12.5, ServingParse.grams("12,5"))
    }

    @Test
    fun `porcoes impossiveis nao entram`() {
        assertNull(ServingParse.grams("0"))
        assertNull(ServingParse.grams("-5"))
        assertNull(ServingParse.grams("5000"))
        assertNull(ServingParse.grams("uma dose"))
        assertNull(ServingParse.grams(null))
    }

    @Test
    fun `sem campo numerico le-se o texto`() {
        assertEquals(30.0, ServingParse.from(null, "30 g").grams)
        assertEquals(250.0, ServingParse.from(null, "1 chávena (250 ml)").grams)
    }

    @Test
    fun `numero de unidades nao e o peso`() {

        assertEquals(25.0, ServingParse.from(null, "2 bolachas (25 g)").grams)
    }

    @Test
    fun `sem numero nenhum nao ha porcao`() {
        assertNull(ServingParse.from(null, "uma dose").grams)
        assertNull(ServingParse.from(null, "").grams)
        assertNull(ServingParse.from(null, null).grams)
    }

    @Test
    fun `um numero absurdo no texto tambem e recusado`() {
        assertNull(ServingParse.from(null, "1 saco (5000 g)").grams)
    }

    @Test
    fun `nem toda a letra depois do numero conta como unidade`() {

        assertNull(ServingParse.gramsFromText("30 kg"))
    }

    @Test
    fun `so o peso nao vale como nome`() {

        assertNull(ServingParse.label("30 g"))
        assertNull(ServingParse.label("100g"))
    }

    @Test
    fun `a parte que descreve a dose fica`() {
        assertEquals("1 chávena", ServingParse.label("1 chávena (250 ml)"))
        assertEquals("2 bolachas", ServingParse.label("2 bolachas (25 g)"))
    }

    @Test
    fun `um texto enorme nao e um nome de porcao`() {
        val enorme = "porção recomendada pelo fabricante para consumo diário de adultos saudáveis"
        assertNull(ServingParse.label(enorme))
    }

    @Test
    fun `vazio e nulo dao nome nenhum`() {
        assertNull(ServingParse.label(""))
        assertNull(ServingParse.label("   "))
        assertNull(ServingParse.label(null))
    }

    @Test
    fun `nome e gramas saem juntos quando ha os dois`() {
        val porcao = ServingParse.from("250", "1 chávena (250 ml)")
        assertEquals("1 chávena", porcao.name)
        assertEquals(250.0, porcao.grams)
    }

    @Test
    fun `sem informacao nenhuma fica tudo por saber`() {
        val porcao = ServingParse.from(null, null)
        assertNull(porcao.name)
        assertNull(porcao.grams)
    }

    @Test
    fun `um numero recusado nao arrasta o nome consigo`() {

        val porcao = ServingParse.from("9999", "1 embalagem (9999 g)")
        assertEquals("1 embalagem", porcao.name)
        assertNull(porcao.grams)
    }

    @Test
    fun `varrimento - nenhuma entrada produz uma porcao fora dos limites`() {
        val entradas = listOf(
            null, "", " ", "0", "-1", "abc", "30", "30 g", "1 chávena (250 ml)",
            "2 bolachas (25g)", "5000", "1,5", "1 unidade", "100 ml", "3500 g",
        )
        var vistos = 0
        for (quantidade in entradas) {
            for (tamanho in entradas) {
                val porcao = ServingParse.from(quantidade, tamanho)
                porcao.grams?.let {
                    kotlin.test.assertTrue(
                        it >= ServingParse.MIN_GRAMS && it <= ServingParse.MAX_GRAMS,
                        "porção fora dos limites: $it (de '$quantidade' / '$tamanho')",
                    )
                }
                porcao.name?.let {
                    kotlin.test.assertTrue(it.length <= ServingParse.MAX_LABEL_LENGTH)
                    kotlin.test.assertTrue(it.isNotBlank())
                }
                vistos++
            }
        }
        kotlin.test.assertTrue(vistos > 200, "varrimento encolheu: $vistos")
    }
}
