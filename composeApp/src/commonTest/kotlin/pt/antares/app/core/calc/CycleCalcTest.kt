package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CycleCalcTest {

    private val d0 = 20_000L

    private fun inicios(quantos: Int, dias: Long) = (0 until quantos).map { d0 + it * dias }

    @Test
    fun `sem dois inicios nao ha comprimento`() {
        assertNull(CycleCalc.averageCycleDays(emptyList()))
        assertNull(CycleCalc.averageCycleDays(listOf(d0)))
    }

    @Test
    fun `dois inicios ja dao um comprimento`() {
        assertEquals(28, CycleCalc.averageCycleDays(listOf(d0, d0 + 28)))
    }

    @Test
    fun `um ciclo muito longo nao estraga a estimativa dos outros`() {

        val comOutlier = listOf(d0, d0 + 28, d0 + 56, d0 + 108, d0 + 136)
        assertEquals(28, CycleCalc.averageCycleDays(comOutlier))
    }

    @Test
    fun `um mes esquecido nao vira um ciclo de dois meses`() {

        val comFalha = listOf(d0, d0 + 28, d0 + 84, d0 + 112)
        assertEquals(28, CycleCalc.averageCycleDays(comFalha))
        assertTrue(CycleCalc.gaps(comFalha).none { it > CycleCalc.PLAUSIBLE_GAP_DAYS.last })
    }

    @Test
    fun `datas absurdas nao entram na conta`() {

        assertNull(CycleCalc.averageCycleDays(listOf(d0, d0 + 3)))
    }

    @Test
    fun `ciclos irregulares continuam a ser lidos`() {

        val irregular = listOf(d0, d0 + 24, d0 + 55, d0 + 82)
        val media = CycleCalc.averageCycleDays(irregular)!!
        assertTrue(media in 24..31, "média fora dos dados: $media")
    }

    @Test
    fun `o dia do inicio e o dia um, nao o dia zero`() {
        assertEquals(1, CycleCalc.dayOfCycle(listOf(d0), d0))
        assertEquals(15, CycleCalc.dayOfCycle(listOf(d0), d0 + 14))
    }

    @Test
    fun `conta-se do ultimo inicio registado`() {
        val varios = inicios(3, 28)
        assertEquals(1, CycleCalc.dayOfCycle(varios, varios.last()))
    }

    @Test
    fun `uma data escrita no futuro nao da dias negativos`() {

        assertNull(CycleCalc.dayOfCycle(listOf(d0 + 100), d0))
    }

    @Test
    fun `sem registos nao ha dia do ciclo`() {
        assertNull(CycleCalc.dayOfCycle(emptyList(), d0))
    }

    @Test
    fun `o proximo inicio e o ultimo mais um ciclo`() {
        val varios = inicios(4, 28)
        assertEquals(varios.last() + 28, CycleCalc.predictedNextStart(varios))
    }

    @Test
    fun `sem comprimento nao se preve nada`() {
        assertNull(CycleCalc.predictedNextStart(listOf(d0)))
        assertNull(CycleCalc.predictedNextStart(emptyList()))
    }

    @Test
    fun `os primeiros dias do periodo trazem retencao`() {
        val varios = inicios(4, 28)
        assertTrue(CycleCalc.retentionLikely(varios, varios.last()))
        assertTrue(CycleCalc.retentionLikely(varios, varios.last() + 2))
    }

    @Test
    fun `a semana antes do proximo inicio tambem`() {
        val varios = inicios(4, 28)

        assertTrue(CycleCalc.retentionLikely(varios, varios.last() + 23))
    }

    @Test
    fun `o meio do ciclo nao tem retencao esperavel`() {
        val varios = inicios(4, 28)

        assertFalse(CycleCalc.retentionLikely(varios, varios.last() + 12))
    }

    @Test
    fun `sem dados nao se afirma retencao nenhuma`() {

        assertFalse(CycleCalc.retentionLikely(emptyList(), d0))

        assertFalse(CycleCalc.retentionLikely(listOf(d0), d0 + 15))
    }

    @Test
    fun `varrimento - a retencao nunca cobre o ciclo inteiro`() {

        for (comprimento in 21L..35L) {
            val varios = (0 until 4).map { d0 + it * comprimento }
            val ultimo = varios.last()
            val dias = (0 until comprimento).count {
                CycleCalc.retentionLikely(varios, ultimo + it)
            }
            assertTrue(dias > 0, "nunca avisa num ciclo de $comprimento dias")
            assertTrue(
                dias < comprimento,
                "avisa todos os dias de um ciclo de $comprimento — deixa de dizer nada",
            )
        }
    }

    @Test
    fun `um periodo de um dia dura um dia`() {
        assertEquals(1, CycleCalc.periodLengthDays(d0, d0))
    }

    @Test
    fun `cinco dias contam-se com as duas pontas`() {
        assertEquals(5, CycleCalc.periodLengthDays(d0, d0 + 4))
    }

    @Test
    fun `um periodo por fechar nao tem duracao`() {

        assertNull(CycleCalc.periodLengthDays(d0, null))
    }

    @Test
    fun `um fim antes do inicio nao e uma duracao negativa`() {
        assertNull(CycleCalc.periodLengthDays(d0, d0 - 3))
    }
}
