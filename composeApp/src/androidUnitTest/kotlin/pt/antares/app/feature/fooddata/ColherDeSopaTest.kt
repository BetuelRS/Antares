package pt.antares.app.feature.fooddata

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A quem é que a app oferece uma colher de sopa.
 *
 * A área 03 do estudo põe-na em «o que é inútil», e o argumento tem duas metades: *«é uma
 * medida de volume aplicada a tudo: faz sentido no azeite e não no bife»*. A primeira
 * correcção escondeu-a onde há porção nomeada — e resolveu a metade do arroz. **A do bife
 * ficou por resolver durante duas passagens da auditoria**, porque as duas leram a regra
 * escrita no código e a regra estava cumprida: um frango cru não tem porção nomeada nenhuma,
 * e por isso continuava a receber uma colher de sopa de frango cru.
 *
 * Só apareceu com o alimento no ecrã ao lado do desenho do esboço. É por isso que a regra
 * vive numa função e tem um teste: para a próxima vez que alguém a mudar, a decisão discute-se
 * aqui e não numa captura de ecrã.
 */
class ColherDeSopaTest {

    @Test
    fun `um liquido sem porcao nomeada recebe a colher`() {
        assertTrue(
            mostraColherDeSopa(liquido = true, semPorcoesExtra = true, porcaoDaFonteG = null),
            "o azeite é o exemplo do próprio estudo",
        )
    }

    @Test
    fun `um solido nao recebe a colher, tenha ou nao porcao`() {
        assertFalse(
            mostraColherDeSopa(liquido = false, semPorcoesExtra = true, porcaoDaFonteG = null),
            "é o «15 g de bife» do estudo — e era este o caso que sobrava",
        )
        assertFalse(mostraColherDeSopa(liquido = false, semPorcoesExtra = false, porcaoDaFonteG = 300.0))
    }

    /**
     * As porções nomeadas vêm de **dois** sítios, e a primeira versão da regra só olhava para
     * um. No aparelho, o «Arroz carreteiro» mostrava «porção (300 g)» e a colher ao lado.
     */
    @Test
    fun `uma porcao nomeada ganha a colher, venha de onde vier`() {
        assertFalse(
            mostraColherDeSopa(liquido = true, semPorcoesExtra = false, porcaoDaFonteG = null),
            "as porções extra são medidas deste alimento; a colher é genérica",
        )
        assertFalse(
            mostraColherDeSopa(liquido = true, semPorcoesExtra = true, porcaoDaFonteG = 200.0),
            "a porção da própria fonte conta tanto como as extra",
        )
    }
}
