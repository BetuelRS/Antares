package pt.antares.app.feature.fooddata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Três separadores, e não seis.
 *
 * Seis separadores numa fila que rola é uma escolha entre seis antes de escrever a primeira
 * letra, e quatro deles respondiam à mesma pergunta: «o que é que eu já comi?».
 *
 * **O que este teste protege é que nada se perdeu.** Um separador a menos não é uma coisa
 * arrumada se a lista que ele mostrava deixou de existir — é comida escondida. Os recentes e
 * os favoritos passam a viver dentro do «Procurar» com a caixa vazia, e as receitas e os
 * modelos partilham o «Refeições».
 */
class SeparadoresDaPesquisaTest {

    @Test
    fun `sao tres separadores`() {
        assertEquals(3, SearchTab.entries.size, "eram seis, e a redução é a versão")
    }

    @Test
    fun `os tres respondem a perguntas diferentes`() {
        assertEquals(
            listOf(SearchTab.SEARCH, SearchTab.MINE, SearchTab.REFEICOES),
            SearchTab.entries.toList(),
            "a ordem é a da frequência: procurar é diário, criar é raro",
        )
    }

    /**
     * A pesquisa abre no «Procurar», e não noutro qualquer.
     *
     * É o único que responde a «quero uma coisa que ainda não sei nomear», e é por isso que
     * abre lá — os outros dois pressupõem que já se sabe o que se quer.
     */
    @Test
    fun `abre no procurar`() {
        assertEquals(SearchTab.SEARCH, FoodSearchState().tab)
    }

    /**
     * Nenhum dos separadores antigos sobrou como valor morto.
     *
     * Um `RECENTS` que continuasse a existir no tipo e não aparecesse em lado nenhum seria um
     * ramo de código que ninguém percorre — e o `when` que os distribui deixaria de ser
     * exaustivo sem o compilador dizer nada.
     */
    @Test
    fun `os separadores antigos nao ficaram para tras`() {
        val nomes = SearchTab.entries.map { it.name }
        for (antigo in listOf("RECENTS", "FAVORITES", "RECIPES", "TEMPLATES")) {
            assertTrue(antigo !in nomes, "«$antigo» continua no tipo depois de sair do ecrã")
        }
    }

    /** O estado começa sem nada aberto e sem nada marcado: é um ecrã acabado de abrir. */
    @Test
    fun `o estado inicial esta vazio`() {
        val s = FoodSearchState()
        assertTrue(s.query.isEmpty())
        assertTrue(s.results.isEmpty())
        assertTrue(s.grupos.isEmpty())
        assertTrue(s.selected.isEmpty())
        assertTrue(s.estadosAbertos.isEmpty())
    }
}
