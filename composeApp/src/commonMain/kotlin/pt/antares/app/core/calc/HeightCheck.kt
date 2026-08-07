package pt.antares.app.core.calc

object HeightCheck {

    const val MINOR_INTERVAL_DAYS = 182

    const val ADULT_INTERVAL_DAYS = 730

    fun isDue(
        ageYears: Int,
        confirmedEpochDay: Long?,
        profileUpdatedEpochDay: Long,
        todayEpochDay: Long,
    ): Boolean {
        val last = confirmedEpochDay ?: profileUpdatedEpochDay
        val interval = if (ageYears < NutritionCalc.ADULT_AGE) {
            MINOR_INTERVAL_DAYS
        } else {
            ADULT_INTERVAL_DAYS
        }
        return todayEpochDay - last >= interval
    }
}
