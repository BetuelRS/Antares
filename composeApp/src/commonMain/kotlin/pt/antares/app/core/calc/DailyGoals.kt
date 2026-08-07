package pt.antares.app.core.calc

import kotlin.math.roundToInt

object DailyGoals {

    const val WATER_ML_PER_KG = 35
    const val WATER_ROUNDING_ML = 50

    const val FIBRE_G_ADULT = 25

    fun waterMl(weightKg: Double): Int {
        if (weightKg <= 0) return 0
        val raw = weightKg * WATER_ML_PER_KG
        return (raw / WATER_ROUNDING_ML).roundToInt() * WATER_ROUNDING_ML
    }

    fun fibreG(): Int = FIBRE_G_ADULT
}
