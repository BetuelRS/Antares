package pt.antares.app.core.calc

import kotlin.math.roundToInt

object MetCalc {

    fun kcal(met: Double, weightKg: Double, durationMin: Int): Int {
        if (met <= 0 || weightKg <= 0 || durationMin <= 0) return 0
        return (met * weightKg * (durationMin / 60.0)).roundToInt()
    }
}

data class DailyBudget(
    val target: Int,
    val consumed: Int,
    val exercise: Int,
) {

    val budget: Int get() = target + exercise

    val remaining: Int get() = budget - consumed
}

object DailyBudgetCalc {
    fun compute(target: Int, consumed: Int, exercise: Int): DailyBudget =
        DailyBudget(target = target, consumed = consumed, exercise = exercise)
}
