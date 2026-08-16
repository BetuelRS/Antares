package pt.antares.app.core.util

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnitConversionsTest {

    @Test
    fun `kg para lb e volta round trip`() {
        val kg = 80.5
        val back = UnitConversions.lbToKg(UnitConversions.kgToLb(kg))
        assertTrue(abs(back - kg) < 0.0001)
    }

    @Test
    fun `valores conhecidos kg lb`() {
        assertTrue(abs(UnitConversions.kgToLb(100.0) - 220.462) < 0.01)
    }

    @Test
    fun `cm para ft in conhecidos`() {
        assertEquals(Pair(5, 10), UnitConversions.cmToFtIn(178))
        assertEquals(178, UnitConversions.ftInToCm(5, 10))
    }

    @Test
    fun `ft in para cm e volta estavel`() {
        for (cm in 140..210 step 7) {
            val (ft, inch) = UnitConversions.cmToFtIn(cm)
            val back = UnitConversions.ftInToCm(ft, inch)
            assertTrue(abs(back - cm) <= 2, "cm=$cm back=$back")
        }
    }
}
