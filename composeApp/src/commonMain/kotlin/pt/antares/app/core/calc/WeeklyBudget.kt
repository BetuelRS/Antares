package pt.antares.app.core.calc

data class WeeklyBudget(

    val targetPerDay: Int,

    val daysElapsed: Int,

    val loggedDays: Int,

    val consumed: Int,
) {
    val weeklyTarget: Int get() = targetPerDay * 7

    val remaining: Int get() = weeklyTarget - consumed

    val daysAfterToday: Int get() = (7 - daysElapsed).coerceAtLeast(0)

    val perDayLeft: Int?
        get() = if (daysAfterToday > 0) remaining / daysAfterToday else null

    val complete: Boolean get() = loggedDays >= daysElapsed

    companion object {

        fun of(
            targetPerDay: Int,
            isoDayOfWeek: Int,
            loggedDays: Int,
            consumed: Int,
        ): WeeklyBudget = WeeklyBudget(
            targetPerDay = targetPerDay,
            daysElapsed = isoDayOfWeek.coerceIn(1, 7),
            loggedDays = loggedDays.coerceAtLeast(0),
            consumed = consumed.coerceAtLeast(0),
        )
    }
}
