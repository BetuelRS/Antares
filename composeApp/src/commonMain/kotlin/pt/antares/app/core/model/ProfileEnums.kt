package pt.antares.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Sex { MALE, FEMALE }

/**
 * Multiplicadores do metabolismo basal. Os atuais são mais baixos do que os clássicos de
 * Harris-Benedict, que estão no `legacyMultiplier`: a app soma o exercício registado por
 * cima do gasto, e os antigos já o contavam lá dentro — usá-los dava o treino a dobrar.
 *
 * O valor antigo continua aqui só para o [ProfileMigration] poder explicar a quem já usava
 * a app porque é que a meta mudou.
 */
@Serializable
enum class ActivityLevel(val multiplier: Double, val legacyMultiplier: Double) {

    SEDENTARY(1.20, 1.2),

    LIGHT(1.30, 1.375),

    MODERATE(1.45, 1.55),

    HIGH(1.60, 1.725),

    ATHLETE(1.70, 1.9),
}

/**
 * Da mais fiável para a menos. A ordem importa porque só as duas primeiras servem para o
 * basal por massa magra: a estimativa por IMC não traz informação que o peso e a altura já
 * não tenham — ver `usableLeanMassKg`.
 */
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

/**
 * O que o seletor de ritmo oferece, em quilos por semana. São limites de interface, mais
 * largos do que a zona segura que o [NutritionCalc] avalia: a app deixa escolher e avisa,
 * em vez de impedir.
 */
object GoalRates {
    const val MAINTAIN = 0

    const val DEFAULT_LOSE_KG_WEEK = 0.5
    // Ganhar de propósito é mais lento do que perder: mais depressa é quase tudo gordura.
    const val DEFAULT_GAIN_KG_WEEK = 0.25

    val LOSE_RANGE_KG_WEEK = 0.1..1.5
    val GAIN_RANGE_KG_WEEK = 0.05..0.75

    // 50 g por semana é o passo mais fino que ainda muda a meta em calorias inteiras.
    const val STEP_KG_WEEK = 0.05

    // Recomposição é perder gordura e ganhar músculo ao mesmo peso, por isso o ritmo é
    // quase nulo — o que muda é a composição, não a balança.
    const val RECOMP_KG_WEEK = 0.1
}

/**
 * Fases que mudam o que a app pode propor. Não é serializável nem obrigatória: só se
 * declara quem quiser, e mexe em duas coisas — o défice desaparece na gravidez e na
 * amamentação, e algumas referências de micronutrientes sobem, no [LifeStageDrv].
 */
enum class LifeStage {

    NONE,
    PREGNANCY,
    LACTATION,

    POSTMENOPAUSAL,
}
