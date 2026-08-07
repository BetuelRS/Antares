package pt.antares.app.core.calc

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class FastingStatsTest {

    private val utc = TimeZone.UTC
    private val hour = 3_600_000L

    private fun at(zone: TimeZone, y: Int, mo: Int, d: Int, h: Int = 12): Long =
        LocalDateTime(y, mo, d, h, 0).toInstant(zone).toEpochMilliseconds()

    private fun fast(endMs: Long, completed: Boolean, durH: Int = 16): FinishedFast =
        FinishedFast(startedAt = endMs - durH * hour, endedAt = endMs, completed = completed)

    @Test
    fun `lista vazia devolve tudo a zero`() {
        val r = FastingStats.compute(emptyList(), now = at(utc, 2024, 6, 1), zone = utc)
        assertEquals(0, r.currentStreak)
        assertEquals(0, r.longestStreak)
        assertEquals(0f, r.completionRate)
        assertEquals(0L, r.averageDurationMs)
    }

    @Test
    fun `dias consecutivos concluidos ate hoje contam o streak`() {
        val today = at(utc, 2024, 6, 10)
        val sessions = listOf(
            fast(at(utc, 2024, 6, 8), completed = true),
            fast(at(utc, 2024, 6, 9), completed = true),
            fast(at(utc, 2024, 6, 10), completed = true),
        )
        val r = FastingStats.compute(sessions, now = today, zone = utc)
        assertEquals(3, r.currentStreak)
        assertEquals(3, r.longestStreak)
    }

    @Test
    fun `hoje sem jejum mas ontem concluido mantem o streak vivo`() {
        val today = at(utc, 2024, 6, 10)
        val sessions = listOf(
            fast(at(utc, 2024, 6, 8), completed = true),
            fast(at(utc, 2024, 6, 9), completed = true),
        )
        val r = FastingStats.compute(sessions, now = today, zone = utc)
        assertEquals(2, r.currentStreak)
    }

    @Test
    fun `ultima conclusao anterior a ontem quebra o streak atual`() {
        val today = at(utc, 2024, 6, 10)
        val sessions = listOf(
            fast(at(utc, 2024, 6, 6), completed = true),
            fast(at(utc, 2024, 6, 7), completed = true),
        )
        val r = FastingStats.compute(sessions, now = today, zone = utc)
        assertEquals(0, r.currentStreak)
        assertEquals(2, r.longestStreak)
    }

    @Test
    fun `buraco no meio parte a corrida e o recorde apanha a maior`() {
        val today = at(utc, 2024, 6, 10)
        val sessions = listOf(

            fast(at(utc, 2024, 6, 1), completed = true),
            fast(at(utc, 2024, 6, 2), completed = true),
            fast(at(utc, 2024, 6, 3), completed = true),

            fast(at(utc, 2024, 6, 9), completed = true),
            fast(at(utc, 2024, 6, 10), completed = true),
        )
        val r = FastingStats.compute(sessions, now = today, zone = utc)
        assertEquals(2, r.currentStreak)
        assertEquals(3, r.longestStreak)
    }

    @Test
    fun `sessoes BROKEN nao contam para streak mas entram na taxa`() {
        val today = at(utc, 2024, 6, 10)
        val sessions = listOf(
            fast(at(utc, 2024, 6, 9), completed = false),
            fast(at(utc, 2024, 6, 10), completed = true),
        )
        val r = FastingStats.compute(sessions, now = today, zone = utc)
        assertEquals(1, r.currentStreak)
        assertEquals(1, r.completedCount)
        assertEquals(1, r.brokenCount)
        assertEquals(0.5f, r.completionRate, 0.0001f)
    }

    @Test
    fun `streak resiste a mudanca de DST entre dias consecutivos`() {

        val lisbon = TimeZone.of("Europe/Lisbon")
        val today = at(lisbon, 2024, 3, 31, h = 20)
        val sessions = listOf(
            fast(at(lisbon, 2024, 3, 30, h = 10), completed = true),
            fast(at(lisbon, 2024, 3, 31, h = 10), completed = true),
        )
        val r = FastingStats.compute(sessions, now = today, zone = lisbon)
        assertEquals(2, r.currentStreak)
        assertEquals(2, r.longestStreak)
    }

    @Test
    fun `duas conclusoes no mesmo dia contam como um so dia de streak`() {
        val today = at(utc, 2024, 6, 10)
        val sessions = listOf(
            fast(at(utc, 2024, 6, 10, h = 8), completed = true),
            fast(at(utc, 2024, 6, 10, h = 20), completed = true),
        )
        val r = FastingStats.compute(sessions, now = today, zone = utc)
        assertEquals(1, r.currentStreak)
        assertEquals(1, r.longestStreak)
    }

    @Test
    fun `taxa de conclusao e duracao media agregam todas as terminadas`() {
        val today = at(utc, 2024, 6, 10)
        val sessions = listOf(
            fast(at(utc, 2024, 6, 8), completed = true, durH = 16),
            fast(at(utc, 2024, 6, 9), completed = false, durH = 12),
            fast(at(utc, 2024, 6, 10), completed = true, durH = 20),
        )
        val r = FastingStats.compute(sessions, now = today, zone = utc)
        assertEquals(2, r.completedCount)
        assertEquals(1, r.brokenCount)
        assertEquals(2f / 3f, r.completionRate, 0.0001f)
        assertEquals((16 + 12 + 20) * hour / 3, r.averageDurationMs)
    }
}
