package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * O ecrã do ciclo só sabia marcar «hoje», e por isso não havia datas para recusar. Com datas
 * à escolha há — e uma data errada não dá erro nenhum: entra na mediana dos ciclos e leva
 * consigo a previsão do próximo período, que é a única coisa para que este ecrã serve.
 */
class CycleEditTest {

    private val hoje = 20_000L

    private fun periodo(id: String, start: Long, end: Long? = null) =
        CycleEdit.Periodo(id, start, end)

    @Test
    fun `um inicio no passado e aceite`() {
        assertNull(CycleEdit.validateStart(hoje - 3, hoje, emptyList()))
    }

    @Test
    fun `um inicio no futuro e recusado`() {
        assertEquals(
            CycleDateError.NO_FUTURO,
            CycleEdit.validateStart(hoje + 1, hoje, emptyList()),
        )
    }

    @Test
    fun `hoje conta como passado`() {
        assertNull(CycleEdit.validateStart(hoje, hoje, emptyList()))
    }

    @Test
    fun `um inicio dentro de um periodo existente e recusado`() {
        val existentes = listOf(periodo("a", start = hoje - 10, end = hoje - 5))

        assertEquals(
            CycleDateError.SOBREPOE,
            CycleEdit.validateStart(hoje - 7, hoje, existentes),
        )
        assertNull(CycleEdit.validateStart(hoje - 4, hoje, existentes), "o dia a seguir está livre")
    }

    @Test
    fun `um periodo a decorrer ocupa ate hoje`() {
        // Sem fim, o período vai do início até hoje: marcar um início a meio dele seria
        // dizer que houve dois períodos ao mesmo tempo.
        val existentes = listOf(periodo("aberto", start = hoje - 4, end = null))

        assertEquals(
            CycleDateError.SOBREPOE,
            CycleEdit.validateStart(hoje - 2, hoje, existentes),
        )
    }

    @Test
    fun `corrigir um periodo nao choca consigo mesmo`() {
        val existentes = listOf(periodo("a", start = hoje - 10, end = hoje - 5))

        assertNull(
            CycleEdit.validateStart(hoje - 9, hoje, existentes, aIgnorar = "a"),
            "mover o início dentro do próprio período não é sobreposição",
        )
    }

    @Test
    fun `o fim nao pode ser antes do inicio`() {
        assertEquals(
            CycleDateError.FIM_ANTES_DO_INICIO,
            CycleEdit.validateEnd(inicio = hoje - 3, novoFim = hoje - 5, hoje = hoje, existentes = emptyList()),
        )
    }

    @Test
    fun `o fim no mesmo dia do inicio e aceite`() {
        // Um período de um dia é curto, mas acontece — e recusá-lo obrigava a inventar.
        assertNull(
            CycleEdit.validateEnd(inicio = hoje - 3, novoFim = hoje - 3, hoje = hoje, existentes = emptyList()),
        )
    }

    @Test
    fun `o fim nao pode engolir o ciclo seguinte`() {
        val existentes = listOf(
            periodo("a", start = hoje - 40),
            periodo("b", start = hoje - 10, end = hoje - 6),
        )

        assertEquals(
            CycleDateError.SOBREPOE,
            CycleEdit.validateEnd(
                inicio = hoje - 40,
                novoFim = hoje - 5,
                hoje = hoje,
                existentes = existentes,
                aIgnorar = "a",
            ),
            "o fim passava por cima do período seguinte, e a mediana dos ciclos ia com ele",
        )
        assertNull(
            CycleEdit.validateEnd(
                inicio = hoje - 40,
                novoFim = hoje - 35,
                hoje = hoje,
                existentes = existentes,
                aIgnorar = "a",
            ),
        )
    }

    @Test
    fun `o fim no futuro e recusado`() {
        assertEquals(
            CycleDateError.NO_FUTURO,
            CycleEdit.validateEnd(inicio = hoje - 3, novoFim = hoje + 2, hoje = hoje, existentes = emptyList()),
        )
    }
}
