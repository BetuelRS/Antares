package pt.antares.app.core.calc

import pt.antares.app.core.model.ActivityLevel

object ActivitySuggestion {

    const val LOW_ACTIVE = 5_000
    const val SOMEWHAT_ACTIVE = 7_500
    const val ACTIVE = 10_000

    const val MIN_DAYS = 7

    private fun usable(dailySteps: List<Long>): List<Long> = dailySteps.filter { it > 0 }

    fun averageDailySteps(dailySteps: List<Long>): Long? {
        val dias = usable(dailySteps)
        if (dias.size < MIN_DAYS) return null
        return dias.sum() / dias.size
    }

    fun levelForSteps(averageSteps: Long): ActivityLevel = when {
        averageSteps < LOW_ACTIVE -> ActivityLevel.SEDENTARY
        averageSteps < SOMEWHAT_ACTIVE -> ActivityLevel.LIGHT
        averageSteps < ACTIVE -> ActivityLevel.MODERATE

        else -> ActivityLevel.HIGH
    }

    fun suggest(dailySteps: List<Long>, current: ActivityLevel?): Suggestion? {
        val media = averageDailySteps(dailySteps) ?: return null
        val sugerido = levelForSteps(media)
        if (sugerido == current) return null

        if (current == ActivityLevel.ATHLETE) return null
        return Suggestion(averageSteps = media, suggested = sugerido, current = current)
    }

    data class Suggestion(
        val averageSteps: Long,
        val suggested: ActivityLevel,
        val current: ActivityLevel?,
    )
}
