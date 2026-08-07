package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeeklyBudgetTest {

    @Test
    fun `comer mais ao domingo e compensar a segunda nao e falhar`() {

        val terca = WeeklyBudget.of(
            targetPerDay = 2000,
            isoDayOfWeek = 2,
            loggedDays = 2,
            consumed = 3000 + 1000,
        )
        assertEquals(14_000, terca.weeklyTarget)
        assertEquals(10_000, terca.remaining)
        assertTrue(terca.complete)
    }

    @Test
    fun `os dias sem registo nao viram folga`() {

        val quarta = WeeklyBudget.of(
            targetPerDay = 2000,
            isoDayOfWeek = 3,
            loggedDays = 1,
            consumed = 1500,
        )
        assertFalse(quarta.complete)
        assertEquals(2, quarta.daysElapsed - quarta.loggedDays)
    }

    @Test
    fun `ao domingo nao ha por-dia porque nao ha dias a seguir`() {
        val domingo = WeeklyBudget.of(
            targetPerDay = 2000,
            isoDayOfWeek = 7,
            loggedDays = 7,
            consumed = 13_000,
        )
        assertEquals(0, domingo.daysAfterToday)

        assertNull(domingo.perDayLeft)
        assertEquals(1000, domingo.remaining)
    }

    @Test
    fun `o que sobra reparte-se pelos dias que faltam`() {

        val segunda = WeeklyBudget.of(
            targetPerDay = 2000,
            isoDayOfWeek = 1,
            loggedDays = 1,
            consumed = 2500,
        )
        assertEquals(6, segunda.daysAfterToday)
        assertEquals(11_500 / 6, segunda.perDayLeft)
    }

    @Test
    fun `passar do orcamento da um resto negativo, nao um zero`() {
        val sabado = WeeklyBudget.of(
            targetPerDay = 2000,
            isoDayOfWeek = 6,
            loggedDays = 6,
            consumed = 15_000,
        )
        assertEquals(-1000, sabado.remaining)
    }

    @Test
    fun `um dia da semana fora de escala nao rebenta a conta`() {

        assertEquals(7, WeeklyBudget.of(2000, isoDayOfWeek = 9, loggedDays = 0, consumed = 0).daysElapsed)
        assertEquals(1, WeeklyBudget.of(2000, isoDayOfWeek = 0, loggedDays = 0, consumed = 0).daysElapsed)
    }
}
