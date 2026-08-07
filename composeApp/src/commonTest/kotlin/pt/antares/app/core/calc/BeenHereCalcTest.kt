package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BeenHereCalcTest {

    private val hoje = 20_650L

    @Test
    fun `nao fala de anteontem`() {

        val pesagens = listOf((hoje - 2) to 82.0)
        assertNull(BeenHereCalc.lastVisit(82.0, hoje, pesagens))
    }

    @Test
    fun `encontra a passagem antiga pelo mesmo peso`() {
        val pesagens = listOf((hoje - 200) to 82.2, (hoje - 5) to 90.0)
        val v = BeenHereCalc.lastVisit(82.0, hoje, pesagens)!!
        assertEquals(hoje - 200, v.epochDay)
        assertEquals(200L, v.daysAgo)
    }

    @Test
    fun `um peso diferente nao conta como o mesmo sitio`() {
        val pesagens = listOf((hoje - 200) to 85.0)
        assertNull(BeenHereCalc.lastVisit(82.0, hoje, pesagens))
    }

    @Test
    fun `escolhe a mais recente das antigas`() {

        val pesagens = listOf((hoje - 700) to 82.0, (hoje - 200) to 82.0)
        assertEquals(hoje - 200, BeenHereCalc.lastVisit(82.0, hoje, pesagens)!!.epochDay)
    }

    @Test
    fun `traz a cintura e a gordura daquele tempo`() {
        val pesagens = listOf((hoje - 200) to 82.0)
        val cinturas = listOf((hoje - 205) to 92.0)
        val gorduras = listOf((hoje - 198) to 24.0)
        val v = BeenHereCalc.lastVisit(82.0, hoje, pesagens, cinturas, gorduras)!!
        assertEquals(92.0, v.waistCm)
        assertEquals(24.0, v.bodyFatPct)
        assertTrue(v.hasComparison)
    }

    @Test
    fun `nao usa uma medida de meses depois para descrever aquele dia`() {

        val pesagens = listOf((hoje - 200) to 82.0)
        val cinturas = listOf(hoje to 87.0)
        val v = BeenHereCalc.lastVisit(82.0, hoje, pesagens, cinturas)!!
        assertNull(v.waistCm)
        assertTrue(!v.hasComparison)
    }
}
