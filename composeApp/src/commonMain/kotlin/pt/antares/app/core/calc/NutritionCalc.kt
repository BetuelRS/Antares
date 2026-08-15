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

/**
 * O que a app corrigiu ou recusou ao calcular as metas. Nada aqui impede o cálculo:
 * os alvos saem sempre, e o aviso serve para o ecrã explicar porquê é que o número
 * não é o que a conta simples daria.
 */
enum class TargetWarning {

    FLOOR_CLAMPED,

    BMR_FLOOR_CLAMPED,

    CARBS_CLAMPED_TO_ZERO,

    RATE_ABOVE_SAFE_ZONE,

    PROTEIN_BELOW_FLOOR,

    GOAL_WEIGHT_REACHED,

    NO_DEFICIT_IN_PREGNANCY,
}

/**
 * Qual das três fórmulas deu o metabolismo basal. A escolha não é preferência: sem
 * massa gorda medida só a Mifflin funciona, porque as outras duas partem da massa magra.
 */
enum class BmrFormula {

    MIFFLIN_ST_JEOR,

    KATCH_MCARDLE,

    CUNNINGHAM,
}

/**
 * O que a app sabe sobre o gasto energético. Guarda-se junto dos alvos para o ecrã
 * poder mostrar de onde veio o número em vez de o afirmar sem origem.
 */
