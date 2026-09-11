package pt.antares.app.ui

import pt.antares.app.core.designsystem.components.GrelhaDeCartoesScope
import pt.antares.app.core.designsystem.components.blocosDaGrelha
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Os cartões pequenos vão de dois em dois, e é sobre os pares que as colunas alternam.
 *
 * A 2.32.1 deu à grelha um segundo tamanho. O que se guarda é a regra dos blocos: pequenos
 * **seguidos** fazem par; um pequeno sozinho fica sozinho em vez de esticar; e um grande no meio
 * parte o par — senão um pequeno antes da água e outro depois dela apareciam lado a lado, fora
 * da ordem em que foram pedidos.
 */
class GrelhaDeCartoesPequenosTest {

    private fun forma(pede: GrelhaDeCartoesScope.() -> Unit): List<String> {
        val scope = GrelhaDeCartoesScope().apply(pede)
        return blocosDaGrelha(scope.itens).map { bloco -> bloco.joinToString("") { if (it.pequeno) "p" else "G" } }
    }

    @Test
    fun `pequenos seguidos vao aos pares`() {
        assertEquals(listOf("pp", "pp"), forma { repeat(4) { pequeno {} } })
    }

    @Test
    fun `um pequeno sozinho fica sozinho`() {
        assertEquals(listOf("pp", "p"), forma { repeat(3) { pequeno {} } })
    }

    @Test
    fun `um grande no meio parte o par`() {
        assertEquals(listOf("p", "G", "p"), forma { pequeno {}; cartao {}; pequeno {} })
    }

    @Test
    fun `sem pequenos a grelha e a de sempre`() {
        assertEquals(listOf("G", "G", "G"), forma { repeat(3) { cartao {} } })
    }
}
