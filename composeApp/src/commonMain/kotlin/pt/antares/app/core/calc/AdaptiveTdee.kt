package pt.antares.app.core.calc

import pt.antares.app.core.model.Sex
import kotlin.math.abs
import kotlin.math.roundToInt

object AdaptiveTdee {

    const val KCAL_PER_KG = 7700.0

    const val SMOOTHING = 0.3

    const val MAX_WEEKLY_CHANGE_KCAL = 200

    const val MIN_LOGGED_DAYS = 5

    const val MIN_WEIGH_INS = 2

    const val MAX_PLAUSIBLE_WEEKLY_KG = 1.5

    enum class Veto {
        FEW_LOGGED_DAYS,
        FEW_WEIGH_INS,
        IMPLAUSIBLE_WEIGHT_CHANGE,

        LIKELY_METABOLIC_ADAPTATION,
    }

    const val PLATEAU_WEEKS = 3

    const val FAITHFUL_LOGGING_DAYS = 6

    enum class Assessment {

        METABOLIC_ADAPTATION,

        LIKELY_UNDER_LOGGING,

        UNCLEAR,
    }

    fun assessPlateau(consecutiveStallWeeks: Int, loggedDays: Int): Assessment = when {
        consecutiveStallWeeks < PLATEAU_WEEKS -> Assessment.UNCLEAR
        loggedDays >= FAITHFUL_LOGGING_DAYS -> Assessment.METABOLIC_ADAPTATION
        else -> Assessment.LIKELY_UNDER_LOGGING
    }

    data class WeekInput(

        val avgIntakeKcal: Double,

        val loggedDays: Int,

        val weightTrendDeltaKg: Double,
        val weighIns: Int,

        val currentTdee: Double,

        val goalRateKcal: Int,
        val sex: Sex,

        val bmr: Double? = null,

        val consecutiveStallWeeks: Int = 0,
    )

    data class Proposal(
        val newTdee: Int,
        val newTargetKcal: Int,
        val previousTargetKcal: Int,

        val observedTdee: Int,
        val clamped: Boolean,
        val flooredToSafety: Boolean,
    ) {
        val deltaKcal: Int get() = newTargetKcal - previousTargetKcal

        val isMeaningful: Boolean get() = deltaKcal != 0
    }

    sealed interface Result {
        data class Propose(val proposal: Proposal) : Result
        data class Skip(val reason: Veto) : Result
    }

    fun evaluate(input: WeekInput): Result {

        if (input.loggedDays < MIN_LOGGED_DAYS) return Result.Skip(Veto.FEW_LOGGED_DAYS)
        if (input.weighIns < MIN_WEIGH_INS) return Result.Skip(Veto.FEW_WEIGH_INS)
        if (abs(input.weightTrendDeltaKg) > MAX_PLAUSIBLE_WEEKLY_KG) {
            return Result.Skip(Veto.IMPLAUSIBLE_WEIGHT_CHANGE)
        }

        if (input.goalRateKcal < 0 && input.consecutiveStallWeeks >= PLATEAU_WEEKS) {
            return Result.Skip(Veto.LIKELY_METABOLIC_ADAPTATION)
        }

        val observed = input.avgIntakeKcal - (input.weightTrendDeltaKg * KCAL_PER_KG) / 7.0

        val smoothed = (1 - SMOOTHING) * input.currentTdee + SMOOTHING * observed

        val previousTarget = (input.currentTdee + input.goalRateKcal).roundToInt()
        var target = (smoothed + input.goalRateKcal).roundToInt()

        var clamped = false
        val delta = target - previousTarget
        if (abs(delta) > MAX_WEEKLY_CHANGE_KCAL) {
            target = previousTarget + MAX_WEEKLY_CHANGE_KCAL * (if (delta > 0) 1 else -1)
            clamped = true
        }

        val absoluteFloor = when (input.sex) {
            Sex.MALE -> NutritionCalc.FLOOR_MALE
            Sex.FEMALE -> NutritionCalc.FLOOR_FEMALE
        }
        val bmrFloor = input.bmr?.let { (it * NutritionCalc.BMR_FLOOR_FRACTION).roundToInt() } ?: 0
        val floor = maxOf(absoluteFloor, bmrFloor)
        var floored = false
        if (target < floor) {
            target = floor
            floored = true
        }

        return Result.Propose(
            Proposal(
                newTdee = (target - input.goalRateKcal),
                newTargetKcal = target,
                previousTargetKcal = previousTarget,
                observedTdee = observed.roundToInt(),
                clamped = clamped,
                flooredToSafety = floored,
            ),
        )
    }
}
