package pt.antares.app.core.calc

data class DietBreakSuggestion(

    val maintenanceKcal: Int,

    val weeks: Int,

    val assessment: AdaptiveTdee.Assessment,
) {

    val isWorthSuggesting: Boolean
        get() = assessment == AdaptiveTdee.Assessment.METABOLIC_ADAPTATION
}

object DietBreak {

    const val DEFAULT_WEEKS = 2

    fun suggest(
        currentTdee: Double,
        consecutiveStallWeeks: Int,
        loggedDays: Int,
    ): DietBreakSuggestion = DietBreakSuggestion(

        maintenanceKcal = currentTdee.toInt(),
        weeks = DEFAULT_WEEKS,
        assessment = AdaptiveTdee.assessPlateau(consecutiveStallWeeks, loggedDays),
    )
}
