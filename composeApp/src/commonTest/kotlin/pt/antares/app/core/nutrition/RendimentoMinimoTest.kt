package pt.antares.app.core.nutrition

import pt.antares.app.core.confecao.LinhaDeConfecao
import pt.antares.app.core.confecao.TabelaDeConfecao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * O chão do que uma confeção publicada explica.
 *
 * **Nasce de um buraco que a área 05 do estudo apanhou e a 2.18.1 só fechou a meio.** O
 * aviso do peso final compara com o envelope de métodos desta receita — e esse envelope é
 * nulo quando nenhum ingrediente tem família de confeção, que é a maioria das receitas.
 * Nesse caso a app não dizia nada: 1 200 g de ingredientes com 50 g de peso final passavam
 * em silêncio, e os valores por 100 g saíam vinte e quatro vezes errados.
 *
 * O estudo pedia aqui «a mesma tolerância do aviso do rótulo», que são 10 %. **Medi e não
 * serve** — a tabela publica rendimentos que descem a 0,39, o que quer dizer que perder 40 %
 * do peso a cozinhar é vulgar. O chão sai da tabela, e não de um número escolhido por mim.
 */
class RendimentoMinimoTest {

    private fun tabela(vararg rendimentos: Double?) = TabelaDeConfecao(
        versao = 1,
        linhas = rendimentos.mapIndexed { i, r ->
            LinhaDeConfecao(familia = "f$i", metodo = "m", rendimento = r)
        },
    )

    @Test
    fun `o chao e o rendimento mais baixo da tabela`() {
        assertEquals(0.39, tabela(0.82, 0.39, 0.61).rendimentoMinimo)
    }

    /** Uma linha sem rendimento publicado é «ninguém mediu», e não «zero». */
    @Test
    fun `as linhas sem rendimento nao puxam o chao para baixo`() {
        assertEquals(0.5, tabela(0.5, null, 0.9).rendimentoMinimo)
    }

    /**
     * Sem tabela não há opinião. É o que acontece quando o ficheiro de confeção não abre —
     * a app funciona na mesma, e apenas não avisa sobre peso nenhum.
     */
    @Test
    fun `sem rendimentos publicados nao ha chao`() {
        assertNull(TabelaDeConfecao.VAZIA.rendimentoMinimo)
        assertNull(tabela(null, null).rendimentoMinimo)
    }

    /**
     * O que o chão deixa passar, de propósito.
     *
     * O próprio exemplo do estudo — 500 g declarados em 1 200 g de ingredientes — dá 0,42 e
     * **não** é acusado: é fisicamente possível cozinhar assim. Acusar isto seria acusar
     * metade dos estufados, que é o que a tolerância de 10 % do rótulo faria.
     */
    @Test
    fun `uma perda de peso vulgar nao e acusada`() {
        val chao = 1200.0 * tabela(0.39, 0.82).rendimentoMinimo!!
        assertEquals(468.0, chao)
        assertEquals(false, 500.0 < chao)
        assertEquals(true, 50.0 < chao)
    }
}
