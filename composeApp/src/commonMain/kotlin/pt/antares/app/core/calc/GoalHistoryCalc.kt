package pt.antares.app.core.calc

import kotlin.math.abs

/**
 * Guarda os objetivos de peso que a pessoa foi definindo e diz quais foram atingidos.
 * Um objetivo antigo não desaparece quando se define outro: é o histórico que mostra
 * o caminho percorrido.
 */
object GoalHistoryCalc {

    // A mesma tolerância das metas, por referência e não por cópia: chegar ao objetivo
    // tem de querer dizer o mesmo em toda a app.
    const val REACHED_TOLERANCE_KG = NutritionCalc.GOAL_REACHED_TOLERANCE_KG

    data class Goal(
        val targetKg: Double,
        val setOnEpochDay: Long,
        // Nulo para objetivos definidos sem nenhuma pesagem no histórico; sem ele não há
        // distância percorrida para mostrar.
        val startWeightKg: Double?,
        val reachedOnEpochDay: Long? = null,
    ) {
        val reached: Boolean get() = reachedOnEpochDay != null

        val daysTaken: Long? get() = reachedOnEpochDay?.let { it - setOnEpochDay }

        val distanceKg: Double? get() = startWeightKg?.let { abs(it - targetKg) }
    }

    fun reaches(targetKg: Double, weightKg: Double): Boolean =
        abs(weightKg - targetKg) < REACHED_TOLERANCE_KG

    /** A primeira pesagem a bater o alvo, e nunca antes do dia em que o objetivo foi posto. */
    fun firstDayReaching(
        targetKg: Double,
        setOnEpochDay: Long,
        weighIns: List<Pair<Long, Double>>,
    ): Long? = weighIns
        // Sem este filtro, um objetivo definido hoje aparecia como atingido no ano passado.
        .filter { (dia, _) -> dia >= setOnEpochDay }
        .filter { (_, kg) -> reaches(targetKg, kg) }
        .minOfOrNull { (dia, _) -> dia }

    /** Fecha os objetivos que já foram atingidos, deixando intactos os que já tinham data. */
    fun settle(goals: List<Goal>, weighIns: List<Pair<Long, Double>>): List<Goal> =
        goals.map { goal ->
            // Uma data já registada nunca é recalculada: apagar uma pesagem antiga não pode
            // desfazer um objetivo que a pessoa cumpriu.
            if (goal.reached) {
                goal
            } else {
                goal.copy(
                    reachedOnEpochDay = firstDayReaching(
                        goal.targetKg,
                        goal.setOnEpochDay,
                        weighIns,
                    ),
                )
            }
        }

    /** Se vale a pena gravar uma entrada nova no histórico. Apagar o objetivo não conta. */
    fun shouldRecord(previousTargetKg: Double?, newTargetKg: Double?): Boolean {
        if (newTargetKg == null) return false
        if (previousTargetKg == null) return true

        // Compara-se à décima, a mesma resolução com que o peso se introduz: sem isto, o
        // roçar de um seletor gerava entradas de histórico que a pessoa não pediu.
        return NutritionCalc.roundToTenth(previousTargetKg) != NutritionCalc.roundToTenth(newTargetKg)
    }
}
