package pt.antares.app.core.calc

import kotlin.math.roundToInt

object MetCalc {

    /**
     * Calorias de um exercício pela tabela de METs. Um MET é o gasto em repouso, por isso
     * o produto dá o gasto total do período — inclui o que se gastaria sentado.
     */
    fun kcal(met: Double, weightKg: Double, durationMin: Int): Int {
        if (met <= 0 || weightKg <= 0 || durationMin <= 0) return 0
        return (met * weightKg * (durationMin / 60.0)).roundToInt()
    }
}

/**
 * O dia em calorias. O exercício soma-se ao orçamento em vez de se subtrair ao consumo:
 * a app trata o gasto como margem ganha, não como comida desfeita.
 */
data class DailyBudget(
    val target: Int,
    val consumed: Int,
    val exercise: Int,
) {

    val budget: Int get() = target + exercise

    // Pode ficar negativo de propósito: mostrar quanto se passou é a informação, e travar
    // em zero escondia-a.
    val remaining: Int get() = budget - consumed
}

object DailyBudgetCalc {
    fun compute(target: Int, consumed: Int, exercise: Int): DailyBudget =
        DailyBudget(target = target, consumed = consumed, exercise = exercise)
}
