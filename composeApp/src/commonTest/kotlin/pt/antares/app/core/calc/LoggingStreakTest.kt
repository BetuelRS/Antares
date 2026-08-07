package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class LoggingStreakTest {

    @Test
    fun emptyIsZero() {
        assertEquals(0, LoggingStreak.current(emptySet(), today = 100))
    }

    @Test
    fun loggedTodayOnlyIsOne() {
        assertEquals(1, LoggingStreak.current(setOf(100L), today = 100))
    }

    @Test
    fun threeConsecutiveEndingToday() {
        assertEquals(3, LoggingStreak.current(setOf(98L, 99L, 100L), today = 100))
    }

    @Test
    fun toleranceYesterdayCountsWhenTodayEmpty() {

        assertEquals(2, LoggingStreak.current(setOf(98L, 99L), today = 100))
    }

    @Test
    fun brokenWhenBothTodayAndYesterdayEmpty() {

        assertEquals(0, LoggingStreak.current(setOf(96L, 97L, 98L), today = 100))
    }

    @Test
    fun gapBreaksTheCount() {

        assertEquals(2, LoggingStreak.current(setOf(96L, 99L, 100L), today = 100))
    }

    @Test
    fun futureDaysDoNotInflate() {

        assertEquals(1, LoggingStreak.current(setOf(100L, 101L, 102L), today = 100))
    }

    @Test
    fun orderIndependent() {
        assertEquals(3, LoggingStreak.current(setOf(100L, 98L, 99L), today = 100))
    }

    @Test
    fun longestEmptyIsZero() {
        assertEquals(0, LoggingStreak.longest(emptySet()))
    }

    @Test
    fun longestSingleIsOne() {
        assertEquals(1, LoggingStreak.longest(setOf(42L)))
    }

    @Test
    fun longestPicksBiggestRun() {

        assertEquals(3, LoggingStreak.longest(setOf(1L, 2L, 3L, 10L, 11L, 20L)))
    }

    @Test
    fun longestAllConsecutive() {
        assertEquals(5, LoggingStreak.longest(setOf(1L, 2L, 3L, 4L, 5L)))
    }

    private val today = 9999L

    @Test
    fun freezeForgivesASingleGap() {

        val r = LoggingStreak.currentWithFreeze(setOf(9999L, 9998L, 9996L), today)
        assertEquals(3, r.current)
        assertEquals(9997L, r.freezeUsedAtDay)
    }

    @Test
    fun twoGapsInSameWeekBreak() {

        val r = LoggingStreak.currentWithFreeze(setOf(9999L, 9998L, 9996L, 9994L), today)
        assertEquals(3, r.current)
        assertEquals(9997L, r.freezeUsedAtDay)
    }

    @Test
    fun gapsInDifferentWeeksEachGetAFreeze() {

        val logged = setOf(
            9999L, 9998L, 9997L,  9995L, 9994L, 9993L, 9992L, 9991L, 9990L,
             9988L, 9987L,
        )
        val r = LoggingStreak.currentWithFreeze(logged, today)
        assertEquals(11, r.current)
        assertEquals(9996L, r.freezeUsedAtDay)
    }

    @Test
    fun twoConsecutiveMissingDaysBreak() {

        val r = LoggingStreak.currentWithFreeze(setOf(9999L, 9998L, 9995L), today)
        assertEquals(2, r.current)
        assertEquals(null, r.freezeUsedAtDay)
    }

    @Test
    fun bothTodayAndYesterdayMissingIsZero() {

        val r = LoggingStreak.currentWithFreeze(setOf(9996L, 9995L, 9994L), today)
        assertEquals(0, r.current)
        assertEquals(null, r.freezeUsedAtDay)
    }

    @Test
    fun freezeMatchesPlainStreakWhenNoGaps() {

        val logged = setOf(9999L, 9998L, 9997L)
        assertEquals(
            LoggingStreak.current(logged, today),
            LoggingStreak.currentWithFreeze(logged, today).current,
        )
    }
}
