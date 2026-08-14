package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.ceil

/**
 * O que falta e quando chega. `remainingKg` está sempre preenchido; as semanas e a data
 * ficam a null sempre que projetar seria adivinhar — sem ritmo, a andar para o lado
 * contrário, ou tão longe que a data não significa nada.
 */
data class Projection(

    val remainingKg: Double,

    val weeks: Int?,

    val etaEpochDay: Long?,

    val reached: Boolean,

    val movingAway: Boolean,
)

object GoalProjection {

    // A mesma tolerância do [NutritionCalc.hasReachedGoalWeight]: os dois têm de concordar
    // sobre o que é ter chegado, senão um ecrã festeja e o outro continua a contar.
    const val REACHED_TOLERANCE_KG = 0.3

    // Abaixo de 50 g por semana a divisão faria a data saltar meses de leitura para
    // leitura — projeção que muda todos os dias não é projeção.
    const val MIN_RATE_KG_WEEK = 0.05

    // Cinco anos. Mais longe do que isto, a data desmotiva em vez de orientar.
    const val MAX_PROJECTED_WEEKS = 260

    /**
     * O ritmo vem medido do histórico, não do que a pessoa pediu: a data tem de sair do
     * que está a acontecer e não da intenção.
     */
    fun project(
        currentKg: Double,
        goalKg: Double,
        measuredRateKgWeek: Double?,
        todayEpochDay: Long,
    ): Projection {
        val delta = currentKg - goalKg
        val remaining = abs(delta)
        if (remaining < REACHED_TOLERANCE_KG) {
            return Projection(remaining, null, null, reached = true, movingAway = false)
        }

        val rate = measuredRateKgWeek
        if (rate == null || abs(rate) < MIN_RATE_KG_WEEK) {
            return Projection(remaining, null, null, reached = false, movingAway = false)
        }

        // Os sinais têm de ser opostos: acima do objetivo (delta positivo) só se avança
        // a perder. Quem quer ganhar e está a perder recebe o aviso, não uma data.
        val movingTowards = (delta > 0 && rate < 0) || (delta < 0 && rate > 0)
        if (!movingTowards) {
            return Projection(remaining, null, null, reached = false, movingAway = true)
        }

        val weeks = ceil(remaining / abs(rate)).toInt()
        if (weeks > MAX_PROJECTED_WEEKS) {
            return Projection(remaining, null, null, reached = false, movingAway = false)
        }
        return Projection(
            remainingKg = remaining,
            weeks = weeks,
            etaEpochDay = todayEpochDay + weeks * 7L,
            reached = false,
            movingAway = false,
        )
    }
}
