package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.Sex
import kotlin.math.abs
import kotlin.math.roundToInt

enum class GoalChangeReason {

    ACTIVITY_MEANING_CHANGED,

    BMR_FORMULA_CHANGED,

    ENERGY_FLOOR_CHANGED,

    EXERCISE_ADD_BACK_FORCED_ON,
}

data class GoalChange(
    val oldKcal: Int,
    val newKcal: Int,
    val reasons: List<GoalChangeReason>,
) {
    val deltaKcal: Int get() = newKcal - oldKcal
}

object ProfileMigration {

    const val NOTICE_THRESHOLD_KCAL = 25

    fun legacyDailyKcal(
        profile: UserProfileEntity,
        weightKg: Double,
        todayEpochDay: Long,
    ): Int {
        val age = NutritionCalc.ageYears(profile.birthEpochDay, todayEpochDay)
        val bmr = NutritionCalc.bmr(profile.sex, weightKg, profile.heightCm, age)
        val tdee = bmr * profile.activityLevel.legacyMultiplier
        val floor = when (profile.sex) {
            Sex.MALE -> NutritionCalc.FLOOR_MALE
            Sex.FEMALE -> NutritionCalc.FLOOR_FEMALE
        }
        return (tdee + profile.goalRateKcal).roundToInt().coerceAtLeast(floor)
    }

    fun detectGoalChange(
        profile: UserProfileEntity,
        weightKg: Double,
        todayEpochDay: Long,
    ): GoalChange? {
        val old = legacyDailyKcal(profile, weightKg, todayEpochDay)
        val new = NutritionCalc.dailyTargets(profile, weightKg, todayEpochDay)
        val moved = abs(new.kcal - old) >= NOTICE_THRESHOLD_KCAL

        val reasons = buildList {
            if (moved) {
                val level = profile.activityLevel
                if (level.multiplier != level.legacyMultiplier) add(GoalChangeReason.ACTIVITY_MEANING_CHANGED)
                if (new.energy?.formula == BmrFormula.KATCH_MCARDLE) add(GoalChangeReason.BMR_FORMULA_CHANGED)
                if (TargetWarning.BMR_FLOOR_CLAMPED in new.warnings) add(GoalChangeReason.ENERGY_FLOOR_CHANGED)
            }

            if (!profile.exerciseAddBack) add(GoalChangeReason.EXERCISE_ADD_BACK_FORCED_ON)
        }
        if (reasons.isEmpty()) return null
        return GoalChange(oldKcal = old, newKcal = new.kcal, reasons = reasons)
    }
}
