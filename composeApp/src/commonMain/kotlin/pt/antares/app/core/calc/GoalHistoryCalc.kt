package pt.antares.app.core.calc

import kotlin.math.abs

object GoalHistoryCalc {

    const val REACHED_TOLERANCE_KG = NutritionCalc.GOAL_REACHED_TOLERANCE_KG

    data class Goal(
        val targetKg: Double,
        val setOnEpochDay: Long,
        val startWeightKg: Double?,
        val reachedOnEpochDay: Long? = null,
    ) {
        val reached: Boolean get() = reachedOnEpochDay != null

        val daysTaken: Long? get() = reachedOnEpochDay?.let { it - setOnEpochDay }

        val distanceKg: Double? get() = startWeightKg?.let { abs(it - targetKg) }
    }

    fun reaches(targetKg: Double, weightKg: Double): Boolean =
        abs(weightKg - targetKg) < REACHED_TOLERANCE_KG

    fun firstDayReaching(
        targetKg: Double,
        setOnEpochDay: Long,
        weighIns: List<Pair<Long, Double>>,
    ): Long? = weighIns
        .filter { (dia, _) -> dia >= setOnEpochDay }
        .filter { (_, kg) -> reaches(targetKg, kg) }
        .minOfOrNull { (dia, _) -> dia }

    fun settle(goals: List<Goal>, weighIns: List<Pair<Long, Double>>): List<Goal> =
        goals.map { goal ->
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

    fun shouldRecord(previousTargetKg: Double?, newTargetKg: Double?): Boolean {
        if (newTargetKg == null) return false
        if (previousTargetKg == null) return true

        return NutritionCalc.roundToTenth(previousTargetKg) != NutritionCalc.roundToTenth(newTargetKg)
    }
}
