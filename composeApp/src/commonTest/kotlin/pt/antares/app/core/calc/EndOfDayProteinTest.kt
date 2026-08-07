package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EndOfDayProteinTest {

    @Test
    fun `dia sem registos nunca notifica`() {
        assertNull(EndOfDayProtein.gapToNotify(consumedG = 0.0, targetG = 150, hasLogs = false))

        assertNull(EndOfDayProtein.gapToNotify(consumedG = 10.0, targetG = 150, hasLogs = false))
    }

    @Test
    fun `acima de 60% da meta nao notifica`() {

        assertNull(EndOfDayProtein.gapToNotify(consumedG = 90.0, targetG = 150, hasLogs = true))
        assertNull(EndOfDayProtein.gapToNotify(consumedG = 149.0, targetG = 150, hasLogs = true))
    }

    @Test
    fun `abaixo de 60% com registos anuncia as gramas em falta`() {

        assertEquals(110, EndOfDayProtein.gapToNotify(consumedG = 40.0, targetG = 150, hasLogs = true))

        assertEquals(150, EndOfDayProtein.gapToNotify(consumedG = 0.0, targetG = 150, hasLogs = true))

        assertEquals(90, EndOfDayProtein.gapToNotify(consumedG = 60.4, targetG = 150, hasLogs = true))
    }

    @Test
    fun `meta invalida nao notifica`() {
        assertNull(EndOfDayProtein.gapToNotify(consumedG = 0.0, targetG = 0, hasLogs = true))
        assertNull(EndOfDayProtein.gapToNotify(consumedG = 0.0, targetG = -5, hasLogs = true))
    }
}