data class EnergyEstimate(
    val bmr: Double,
    val tdee: Double,
    val formula: BmrFormula,

    // Nulo quando a massa gorda não é utilizável — ver [NutritionCalc.usableLeanMassKg].
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

    // Chão absoluto de calorias para dieta sem acompanhamento clínico. Abaixo disto
    // a app deixa de ser um contador e passa a receitar jejum.
    const val FLOOR_MALE = 1500
    const val FLOOR_FEMALE = 1200

    // Segundo chão, este relativo à pessoa: comer abaixo de 80% do basal deixa de ser
    // défice e passa a ser subalimentação, mesmo que o número absoluto pareça aceitável.
    const val BMR_FLOOR_FRACTION = 0.80

    // Fatores de Atwater. São os mesmos que os rótulos usam, por isso a soma dos macros
    // bate certo com as calorias declaradas na embalagem.
    const val KCAL_PER_G_PROTEIN = 4
    const val KCAL_PER_G_CARB = 4
    const val KCAL_PER_G_FAT = 9

    private const val KATCH_BASE = 370.0
    private const val KATCH_PER_KG_LEAN = 21.6

    private const val CUNNINGHAM_BASE = 500.0
    private const val CUNNINGHAM_PER_KG_LEAN = 22.0

    // Ritmo seguro em fração do peso por semana, não em quilos fixos: 0,5 kg/semana é
    // moderado para quem tem 100 kg e agressivo para quem tem 50.
    const val SAFE_LOSS_MIN_PCT = 0.003
    const val SAFE_LOSS_MAX_PCT = 0.010
    // Ganhar é mais lento do que perder porque o corpo constrói músculo devagar; ritmo
    // maior é quase todo gordura.
    const val SAFE_GAIN_MIN_PCT = 0.0015
    const val SAFE_GAIN_MAX_PCT = 0.005

    const val ADULT_AGE = 18
    // Menor de idade ainda está a crescer: metade do ritmo máximo de perda do adulto.
    const val MINOR_LOSS_MAX_FACTOR = 0.5

    // Chão de proteína. Em défice sobe porque a proteína é o que impede o corpo de ir
    // buscar músculo à falta de calorias.
    // Em défice o chão por massa magra vive no [ProteinFloor], que o faz escalar com o
    // treino de força e com a profundidade do défice.
    const val PROTEIN_FLOOR_PER_KG_LEAN = 1.2
    const val PROTEIN_FLOOR_PER_KG_DEFICIT = 1.4

    // Dose recomendada da EFSA para adultos. É o mínimo de saúde, não o de desempenho.
    const val PROTEIN_RDA_PER_KG = 0.83

    // A partir dos 65 a recomendação sobe: o músculo responde pior à mesma dose.
    const val PROTEIN_OLDER_ADULT_AGE = 65
    const val PROTEIN_RDA_PER_KG_OLDER = 1.0

    fun mifflinSexTerm(sex: Sex): Double = when (sex) {
        Sex.MALE -> 5.0
        Sex.FEMALE -> -161.0
    }

    /** Mifflin-St Jeor. Peso em kg, altura em cm, idade em anos; devolve kcal/dia. */
    fun bmr(sex: Sex, weightKg: Double, heightCm: Int, ageYears: Int): Double {

        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return base + mifflinSexTerm(sex)
    }

    fun bmrKatchMcArdle(leanMassKg: Double): Double =
        KATCH_BASE + KATCH_PER_KG_LEAN * leanMassKg

    fun bmrCunningham(leanMassKg: Double): Double =
        CUNNINGHAM_BASE + CUNNINGHAM_PER_KG_LEAN * leanMassKg

    fun tdee(bmr: Double, activityMultiplier: Double): Double = bmr * activityMultiplier

    /**
     * Massa magra, mas só quando a origem da massa gorda a torna fiável. Devolve null
     * para quem não tem medição.
     */
    fun usableLeanMassKg(profile: UserProfileEntity, weightKg: Double): Double? {
        val pct = profile.bodyFatPct ?: return null

        val source = profile.bodyFatSource ?: BodyFatSource.MEASURED
        // Massa gorda estimada pelo IMC é o próprio peso e altura por outra via: usá-la
        // na Katch-McArdle daria um basal com ar de personalizado e sem informação nova.
        if (source == BodyFatSource.BMI) return null
        return BodyComposition.leanMassKg(weightKg, pct)
    }

    fun roundToTenth(value: Double): Double = (value * 10).roundToLong() / 10.0

    /**
     * Escolhe a fórmula e devolve basal e gasto diário. A escolha depende só dos dados
     * disponíveis; o override do perfil apenas decide entre as duas de massa magra.
     */
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

        // Arredonda-se antes de multiplicar pela atividade para que o basal mostrado no
        // ecrã e o basal usado na conta sejam o mesmo número.
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

        // Ano médio gregoriano, com os bissextos incluídos. Dividir por 365 adiantava a
        // idade quase um ano a quem tem 80.
        return (days / 365.2425).toInt()
    }

    // 7700 kcal por quilo é a equivalência clássica da gordura corporal, e é a mesma
    // que o [AdaptiveTdee] usa a inferir o gasto — as duas contas têm de coincidir.
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

    // A tolerância existe porque o peso oscila mais do que isto de um dia para o outro:
    // exigir o valor exato deixava o objetivo por atingir para sempre.
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
        // Só o teto interessa: um ritmo abaixo do mínimo é lento, não é perigoso.
        return weekly > max
    }

    fun removesDeficit(stage: LifeStage?): Boolean =
        stage == LifeStage.PREGNANCY || stage == LifeStage.LACTATION

    /**
     * Ponto de entrada das metas do dia. A ordem importa: o défice é anulado antes de
     * qualquer conta, e os chãos são aplicados depois de somar o ritmo ao gasto.
     */
    fun dailyTargets(
        profile: UserProfileEntity,
        weightKg: Double,
        todayEpochDay: Long,
        // Sai do histórico de treinos, e não do nível de atividade: um trabalhador da
        // construção é muito ativo e não treina força, e o intervalo de Helms fala de
        // treino de resistência. Falso por omissão — quem chama sem saber não sobe nada.
        treinaForca: Boolean = false,
    ): Targets {
        val warnings = mutableListOf<TargetWarning>()

        val estimate = energy(profile, weightKg, todayEpochDay)

        // Na gravidez e na amamentação o défice desaparece em vez de ser reduzido: não há
        // ritmo de perda aceitável para a app propor sem acompanhamento.
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

            // O aviso nomeia o chão que mandou, não os dois: quem tem basal alto é travado
            // pelos 80% muito antes de chegar ao mínimo absoluto, e a explicação no ecrã
            // tem de bater certo com o número aplicado.
            warnings += if (absoluteFloor >= bmrFloor) {
                TargetWarning.FLOOR_CLAMPED
            } else {
                TargetWarning.BMR_FLOOR_CLAMPED
            }
            kcal = maxOf(absoluteFloor, bmrFloor)
        }

        return macros(profile, weightKg, kcal, estimate, warnings, ageYears = age, treinaForca = treinaForca)
    }

    /**
     * Reparte as calorias já fixadas. A proteína é decidida primeiro e a gordura a seguir,
     * ficando os hidratos com o resto — são o macro sem mínimo fisiológico.
     */
    private fun macros(
        profile: UserProfileEntity,
        weightKg: Double,
        kcal: Int,
        estimate: EnergyEstimate,
        warnings: MutableList<TargetWarning>,
        ageYears: Int,
        treinaForca: Boolean,
    ): Targets {
        val lean = estimate.leanMassKg
        // Basta o ritmo ser negativo: quem escolheu manter mas pôs um défice manual está
        // em défice para efeitos de proteína.
        val inDeficit = profile.goalType == GoalType.LOSE || profile.goalRateKcal < 0
        val proteinFloorG = proteinFloorG(
            weightKg = weightKg,
            leanMassKg = lean,
            inDeficit = inDeficit,
            ageYears = ageYears,
            treinaForca = treinaForca,
            deficitFraction = ProteinFloor.deficitFraction(profile.goalRateKcal, estimate.tdee),
        )

        if (profile.macroStrategy == MacroStrategy.CUSTOM) {

            // Nos macros manuais a app avisa mas não corrige: o número é do utilizador, e
            // sobrepor-lhe o chão tornaria o campo impossível de usar.
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

        // Havendo massa magra, a proteína conta-se sobre ela: o tecido gordo não precisa
        // de ser alimentado a proteína, e por peso total quem tem muita gordura recebia a mais.
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

                // Em low-carb a gordura é fixada por percentagem das calorias, não por peso
                // corporal: é ela que substitui os hidratos que saíram.
                fatG = (kcal * 0.40 / KCAL_PER_G_FAT).roundToInt()
                carbsG = remainderCarbs(kcal, proteinG, fatG, warnings)
            }
            MacroStrategy.KETO -> {

                // Em keto a ordem inverte-se: os hidratos são o valor fixo — o limiar comum
                // para manter cetose — e a gordura é que absorve o que sobra.
                carbsG = 25
                val remaining = kcal - proteinG * KCAL_PER_G_PROTEIN - carbsG * KCAL_PER_G_CARB
                fatG = (remaining.coerceAtLeast(0) / KCAL_PER_G_FAT.toDouble()).roundToInt()
            }
            else -> {

                // Um grama de gordura por quilo de massa magra. É o mínimo comum para hormonas
                // e absorção das vitaminas lipossolúveis, não um alvo de desempenho.
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

    /**
     * O mínimo de proteína em gramas. Nunca desce abaixo da dose recomendada calculada
     * sobre o peso total, mesmo quando a conta por massa magra daria menos.
     */
    fun proteinFloorG(
        weightKg: Double,
        leanMassKg: Double?,
        inDeficit: Boolean,
        ageYears: Int? = null,
        // Quem treina força em défice precisa de mais — ver [ProteinFloor]. Por omissão
        // falso: os dois só sobem o chão de quem tem as duas coisas comprovadas.
        treinaForca: Boolean = false,
        deficitFraction: Double = 0.0,
    ): Int {

        val rdaPerKg = if (ageYears != null && ageYears >= PROTEIN_OLDER_ADULT_AGE) {
            PROTEIN_RDA_PER_KG_OLDER
        } else {
            PROTEIN_RDA_PER_KG
        }
        val fromLeanOrWeight = if (leanMassKg != null) {
            val perKg = if (inDeficit) {
                ProteinFloor.perKgLean(treinaForca, deficitFraction)
            } else {
                PROTEIN_FLOOR_PER_KG_LEAN
            }
            perKg * leanMassKg
        } else {
            // Sem massa magra o chão não escala com o treino: o intervalo de Helms é por
            // quilo de massa magra, e aplicá-lo ao peso todo dava mais proteína a quem tem
            // mais gordura — exatamente ao contrário do que ele diz.
            val perKg = if (inDeficit) PROTEIN_FLOOR_PER_KG_DEFICIT else rdaPerKg
            perKg * weightKg
        }
        // O máximo protege quem tem muita gordura: 1,2 g por kg de massa magra pode ficar
        // abaixo da recomendação de saúde calculada sobre o peso todo.
        return maxOf(fromLeanOrWeight, rdaPerKg * weightKg).roundToInt()
    }

    private fun proteinPerKgLean(strategy: MacroStrategy, goal: GoalType): Double =
        when (strategy) {

            // Perder e recomposição pedem mais do que ganhar: é em défice que o músculo está
            // em risco, não em superavit.
            MacroStrategy.BALANCED -> if (goal == GoalType.LOSE || goal == GoalType.RECOMP) 2.4 else 2.2
            MacroStrategy.HIGH_PROTEIN -> 2.6
            MacroStrategy.LOW_CARB -> 2.4
            MacroStrategy.KETO -> 2.2
            // A estratégia manual sai em [macros] antes de chegar aqui.
            MacroStrategy.CUSTOM -> error("tratado acima")
        }

    // Os mesmos valores baixados cerca de 15%, porque agora multiplicam o peso todo e não
    // só a massa magra.
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
        // Acontece quando os chãos de proteína e gordura já gastaram todas as calorias —
        // corpo pesado com meta baixa. Hidratos negativos não são uma meta.
        if (remaining < 0) {
            warnings += TargetWarning.CARBS_CLAMPED_TO_ZERO
            return 0
        }
        return remaining / KCAL_PER_G_CARB
    }
}
