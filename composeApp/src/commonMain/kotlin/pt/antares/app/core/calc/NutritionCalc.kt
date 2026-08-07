package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class TargetWarning {

    FLOOR_CLAMPED,

    BMR_FLOOR_CLAMPED,

    CARBS_CLAMPED_TO_ZERO,

    RATE_ABOVE_SAFE_ZONE,

    PROTEIN_BELOW_FLOOR,

    GOAL_WEIGHT_REACHED,

    NO_DEFICIT_IN_PREGNANCY,
}

enum class BmrFormula {

    MIFFLIN_ST_JEOR,

    KATCH_MCARDLE,

    CUNNINGHAM,
}

data class EnergyEstimate(
    val bmr: Double,
    val tdee: Double,
    val formula: BmrFormula,

    val leanMassKg: Double?,
    val bodyFatPct: Double?,
    val bodyFatSource: BodyFatSource?,
)

data class Targets(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val warnings: List<TargetWarning> = emptyList(),

    val energy: EnergyEstimate? = null,
)

object NutritionCalc {

    const val FLOOR_MALE = 1500
    const val FLOOR_FEMALE = 1200

    const val BMR_FLOOR_FRACTION = 0.80

    const val KCAL_PER_G_PROTEIN = 4
    const val KCAL_PER_G_CARB = 4
    const val KCAL_PER_G_FAT = 9

    private const val KATCH_BASE = 370.0
    private const val KATCH_PER_KG_LEAN = 21.6

    private const val CUNNINGHAM_BASE = 500.0
    private const val CUNNINGHAM_PER_KG_LEAN = 22.0

    const val SAFE_LOSS_MIN_PCT = 0.003
    const val SAFE_LOSS_MAX_PCT = 0.010
    const val SAFE_GAIN_MIN_PCT = 0.0015
    const val SAFE_GAIN_MAX_PCT = 0.005

    const val ADULT_AGE = 18
    const val MINOR_LOSS_MAX_FACTOR = 0.5

    const val PROTEIN_FLOOR_PER_KG_LEAN_DEFICIT = 1.8
    const val PROTEIN_FLOOR_PER_KG_LEAN = 1.2
    const val PROTEIN_FLOOR_PER_KG_DEFICIT = 1.4

    const val PROTEIN_RDA_PER_KG = 0.83

    const val PROTEIN_OLDER_ADULT_AGE = 65
    const val PROTEIN_RDA_PER_KG_OLDER = 1.0

    fun mifflinSexTerm(sex: Sex): Double = when (sex) {
        Sex.MALE -> 5.0
        Sex.FEMALE -> -161.0
    }

    fun bmr(sex: Sex, weightKg: Double, heightCm: Int, ageYears: Int): Double {

        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return base + mifflinSexTerm(sex)
    }

    fun bmrKatchMcArdle(leanMassKg: Double): Double =
        KATCH_BASE + KATCH_PER_KG_LEAN * leanMassKg

    fun bmrCunningham(leanMassKg: Double): Double =
        CUNNINGHAM_BASE + CUNNINGHAM_PER_KG_LEAN * leanMassKg

    fun tdee(bmr: Double, activityMultiplier: Double): Double = bmr * activityMultiplier

    fun usableLeanMassKg(profile: UserProfileEntity, weightKg: Double): Double? {
        val pct = profile.bodyFatPct ?: return null

        val source = profile.bodyFatSource ?: BodyFatSource.MEASURED
        if (source == BodyFatSource.BMI) return null
        return BodyComposition.leanMassKg(weightKg, pct)
    }

    fun roundToTenth(value: Double): Double = (value * 10).roundToLong() / 10.0

    fun energy(
        profile: UserProfileEntity,
        weightKg: Double,
        todayEpochDay: Long,
    ): EnergyEstimate {
        val lean = usableLeanMassKg(profile, weightKg)

        val chosen = when {
            lean == null -> BmrFormula.MIFFLIN_ST_JEOR
            profile.bmrFormulaOverride == BmrFormula.CUNNINGHAM -> BmrFormula.CUNNINGHAM
            else -> BmrFormula.KATCH_MCARDLE
        }
        val bmrRaw = when (chosen) {
            BmrFormula.CUNNINGHAM -> bmrCunningham(lean!!)
            BmrFormula.KATCH_MCARDLE -> bmrKatchMcArdle(lean!!)
            BmrFormula.MIFFLIN_ST_JEOR ->
                bmr(profile.sex, weightKg, profile.heightCm, ageYears(profile.birthEpochDay, todayEpochDay))
        }

        val bmrValue = roundToTenth(bmrRaw)
        return EnergyEstimate(
            bmr = bmrValue,
            tdee = tdee(bmrValue, profile.activityLevel.multiplier),
            formula = chosen,
            leanMassKg = lean,
            bodyFatPct = if (lean != null) profile.bodyFatPct else null,
            bodyFatSource = if (lean != null) {
                profile.bodyFatSource ?: BodyFatSource.MEASURED
            } else {
                null
            },
        )
    }

