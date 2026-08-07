package pt.antares.app.core.calc

import pt.antares.app.core.util.weekStartEpochDay

data class StreakResult(
    val current: Int,

    val freezeUsedAtDay: Long?,
)

object LoggingStreak {

    fun current(loggedDays: Set<Long>, today: Long): Int {
        if (loggedDays.isEmpty()) return 0

        val anchor = when {
            today in loggedDays -> today
            (today - 1) in loggedDays -> today - 1
            else -> return 0
        }

        var day = anchor
        var count = 0
        while (day in loggedDays) {
            count++
            day--
        }
        return count
    }

    fun currentWithFreeze(loggedDays: Set<Long>, today: Long): StreakResult {
        if (loggedDays.isEmpty()) return StreakResult(0, null)
        val anchor = when {
            today in loggedDays -> today
            (today - 1) in loggedDays -> today - 1
            else -> return StreakResult(0, null)
        }

        var day = anchor
        var count = 0
        var mostRecentFreeze: Long? = null
        val frozenWeeks = HashSet<Long>()
        while (true) {
            if (day in loggedDays) {
                count++
                day--
            } else {
                val isolatedGap = (day - 1) in loggedDays
                val week = weekStartEpochDay(day)
                if (isolatedGap && week !in frozenWeeks) {
                    frozenWeeks.add(week)
                    if (mostRecentFreeze == null) mostRecentFreeze = day
                    day--
                } else {
                    break
                }
            }
        }
        return StreakResult(count, mostRecentFreeze)
    }

    fun longest(loggedDays: Set<Long>): Int {
        if (loggedDays.isEmpty()) return 0
        val sorted = loggedDays.toSortedSet()
        var best = 1
        var run = 1
        var previous: Long? = null
        for (day in sorted) {
            if (previous != null && day == previous + 1) {
                run++
            } else if (previous != null) {
                run = 1
            }
            if (run > best) best = run
            previous = day
        }
        return best
    }
}
