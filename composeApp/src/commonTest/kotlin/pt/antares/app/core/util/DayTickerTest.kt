package pt.antares.app.core.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayTickerTest {

    private val utc = TimeZone.UTC

    @Test
    fun midDayGivesRemainingHours() {

        val now = Instant.parse("2026-07-15T18:00:00Z")
        assertEquals(6 * 3600_000L, DayTicker.msUntilNextMidnight(now, utc))
    }

    @Test
    fun oneSecondBeforeMidnight() {
        val now = Instant.parse("2026-07-15T23:59:59Z")
        assertEquals(1_000L, DayTicker.msUntilNextMidnight(now, utc))
    }

    @Test
    fun exactlyAtMidnightGivesFullDay() {

        val now = Instant.parse("2026-07-15T00:00:00Z")
        assertEquals(24 * 3600_000L, DayTicker.msUntilNextMidnight(now, utc))
    }

    @Test
    fun dstSpringForwardDayIsShorter() {

        val zone = TimeZone.of("Europe/Lisbon")
        val now = Instant.parse("2026-03-29T00:00:00Z")
        val ms = DayTicker.msUntilNextMidnight(now, zone)
        assertEquals(23 * 3600_000L, ms)
    }

    @Test
    fun alwaysStrictlyPositive() {

        val samples = listOf(
            "2026-01-01T00:00:00Z", "2026-06-30T23:59:59.999Z", "2026-12-31T12:34:56Z",
        )
        for (s in samples) {
            val ms = DayTicker.msUntilNextMidnight(Instant.parse(s), utc)
            assertTrue(ms > 0, "esperado > 0 para $s, veio $ms")
        }
    }

    @Test
    fun viewerOnTodayFollowsTheNewDay() {
        assertEquals(101L, followDayChange(selected = 100L, oldToday = 100L, newToday = 101L))
    }

    @Test
    fun viewerOnPastDayStaysPut() {
        assertEquals(95L, followDayChange(selected = 95L, oldToday = 100L, newToday = 101L))
    }

    @Test
    fun multiDayJumpStillFollows() {

        assertEquals(103L, followDayChange(selected = 100L, oldToday = 100L, newToday = 103L))
    }
}
