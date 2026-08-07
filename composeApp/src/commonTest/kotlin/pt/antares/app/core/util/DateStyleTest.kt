package pt.antares.app.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateStyleTest {

    private val hoje = kotlinx.datetime.LocalDate(2026, 7, 29).toEpochDays().toLong()

    private fun dia(ano: Int, mes: Int, dia: Int) =
        kotlinx.datetime.LocalDate(ano, mes, dia).toEpochDays().toLong()

    @Test
    fun `este ano nao leva ano`() {
        assertFalse(DateStyle.needsYear(dia(2026, 1, 1), hoje))
        assertFalse(DateStyle.needsYear(dia(2026, 12, 31), hoje))
        assertFalse(DateStyle.needsYear(hoje, hoje))
    }

    @Test
    fun `duas pesagens no mesmo dia de anos diferentes nao podem ler-se igual`() {
        val esteAno = dia(2026, 7, 15)
        val anoPassado = dia(2025, 7, 15)
        assertFalse(DateStyle.needsYear(esteAno, hoje))
        assertTrue(DateStyle.needsYear(anoPassado, hoje), "15 jul de 2025 ficaria igual ao de 2026")
    }

    @Test
    fun `o futuro de outro ano tambem leva ano`() {

        assertTrue(DateStyle.needsYear(dia(2027, 1, 2), hoje))
    }

    @Test
    fun `a viragem do ano e no dia 1 de janeiro, nao 365 dias antes`() {

        val ultimoDeDezembro = dia(2025, 12, 31)
        val primeiroDeJaneiro = dia(2026, 1, 1)
        assertTrue(DateStyle.needsYear(ultimoDeDezembro, primeiroDeJaneiro))
        assertFalse(DateStyle.needsYear(primeiroDeJaneiro, primeiroDeJaneiro))
    }

    @Test
    fun `um eixo de semanas mostra dias`() {
        assertEquals(DateStyle.AxisStyle.DAY_MONTH, DateStyle.axisStyle(14))
        assertEquals(DateStyle.AxisStyle.DAY_MONTH, DateStyle.axisStyle(90))
        assertEquals(DateStyle.AxisStyle.DAY_MONTH, DateStyle.axisStyle(365))
    }

    @Test
    fun `um eixo de anos mostra mes e ano`() {
        assertEquals(DateStyle.AxisStyle.MONTH_YEAR, DateStyle.axisStyle(DateStyle.LONG_SPAN_DAYS))
        assertEquals(DateStyle.AxisStyle.MONTH_YEAR, DateStyle.axisStyle(2000))
    }

    @Test
    fun `um eixo sem amplitude nao rebenta`() {
        assertEquals(DateStyle.AxisStyle.DAY_MONTH, DateStyle.axisStyle(0))
    }
}
