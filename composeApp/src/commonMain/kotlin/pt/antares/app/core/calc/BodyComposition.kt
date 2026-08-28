package pt.antares.app.core.calc

import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.Sex
import kotlin.math.log10

enum class BmiCategory { UNDERWEIGHT, HEALTHY, OVERWEIGHT, OBESE }

enum class WaistRisk { HEALTHY, INCREASED, HIGH }

/**
 * Retrato do corpo num instante. Todos os campos são anuláveis porque cada um depende
 * de medidas diferentes, e a app mostra o que tem em vez de exigir o perfil completo.
 */
data class BodyStats(
    val bmi: Double?,
    val bmiCategory: BmiCategory?,

    val healthyWeightRangeKg: ClosedFloatingPointRange<Double>?,
    val bodyFatPct: Double?,
    // A origem viaja com o valor porque decide o que se pode fazer com ele: o
    // [NutritionCalc] recusa massa magra vinda de uma estimativa por IMC.
    val bodyFatSource: BodyFatSource?,
    val leanMassKg: Double?,
    val fatMassKg: Double?,
    val waistToHeight: Double?,
    val waistRisk: WaistRisk?,

    val ffmi: Double? = null,
)

object BodyComposition {

    // Cortes da OMS para adultos. Não se ajustam por sexo nem por etnia: o IMC é um
    // indicador grosseiro, e afinar as fronteiras dava-lhe uma precisão que não tem.
    const val BMI_UNDERWEIGHT = 18.5
    const val BMI_OVERWEIGHT = 25.0
    const val BMI_OBESE = 30.0

    // Cintura a menos de metade da altura. Diz mais sobre gordura visceral do que o IMC
    // e dispensa tabelas por sexo.
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

    /** Serve para avisar no ecrã do objetivo, não para o impedir. */
    fun isGoalWeightBelowHealthy(goalWeightKg: Double, heightCm: Int): Boolean {
        val range = healthyWeightRange(heightCm) ?: return false
        return goalWeightKg < range.start
    }

    fun healthyWeightRange(heightCm: Int): ClosedFloatingPointRange<Double>? {
        if (heightCm <= 0) return null
        val m2 = (heightCm / 100.0) * (heightCm / 100.0)
        return (BMI_UNDERWEIGHT * m2)..(BMI_OVERWEIGHT * m2)
    }

    /**
     * O desvio sistemático do método das circunferências, contra a absorciometria.
     *
     * Potter et al. (2022), contra DEXA: a fórmula **subestima** a gordura dos homens em 2,6
     * pontos percentuais e **sobrestima** a das mulheres em 2,3. Não é ruído — é sempre na
     * mesma direção, todos os dias.
     *
     * **Porque isto importa mais do que a incerteza que a app já declara.** O erro-padrão de
     * 3,6 pp do [NavyUncertainty] é aleatório e anula-se ao longo do tempo; um viés não se
     * anula nunca. Num homem de 80 kg, 2,6 pp são 2,08 kg de massa magra a mais, e isso
     * inflaciona o metabolismo basal em cerca de 45 kcal por dia — na mesma direção, sempre.
     * O `AdaptiveTdee` acabaria por o corrigir, mas leva semanas e o número inicial fica
     * previsivelmente errado.
     *
     * O `NavyUncertainty` já dizia «com viés por sexo» no comentário dele e a conta não o
     * aplicava: o código descrevia-se a si próprio como fazendo uma coisa que não fazia.
     */
    private fun viesDaFita(sex: Sex): Double = when (sex) {
        Sex.MALE -> VIES_HOMENS_PP
        Sex.FEMALE -> VIES_MULHERES_PP
    }

    /**
     * Massa gorda pelo método da marinha americana, a partir de fita métrica. Precisa de
     * anca nas mulheres; sem ela devolve null em vez de improvisar com a fórmula masculina.
     *
     * O resultado sai **já corrigido do viés por sexo** — ver [viesDaFita]. O filtro de
     * plausibilidade aplica-se depois da correcção, que é onde ele descreve o número que a
     * app vai mesmo mostrar.
     */
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
                // Pescoço maior do que a cintura é engano de campo trocado; o log10 de um
                // número não positivo rebentaria a conta a seguir.
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
        val pct = 495.0 / denominator - 450.0 + viesDaFita(sex)
        return pct.takeIf { it.isFinite() && it in PLAUSIBLE_BODY_FAT }
    }

    /**
     * Estimativa de último recurso, a partir do IMC, idade e sexo. Não mede nada de novo,
     * e é por isso que a sua origem fica marcada como [BodyFatSource.BMI].
     */
    fun deurenbergBodyFat(sex: Sex, bmi: Double, ageYears: Int): Double? {
        if (bmi <= 0 || ageYears <= 0) return null
        val sexTerm = if (sex == Sex.MALE) 1 else 0
        val pct = 1.20 * bmi + 0.23 * ageYears - 10.8 * sexTerm - 5.4
        return pct.takeIf { it.isFinite() && it in PLAUSIBLE_BODY_FAT }
    }

    /** Índice de massa magra: o IMC sem contar a gordura, para acompanhar ganho de músculo. */
    fun ffmi(leanMassKg: Double, heightCm: Int): Double? {
        if (leanMassKg <= 0 || heightCm <= 0) return null
        val m = heightCm / 100.0
        return leanMassKg / (m * m)
    }

    fun leanMassKg(weightKg: Double, bodyFatPct: Double): Double? {
        if (weightKg <= 0 || bodyFatPct !in PLAUSIBLE_BODY_FAT) return null

        // Uma casa decimal, a mesma do basal: a massa magra alimenta a Katch-McArdle, e
        // arredondar só no fim fazia o basal mostrado divergir do guardado.
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

        // Cascata da melhor origem para a pior: valor medido, depois fita métrica, e só
        // então a estimativa por IMC. Cada degrau só corre se o anterior não deu nada.
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

    // Fora deste intervalo é engano de digitação ou unidade trocada: 3% é o limite da
    // sobrevivência e 70% não existe em ninguém que consiga usar a app.
    private val PLAUSIBLE_BODY_FAT = 3.0..70.0

    // Potter et al. (2022), circunferências contra DEXA. Somam-se porque é o que corrige a
    // direção do desvio: a fórmula dá pouco aos homens e demais às mulheres.
    private const val VIES_HOMENS_PP = 2.6
    private const val VIES_MULHERES_PP = -2.3
}
