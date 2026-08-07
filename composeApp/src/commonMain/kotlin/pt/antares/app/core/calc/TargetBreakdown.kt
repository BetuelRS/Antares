package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.UserProfileEntity
import kotlin.math.roundToInt

data class TargetBreakdown(
    val steps: List<Step>,
    val finalKcal: Int,
) {

    data class Step(
        val kind: Kind,
        val values: List<Double>,
        val exact: Double,
        val result: Int = exact.roundToInt(),
    )

    enum class Kind {

        BMR_FROM_LEAN,

        BMR_MIFFLIN,

        ACTIVITY,

        RATE,

        FLOOR,
    }
}

object TargetBreakdownCalc {

    fun of(
        profile: UserProfileEntity,
        targets: Targets,
        weightKg: Double,
        todayEpochDay: Long,
    ): TargetBreakdown? {
        val e = targets.energy ?: return null
        val steps = mutableListOf<TargetBreakdown.Step>()

        val lean = e.leanMassKg
        if (lean != null) {
            steps += TargetBreakdown.Step(
                kind = TargetBreakdown.Kind.BMR_FROM_LEAN,
                values = listOf(lean),
                exact = e.bmr,
            )
        } else {
            val age = NutritionCalc.ageYears(profile.birthEpochDay, todayEpochDay)

            steps += TargetBreakdown.Step(
                kind = TargetBreakdown.Kind.BMR_MIFFLIN,
                values = listOf(
                    weightKg,
                    profile.heightCm.toDouble(),
                    age.toDouble(),
                    NutritionCalc.mifflinSexTerm(profile.sex),
                ),
                exact = e.bmr,
            )
        }

        steps += TargetBreakdown.Step(
            kind = TargetBreakdown.Kind.ACTIVITY,
            values = listOf(e.bmr, profile.activityLevel.multiplier),
            exact = e.tdee,
        )

        val tdeeShown = e.tdee.roundToInt()
        val afterRate = tdeeShown + profile.goalRateKcal
        steps += TargetBreakdown.Step(
            kind = TargetBreakdown.Kind.RATE,
            values = listOf(tdeeShown.toDouble(), profile.goalRateKcal.toDouble()),
            exact = afterRate.toDouble(),
        )

        if (afterRate != targets.kcal) {
            steps += TargetBreakdown.Step(
                kind = TargetBreakdown.Kind.FLOOR,
                values = listOf(afterRate.toDouble()),
                exact = targets.kcal.toDouble(),
            )
        }

        return TargetBreakdown(steps = steps, finalKcal = targets.kcal)
    }
}
