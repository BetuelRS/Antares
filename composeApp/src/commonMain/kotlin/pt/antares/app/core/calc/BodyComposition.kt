package pt.antares.app.core.calc

import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.Sex
import kotlin.math.log10

enum class BmiCategory { UNDERWEIGHT, HEALTHY, OVERWEIGHT, OBESE }

enum class WaistRisk { HEALTHY, INCREASED, HIGH }

data class BodyStats(
    val bmi: Double?,
    val bmiCategory: BmiCategory?,

    val healthyWeightRangeKg: ClosedFloatingPointRange<Double>?,
    val bodyFatPct: Double?,
    val bodyFatSource: BodyFatSource?,
    val leanMassKg: Double?,
    val fatMassKg: Double?,
    val waistToHeight: Double?,
    val waistRisk: WaistRisk?,

    val ffmi: Double? = null,
)

object BodyComposition {

    const val BMI_UNDERWEIGHT = 18.5
    const val BMI_OVERWEIGHT = 25.0
    const val BMI_OBESE = 30.0

    const val WAIST_HEALTHY = 0.5
    const val WAIST_HIGH = 0.6

    fun bmi(weightKg: Double, heightCm: Int): Double? {
        if (weightKg <= 0 || heightCm <= 0) return null
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun bmiCategory(bmi: Double): BmiCategory = when {
        bmi < BMI_UNDERWEIGHT -> BmiCategory.UNDERWEIGHT
        bmi < BMI_OVERWEIGHT -> BmiCategory.HEALTHY
        bmi < BMI_OBESE -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }

    fun isGoalWeightBelowHealthy(goalWeightKg: Double, heightCm: Int): Boolean {
        val range = healthyWeightRange(heightCm) ?: return false
        return goalWeightKg < range.start
    }

    fun healthyWeightRange(heightCm: Int): ClosedFloatingPointRange<Double>? {
        if (heightCm <= 0) return null
        val m2 = (heightCm / 100.0) * (heightCm / 100.0)
        return (BMI_UNDERWEIGHT * m2)..(BMI_OVERWEIGHT * m2)
    }

    fun navyBodyFat(
        sex: Sex,
        heightCm: Int,
        waistCm: Double,
        neckCm: Double,
        hipCm: Double? = null,
    ): Double? {
        if (heightCm <= 0 || waistCm <= 0 || neckCm <= 0) return null
        val h = log10(heightCm.toDouble())
        val denominator = when (sex) {
            Sex.MALE -> {
                val d = waistCm - neckCm
                if (d <= 0) return null
                1.0324 - 0.19077 * log10(d) + 0.15456 * h
            }
            Sex.FEMALE -> {
                val hip = hipCm ?: return null
                val d = waistCm + hip - neckCm
                if (d <= 0) return null
                1.29579 - 0.35004 * log10(d) + 0.22100 * h
            }
        }
        if (denominator <= 0) return null
        val pct = 495.0 / denominator - 450.0
        return pct.takeIf { it.isFinite() && it in PLAUSIBLE_BODY_FAT }
    }

    fun deurenbergBodyFat(sex: Sex, bmi: Double, ageYears: Int): Double? {
        if (bmi <= 0 || ageYears <= 0) return null
        val sexTerm = if (sex == Sex.MALE) 1 else 0
        val pct = 1.20 * bmi + 0.23 * ageYears - 10.8 * sexTerm - 5.4
        return pct.takeIf { it.isFinite() && it in PLAUSIBLE_BODY_FAT }
    }

    fun ffmi(leanMassKg: Double, heightCm: Int): Double? {
        if (leanMassKg <= 0 || heightCm <= 0) return null
        val m = heightCm / 100.0
        return leanMassKg / (m * m)
    }

    fun leanMassKg(weightKg: Double, bodyFatPct: Double): Double? {
        if (weightKg <= 0 || bodyFatPct !in PLAUSIBLE_BODY_FAT) return null

        return NutritionCalc.roundToTenth(weightKg * (1 - bodyFatPct / 100.0))
    }

    fun waistRisk(ratio: Double): WaistRisk = when {
        ratio < WAIST_HEALTHY -> WaistRisk.HEALTHY
        ratio < WAIST_HIGH -> WaistRisk.INCREASED
        else -> WaistRisk.HIGH
    }

    fun stats(
        sex: Sex,
        weightKg: Double,
        heightCm: Int,
        ageYears: Int,
        bodyFatPct: Double? = null,
        bodyFatSource: BodyFatSource? = null,
        waistCm: Double? = null,
        neckCm: Double? = null,
        hipCm: Double? = null,
    ): BodyStats {
        val bmi = bmi(weightKg, heightCm)

        var fat = bodyFatPct?.takeIf { it in PLAUSIBLE_BODY_FAT }
        var source = if (fat != null) bodyFatSource ?: BodyFatSource.MEASURED else null
        if (fat == null && waistCm != null && neckCm != null) {
            fat = navyBodyFat(sex, heightCm, waistCm, neckCm, hipCm)
            if (fat != null) source = BodyFatSource.NAVY
        }
        if (fat == null && bmi != null) {
            fat = deurenbergBodyFat(sex, bmi, ageYears)
            if (fat != null) source = BodyFatSource.BMI
        }

        val lean = fat?.let { leanMassKg(weightKg, it) }
        val ratio = waistCm?.takeIf { it > 0 && heightCm > 0 }?.let { it / heightCm }

        return BodyStats(
            bmi = bmi,
            bmiCategory = bmi?.let { bmiCategory(it) },
            healthyWeightRangeKg = healthyWeightRange(heightCm),
            bodyFatPct = fat,
            bodyFatSource = source,
            leanMassKg = lean,
            fatMassKg = lean?.let { weightKg - it },
            waistToHeight = ratio,
            waistRisk = ratio?.let { waistRisk(it) },
            ffmi = lean?.let { ffmi(it, heightCm) },
        )
    }

    private val PLAUSIBLE_BODY_FAT = 3.0..70.0
}
