package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Quanto pesa **uma unidade tua**.
 *
 * A tabela diz que uma fatia de queijo tem 30 g. A tua faca corta 45, e o teu pão é o do teu
 * padeiro. A porção da tabela é a mediana de uma medição feita noutro sítio; a tua é uma
 * medição do que tu comes, e para ti é melhor.
 *
 * O que aqui se protege é o silêncio: a app só diz alguma coisa quando há mesmo um hábito, e
 * cala-se em todos os outros casos. Um número inventado no lugar de uma porção é pior do que
 * não haver porção nenhuma.
 */
class UnidadeAprendidaTest {

    @Test
    fun `sem porcao na tabela nao ha unidade a aprender`() {
        assertNull(UsualPortion.unidadeDe(listOf(45.0, 45.0, 45.0), null))
        assertNull(UsualPortion.unidadeDe(listOf(45.0, 45.0, 45.0), 0.0))
    }

    /** Menos de três registos são refeições, não um hábito. */
    @Test
    fun `sem habito nao ha unidade a aprender`() {
        assertNull(UsualPortion.unidadeDe(listOf(45.0, 45.0), 30.0))
        assertNull(UsualPortion.unidadeDe(emptyList(), 30.0))
    }

    @Test
    fun `a tua fatia e maior do que a da tabela`() {
        val minha = UsualPortion.unidadeDe(listOf(45.0, 45.0, 46.0), gramasDaTabela = 30.0)
        assertEquals(45.0, minha!!, 1e-9)
    }

    /**
     * Quem come várias unidades de uma vez não ganha uma unidade nova.
     *
     * 105 g de um alimento cuja fatia são 30 é três fatias e meia. A tabela já diz quanto
     * pesa uma, e o hábito de 105 g já vai preenchido no campo — inventar daí «a tua fatia
     * tem 35 g» era dividir por um número que ninguém escolheu.
     *
     * A primeira versão desta conta fazia exactamente isso, e um hábito de 45 g com uma
     * fatia de 30 saía como 22,5: meia fatia, que ninguém corta nem come.
     */
    @Test
    fun `quem come varias unidades nao ganha unidade nova`() {
        assertNull(UsualPortion.unidadeDe(listOf(105.0, 105.0, 105.0), gramasDaTabela = 30.0))
    }

    /**
     * Um hábito igual ao da tabela não se repete no ecrã.
     *
     * Duas linhas com o mesmo número e nomes diferentes não ajudam ninguém — só ocupam a
     * linha de atalhos, que é onde se escolhe depressa.
     */
    @Test
    fun `um habito igual ao da tabela nao se mostra`() {
        assertNull(UsualPortion.unidadeDe(listOf(30.0, 30.0, 31.0), gramasDaTabela = 30.0))
        assertNull(UsualPortion.unidadeDe(listOf(32.0, 32.0, 32.0), gramasDaTabela = 30.0))
    }

    /**
     * Acima de seis unidades a divisão deixa de descrever um hábito.
     *
     * Quem regista 300 g de um alimento cuja porção são 30 come 300 g. A fatia não é a
     * unidade dele, e dividir dava-lhe uma fatia que ele nunca comeu.
     */
    @Test
    fun `muitas unidades deixam de ser um habito`() {
        assertNull(UsualPortion.unidadeDe(listOf(300.0, 300.0, 300.0), gramasDaTabela = 30.0))
    }

    /**
     * Uma unidade absurdamente diferente não é a mesma coisa cortada de outra maneira.
     *
     * É outra comida, ou um zero a mais que se repetiu. Uma fatia de 30 g que a app
     * aprendesse como 9 g não é uma fatia — é uma lasca.
     */
    @Test
    fun `uma unidade fora de escala nao se aprende`() {
        assertNull(UsualPortion.unidadeDe(listOf(9.0, 9.0, 9.0), gramasDaTabela = 30.0))
        assertNull(UsualPortion.unidadeDe(listOf(80.0, 80.0, 80.0), gramasDaTabela = 30.0))
    }

    /** Uma unidade ao dobro ainda é a mesma coisa cortada com outra faca. */
    @Test
    fun `o dobro ainda e a mesma comida`() {
        val minha = UsualPortion.unidadeDe(listOf(58.0, 58.0, 59.0), gramasDaTabela = 30.0)
        assertEquals(58.0, minha!!, 1e-9)
    }

    @Test
    fun `a tua fatia tambem pode ser menor`() {
        val minha = UsualPortion.unidadeDe(listOf(20.0, 20.0, 21.0), gramasDaTabela = 30.0)
        assertEquals(20.0, minha!!, 1e-9)
    }
}
