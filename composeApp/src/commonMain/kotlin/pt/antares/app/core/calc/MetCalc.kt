package pt.antares.app.core.calc

import kotlin.math.roundToInt

object MetCalc {

    // Um MET é, por definição, o gasto em repouso.
    const val REST_MET = 1.0

    /**
     * O que um exercício gasta **a mais** do que estar sentado.
     *
     * O produto `MET × peso × horas` dá o gasto total do período, repouso incluído. Mas o
     * repouso já está dentro da meta diária, que cobre as 24 horas: somar o valor bruto ao
     * orçamento contava-o duas vezes. Numa hora de 5 MET a 80 kg eram 80 kcal a mais — 20%.
     *
     * Subtrair um MET é a correção, e é a mesma que a Health Connect assume quando fala de
     * calorias ativas.
     */
    fun kcal(met: Double, weightKg: Double, durationMin: Int): Int {
        if (met <= REST_MET || weightKg <= 0 || durationMin <= 0) return 0
        return ((met - REST_MET) * weightKg * (durationMin / 60.0)).roundToInt()
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
