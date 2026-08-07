package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressCalcTest {

    private val quinta = 20_650L

    @Test
    fun `a grelha comeca a uma segunda-feira`() {
        val grid = ProgressCalc.consistencyGrid(emptySet(), today = quinta, weeks = 2)

        assertEquals(0L, (grid.first().epochDay + 3) % 7)
    }

    @Test
    fun `a grelha tem sete dias por semana`() {
        assertEquals(84, ProgressCalc.consistencyGrid(emptySet(), quinta, weeks = 12).size)
        assertEquals(14, ProgressCalc.consistencyGrid(emptySet(), quinta, weeks = 2).size)
    }

    @Test
    fun `os dias que ainda nao chegaram sao futuro, nao falha`() {

        val grid = ProgressCalc.consistencyGrid(emptySet(), today = quinta, weeks = 1)
        val futuros = grid.filter { it.inFuture }
        assertEquals(3, futuros.size, "de quinta sobram sexta, sábado e domingo")
        assertTrue(futuros.all { it.epochDay > quinta })
    }

    @Test
    fun `a percentagem ignora o futuro`() {

        val registados = setOf(quinta - 3, quinta - 2, quinta - 1, quinta)
        val grid = ProgressCalc.consistencyGrid(registados, today = quinta, weeks = 1)
        assertEquals(100, ProgressCalc.consistencyPct(grid))
    }

    @Test
    fun `sem dias passados a percentagem e zero e nao rebenta`() {
        assertEquals(0, ProgressCalc.consistencyPct(emptyList()))
    }

    @Test
    fun `zero semanas da grelha vazia`() {
        assertEquals(emptyList(), ProgressCalc.consistencyGrid(emptySet(), quinta, weeks = 0))
    }

    @Test
    fun `sem dados do lado de tras nao ha comparacao, so o valor atual`() {
        val c = ProgressCalc.compare(List(10) { 2000.0 }, emptyList())
        assertEquals(2000.0, c?.current)
        assertNull(c?.previous)
        assertNull(c?.delta)
        assertEquals(ProgressCalc.Direction.UNKNOWN, c?.direction)
    }

    @Test
    fun `poucos dias de um lado nao chegam para comparar`() {

        val c = ProgressCalc.compare(List(10) { 2000.0 }, List(3) { 1500.0 })
        assertNull(c?.previous, "três dias não são período")
    }

    @Test
    fun `poucos dias do lado atual nao produzem comparacao nenhuma`() {
        assertNull(ProgressCalc.compare(List(2) { 2000.0 }, List(10) { 1900.0 }))
    }

    @Test
    fun `compara quando ha dados dos dois lados`() {
        val c = ProgressCalc.compare(List(10) { 2100.0 }, List(10) { 2000.0 })!!
        assertEquals(100.0, c.delta)
        assertEquals(5.0, c.deltaPct!!, 0.001)
        assertEquals(ProgressCalc.Direction.UP, c.direction)
    }

    @Test
    fun `dividir por zero nunca produz infinito`() {

        val c = ProgressCalc.Comparison(current = 10.0, previous = 0.0)
        assertNull(c.deltaPct)
        assertEquals(10.0, c.delta)
    }

    @Test
    fun `um mes sem registos tem media nenhuma, nao media zero`() {

        assertNull(ProgressCalc.meanOrNull(emptyList()))
        assertEquals(2.0, ProgressCalc.meanOrNull(listOf(1.0, 2.0, 3.0)))
    }

    @Test
    fun `sem mudanca a direcao e plana`() {
        val c = ProgressCalc.Comparison(current = 2000.0, previous = 2000.0)
        assertEquals(ProgressCalc.Direction.FLAT, c.direction)
    }

    @Test
    fun `os marcos de registo trazem o dia em que aconteceram`() {

        val dias = (0 until 30).map { quinta - 29 + it }.toSet()
        val marcos = ProgressCalc.loggingMilestones(dias)
        assertEquals(listOf(7, 30), marcos.map { it.value })
        assertEquals(quinta - 29 + 6, marcos[0].epochDay)
        assertEquals(quinta, marcos[1].epochDay)
    }

    @Test
    fun `sem registos nao ha marcos`() {
        assertEquals(emptyList(), ProgressCalc.loggingMilestones(emptySet()))
    }

    @Test
    fun `marcos de peso contam a distancia percorrida`() {
        val pesagens = listOf(
            (quinta - 60) to 90.0,
            (quinta - 30) to 85.0,
            quinta to 84.0,
        )
        val marcos = ProgressCalc.weightMilestones(pesagens)
        assertEquals(listOf(5), marcos.map { it.value })
        assertEquals(quinta - 30, marcos[0].epochDay)
    }

    @Test
    fun `uma recaida nao apaga o marco de peso`() {

        val pesagens = listOf(
            (quinta - 60) to 90.0,
            (quinta - 30) to 84.0,
            quinta to 86.0,
        )
        assertEquals(listOf(5), ProgressCalc.weightMilestones(pesagens).map { it.value })
    }

    @Test
    fun `ganhar peso tambem da marcos`() {

        val pesagens = listOf((quinta - 90) to 60.0, quinta to 70.5)
        assertEquals(listOf(5, 10), ProgressCalc.weightMilestones(pesagens).map { it.value })
    }

    @Test
    fun `uma pesagem so nao da marco nenhum`() {
        assertEquals(emptyList(), ProgressCalc.weightMilestones(listOf(quinta to 80.0)))
    }

    @Test
    fun `os marcos de peso nao se repetem`() {
        val pesagens = (0..20).map { (quinta - 20 + it) to (90.0 - it * 0.5) }
        val marcos = ProgressCalc.weightMilestones(pesagens)
        assertEquals(marcos.map { it.value }.toSet().size, marcos.size)
        assertEquals(marcos.map { it.value }.sorted(), marcos.map { it.value })
    }

    @Test
    fun `varrimento - a grelha nunca inventa dias nem os perde`() {
        var vistos = 0
        for (offset in 0..90) {
            val hoje = quinta + offset
            val grid = ProgressCalc.consistencyGrid(emptySet(), hoje, weeks = 4)
            assertEquals(28, grid.size)
            assertEquals(grid.map { it.epochDay }.sorted(), grid.map { it.epochDay })
            assertEquals(grid.size, grid.map { it.epochDay }.toSet().size, "dias repetidos")
            assertTrue(grid.any { it.epochDay == hoje }, "hoje tem de estar na grelha")
            vistos++
        }
        assertTrue(vistos > 80)
    }
}
