package pt.antares.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Sex { MALE, FEMALE }

@Serializable
enum class ActivityLevel(val multiplier: Double, val legacyMultiplier: Double) {

    SEDENTARY(1.20, 1.2),

    LIGHT(1.30, 1.375),

    MODERATE(1.45, 1.55),

    HIGH(1.60, 1.725),

    ATHLETE(1.70, 1.9),
}

@Serializable
enum class BodyFatSource {

    MEASURED,

    NAVY,

    BMI,
}

@Serializable
enum class GoalType { LOSE, MAINTAIN, GAIN, RECOMP }

@Serializable
enum class MacroStrategy { BALANCED, HIGH_PROTEIN, LOW_CARB, KETO, CUSTOM }

@Serializable
enum class UnitSystem { METRIC, IMPERIAL }

@Serializable
enum class EnergyUnit { KCAL, KJ }

object GoalRates {
    const val MAINTAIN = 0

    const val DEFAULT_LOSE_KG_WEEK = 0.5
    const val DEFAULT_GAIN_KG_WEEK = 0.25

    val LOSE_RANGE_KG_WEEK = 0.1..1.5
    val GAIN_RANGE_KG_WEEK = 0.05..0.75

    const val STEP_KG_WEEK = 0.05

    const val RECOMP_KG_WEEK = 0.1
}

enum class LifeStage {

    NONE,
    PREGNANCY,
    LACTATION,

    POSTMENOPAUSAL,
}
