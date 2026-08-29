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
 * **São os três do esboço 03: Tudo · Favoritos · Meus.** O que este teste protege é que nada
 * se perdeu. Um separador a menos não é uma coisa arrumada se a lista que ele mostrava
 * deixou de existir — é comida escondida. As refeições guardadas, os mais registados e os
 * recentes passam a ser secções do «Tudo», por essa ordem.
 */
class SeparadoresDaPesquisaTest {

    @Test
    fun `sao tres separadores`() {
        assertEquals(3, SearchTab.entries.size, "eram seis, e a redução é a versão")
    }

    @Test
    fun `os tres respondem a perguntas diferentes`() {
        assertEquals(
            listOf(SearchTab.TUDO, SearchTab.FAVORITOS, SearchTab.MEUS),
            SearchTab.entries.toList(),
            "a ordem é a do esboço 03, e a da frequência: tudo é diário, criar é raro",
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
        assertEquals(SearchTab.TUDO, FoodSearchState().tab)
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
        // O «REFEICOES» juntou-se à lista: era separador e passou a secção do «Tudo».
        for (antigo in listOf("RECENTS", "FAVORITES", "RECIPES", "TEMPLATES", "REFEICOES")) {
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

    /**
     * O «Tudo» abre nas refeições guardadas, e não nos alimentos.
     *
     * É a proposta 2 do esboço 03, e a ordem dele: AS TUAS REFEIÇÕES · O QUE COMES MAIS ·
     * RECENTES. **Já esteve ao contrário**: as refeições saíram do ecrã de abertura a
     * 2026-08-29, quando fechei a porta errada de duas que o estudo dizia serem uma a mais.
     * O que ficou fechado foi a secção, e o estudo mandava fechar o separador.
     */
    @Test
    fun `o tudo e o primeiro separador`() {
        assertEquals(SearchTab.TUDO, SearchTab.entries.first())
    }

    /** O separador do meio é o dos favoritos, e não o dos alimentos criados. */
    @Test
    fun `os favoritos ganharam separador proprio`() {
        assertTrue(SearchTab.FAVORITOS in SearchTab.entries)
        assertEquals(1, SearchTab.entries.indexOf(SearchTab.FAVORITOS))
    }
}
