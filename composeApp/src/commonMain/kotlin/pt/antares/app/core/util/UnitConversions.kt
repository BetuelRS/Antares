package pt.antares.app.core.util

import pt.antares.app.core.model.UnitSystem
import kotlin.math.roundToInt

object UnitConversions {
    const val KG_PER_LB = 0.45359237
    const val CM_PER_IN = 2.54
    const val KJ_PER_KCAL = 4.184

    fun kgToLb(kg: Double): Double = kg / KG_PER_LB
    fun lbToKg(lb: Double): Double = lb * KG_PER_LB

    fun kcalToKj(kcal: Int): Int = (kcal * KJ_PER_KCAL).roundToInt()
    fun kjToKcal(kj: Int): Int = (kj / KJ_PER_KCAL).roundToInt()

    fun cmToFtIn(cm: Int): Pair<Int, Int> {
        val totalInches = (cm / CM_PER_IN).roundToInt()
        return Pair(totalInches / 12, totalInches % 12)
    }

    fun ftInToCm(feet: Int, inches: Int): Int =
        ((feet * 12 + inches) * CM_PER_IN).roundToInt()

    fun weightToDisplay(kg: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) kgToLb(kg) else kg
}
