package pt.antares.app.core.coach

import pt.antares.app.core.util.weekStartEpochDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoachTriggerTest {

    private val monday = 20_000L.let { weekStartEpochDay(it) }

    @Test
    fun `semana ISO comeca sempre a segunda`() {

        assertEquals(19_996L, weekStartEpochDay(20_000L))

        assertEquals(19_996L, weekStartEpochDay(19_996L))

        assertEquals(19_996L, weekStartEpochDay(20_002L))
    }

    @Test
    fun `analisa a semana ANTERIOR, nunca a corrente`() {

        assertEquals(monday - 7, CoachTrigger.targetWeekStart(monday + 3))
        assertEquals(monday - 7, CoachTrigger.targetWeekStart(monday))
    }

    @Test
    fun `primeira abertura da semana com dados suficientes gera relatorio`() {
        assertTrue(
            CoachTrigger.shouldGenerate(
                todayEpochDay = monday,
                lastReportWeekStart = null,
                loggedDaysLastWeek = 5,
            ),
        )
    }

    @Test
    fun `semana esparsa nao gasta quota nenhuma`() {
        assertFalse(
            CoachTrigger.shouldGenerate(
                todayEpochDay = monday,
                lastReportWeekStart = null,
                loggedDaysLastWeek = 3,
            ),
        )

        assertTrue(
            CoachTrigger.shouldGenerate(
                todayEpochDay = monday,
                lastReportWeekStart = null,
                loggedDaysLastWeek = 4,
            ),
        )
    }

    @Test
    fun `nao regera o relatorio que ja existe`() {

        assertFalse(
            CoachTrigger.shouldGenerate(
                todayEpochDay = monday,
                lastReportWeekStart = monday - 7,
                loggedDaysLastWeek = 7,
            ),
        )
    }

    @Test
    fun `semana nova volta a gerar`() {

        assertTrue(
            CoachTrigger.shouldGenerate(
                todayEpochDay = monday,
                lastReportWeekStart = monday - 14,
                loggedDaysLastWeek = 6,
            ),
        )
    }

    @Test
    fun `manual usa a semana atual quando a anterior esta vazia`() {

        assertEquals(
            monday,
            CoachTrigger.manualWeekStart(monday + 2, loggedDaysPreviousWeek = 0, loggedDaysCurrentWeek = 5),
        )
    }

    @Test
    fun `manual mantem a semana anterior quando ela tem dados`() {

        assertEquals(
            monday - 7,
            CoachTrigger.manualWeekStart(monday + 2, loggedDaysPreviousWeek = 6, loggedDaysCurrentWeek = 5),
        )
    }

    @Test
    fun `manual mantem a anterior se nem uma nem outra tem dados suficientes`() {

        assertEquals(
            monday - 7,
            CoachTrigger.manualWeekStart(monday + 2, loggedDaysPreviousWeek = 1, loggedDaysCurrentWeek = 2),
        )
    }

    @Test
    fun `relatorio do futuro nao faz regenerar o passado`() {

        assertFalse(
            CoachTrigger.shouldGenerate(
                todayEpochDay = monday,
                lastReportWeekStart = monday,
                loggedDaysLastWeek = 7,
            ),
        )
    }
}
