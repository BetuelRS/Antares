package pt.antares.app.core.coach

import pt.antares.app.core.util.weekStartEpochDay

object CoachTrigger {

    const val MIN_LOGGED_DAYS = 4

    fun targetWeekStart(todayEpochDay: Long): Long = weekStartEpochDay(todayEpochDay) - 7

    fun manualWeekStart(
        todayEpochDay: Long,
        loggedDaysPreviousWeek: Int,
        loggedDaysCurrentWeek: Int,
    ): Long {
        val current = weekStartEpochDay(todayEpochDay)
        val previous = current - 7
        return if (loggedDaysPreviousWeek < MIN_LOGGED_DAYS && loggedDaysCurrentWeek >= MIN_LOGGED_DAYS) {
            current
        } else {
            previous
        }
    }

    fun shouldGenerate(
        todayEpochDay: Long,
        lastReportWeekStart: Long?,
        loggedDaysLastWeek: Int,
    ): Boolean {
        if (loggedDaysLastWeek < MIN_LOGGED_DAYS) return false
        val target = targetWeekStart(todayEpochDay)

        return lastReportWeekStart == null || lastReportWeekStart < target
    }
}