    fun ageYears(birthEpochDay: Long, todayEpochDay: Long): Int {
        val days = todayEpochDay - birthEpochDay

        return (days / 365.2425).toInt()
    }

    fun kcalPerDayFromWeeklyKg(kgPerWeek: Double): Int =
        (kgPerWeek * AdaptiveTdee.KCAL_PER_KG / 7.0).roundToInt()

    fun weeklyKgFromKcalPerDay(kcalPerDay: Int): Double =
        kcalPerDay * 7.0 / AdaptiveTdee.KCAL_PER_KG

    fun safeWeeklyLossKg(
        weightKg: Double,
        ageYears: Int? = null,
    ): ClosedFloatingPointRange<Double> {
        val maxPct = if (ageYears != null && ageYears < ADULT_AGE) {
            SAFE_LOSS_MAX_PCT * MINOR_LOSS_MAX_FACTOR
        } else {
            SAFE_LOSS_MAX_PCT
        }
        return (weightKg * SAFE_LOSS_MIN_PCT)..(weightKg * maxPct)
    }

    fun safeWeeklyGainKg(weightKg: Double): ClosedFloatingPointRange<Double> =
        (weightKg * SAFE_GAIN_MIN_PCT)..(weightKg * SAFE_GAIN_MAX_PCT)

    const val GOAL_REACHED_TOLERANCE_KG = 0.3

    fun hasReachedGoalWeight(goalWeightKg: Double?, weightKg: Double): Boolean {
        val goal = goalWeightKg ?: return false
        return abs(weightKg - goal) < GOAL_REACHED_TOLERANCE_KG
    }

    fun isRateAboveSafeZone(
        goal: GoalType,
        goalRateKcal: Int,
        weightKg: Double,
        ageYears: Int? = null,
    ): Boolean {
        if (goal == GoalType.MAINTAIN || goalRateKcal == 0 || weightKg <= 0) return false
        val weekly = abs(weeklyKgFromKcalPerDay(goalRateKcal))
        val max = if (goalRateKcal < 0) {
            safeWeeklyLossKg(weightKg, ageYears).endInclusive
        } else {
            safeWeeklyGainKg(weightKg).endInclusive
        }
        return weekly > max
    }

    fun removesDeficit(stage: LifeStage?): Boolean =
        stage == LifeStage.PREGNANCY || stage == LifeStage.LACTATION

    fun dailyTargets(
        profile: UserProfileEntity,
        weightKg: Double,
        todayEpochDay: Long,
    ): Targets {
        val warnings = mutableListOf<TargetWarning>()

        val estimate = energy(profile, weightKg, todayEpochDay)

        val rateKcal = if (removesDeficit(profile.lifeStage) && profile.goalRateKcal < 0) {
            warnings += TargetWarning.NO_DEFICIT_IN_PREGNANCY
            0
        } else {
            profile.goalRateKcal
        }

        val age = ageYears(profile.birthEpochDay, todayEpochDay)
        if (isRateAboveSafeZone(profile.goalType, rateKcal, weightKg, age)) {
            warnings += TargetWarning.RATE_ABOVE_SAFE_ZONE
        }

        if (rateKcal != 0 && hasReachedGoalWeight(profile.goalWeightKg, weightKg)) {
            warnings += TargetWarning.GOAL_WEIGHT_REACHED
        }

        var kcal = (estimate.tdee + rateKcal).roundToInt()

        val absoluteFloor = when (profile.sex) {
            Sex.MALE -> FLOOR_MALE
            Sex.FEMALE -> FLOOR_FEMALE
        }
        val bmrFloor = (estimate.bmr * BMR_FLOOR_FRACTION).roundToInt()
        if (kcal < absoluteFloor || kcal < bmrFloor) {

            warnings += if (absoluteFloor >= bmrFloor) {
                TargetWarning.FLOOR_CLAMPED
            } else {
                TargetWarning.BMR_FLOOR_CLAMPED
            }
            kcal = maxOf(absoluteFloor, bmrFloor)
        }

        return macros(profile, weightKg, kcal, estimate, warnings, ageYears = age)
    }

    private fun macros(
        profile: UserProfileEntity,
        weightKg: Double,
        kcal: Int,
        estimate: EnergyEstimate,
        warnings: MutableList<TargetWarning>,
        ageYears: Int,
    ): Targets {
        val lean = estimate.leanMassKg
        val inDeficit = profile.goalType == GoalType.LOSE || profile.goalRateKcal < 0
        val proteinFloorG = proteinFloorG(weightKg, lean, inDeficit, ageYears)

        if (profile.macroStrategy == MacroStrategy.CUSTOM) {

            val custom = profile.customProteinG ?: 0
            if (custom < proteinFloorG) warnings += TargetWarning.PROTEIN_BELOW_FLOOR
            return Targets(
                kcal = kcal,
                proteinG = custom,
                carbsG = profile.customCarbsG ?: 0,
                fatG = profile.customFatG ?: 0,
                warnings = warnings.toList(),
                energy = estimate,
            )
        }

        val proteinG = if (lean != null) {
            (proteinPerKgLean(profile.macroStrategy, profile.goalType) * lean).roundToInt()
        } else {
            (proteinPerKgWeight(profile.macroStrategy, profile.goalType) * weightKg).roundToInt()
        }.let { computed ->
            if (computed < proteinFloorG) {
                warnings += TargetWarning.PROTEIN_BELOW_FLOOR
                proteinFloorG
            } else {
                computed
            }
        }

        val fatG: Int
        val carbsG: Int
        when (profile.macroStrategy) {
            MacroStrategy.LOW_CARB -> {

                fatG = (kcal * 0.40 / KCAL_PER_G_FAT).roundToInt()
                carbsG = remainderCarbs(kcal, proteinG, fatG, warnings)
            }
            MacroStrategy.KETO -> {

                carbsG = 25
                val remaining = kcal - proteinG * KCAL_PER_G_PROTEIN - carbsG * KCAL_PER_G_CARB
                fatG = (remaining.coerceAtLeast(0) / KCAL_PER_G_FAT.toDouble()).roundToInt()
            }
            else -> {

                fatG = if (lean != null) {
                    (1.0 * lean).roundToInt()
                } else {
                    (0.8 * weightKg).roundToInt()
                }
                carbsG = remainderCarbs(kcal, proteinG, fatG, warnings)
            }
        }

        return Targets(kcal, proteinG, carbsG, fatG, warnings.toList(), estimate)
    }

    fun proteinFloorG(
        weightKg: Double,
        leanMassKg: Double?,
        inDeficit: Boolean,
        ageYears: Int? = null,
    ): Int {

        val rdaPerKg = if (ageYears != null && ageYears >= PROTEIN_OLDER_ADULT_AGE) {
            PROTEIN_RDA_PER_KG_OLDER
        } else {
            PROTEIN_RDA_PER_KG
        }
        val fromLeanOrWeight = if (leanMassKg != null) {
            val perKg = if (inDeficit) {
                PROTEIN_FLOOR_PER_KG_LEAN_DEFICIT
            } else {
                PROTEIN_FLOOR_PER_KG_LEAN
            }
            perKg * leanMassKg
        } else {
            val perKg = if (inDeficit) PROTEIN_FLOOR_PER_KG_DEFICIT else rdaPerKg
            perKg * weightKg
        }
        return maxOf(fromLeanOrWeight, rdaPerKg * weightKg).roundToInt()
    }

    private fun proteinPerKgLean(strategy: MacroStrategy, goal: GoalType): Double =
        when (strategy) {

            MacroStrategy.BALANCED -> if (goal == GoalType.LOSE || goal == GoalType.RECOMP) 2.4 else 2.2
            MacroStrategy.HIGH_PROTEIN -> 2.6
            MacroStrategy.LOW_CARB -> 2.4
            MacroStrategy.KETO -> 2.2
            MacroStrategy.CUSTOM -> error("tratado acima")
        }

    private fun proteinPerKgWeight(strategy: MacroStrategy, goal: GoalType): Double =
        when (strategy) {
            MacroStrategy.BALANCED -> if (goal == GoalType.LOSE || goal == GoalType.RECOMP) 2.0 else 1.8
            MacroStrategy.HIGH_PROTEIN -> 2.2
            MacroStrategy.LOW_CARB -> 2.0
            MacroStrategy.KETO -> 1.8
            MacroStrategy.CUSTOM -> error("tratado acima")
        }

    private fun remainderCarbs(
        kcal: Int,
        proteinG: Int,
        fatG: Int,
        warnings: MutableList<TargetWarning>,
    ): Int {
        val remaining = kcal - proteinG * KCAL_PER_G_PROTEIN - fatG * KCAL_PER_G_FAT
        if (remaining < 0) {
            warnings += TargetWarning.CARBS_CLAMPED_TO_ZERO
            return 0
        }
        return remaining / KCAL_PER_G_CARB
    }
}
