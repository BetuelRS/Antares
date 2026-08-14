package pt.antares.app.core.calc

import kotlin.math.roundToInt

data class IngredientNutrition(
    val kcalPer100: Int,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val grams: Double,

    val sugarsPer100: Double? = null,
    val satFatPer100: Double? = null,
    val fiberPer100: Double? = null,
    val sodiumMgPer100: Double? = null,
    val microsPer100: Map<String, Double> = emptyMap(),
)

/**
 * O resultado da receita. Os totais são da receita inteira; os `per100` derivam deles
 * pela base, que é o peso depois de cozinhar quando esse valor existe.
 */
data class RecipeNutrition(
    val totalKcal: Int,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val basisGrams: Double,

    val sugarsPer100: Double? = null,
    val satFatPer100: Double? = null,
    val fiberPer100: Double? = null,
    val sodiumMgPer100: Double? = null,

    val microsPer100: Map<String, Double> = emptyMap(),
) {
    val kcalPer100: Int get() = per100(totalKcal.toDouble()).roundToInt()
    val proteinPer100: Double get() = per100(totalProteinG)
    val carbsPer100: Double get() = per100(totalCarbsG)
    val fatPer100: Double get() = per100(totalFatG)

    private fun per100(total: Double): Double = if (basisGrams > 0) total / basisGrams * 100 else 0.0
}

object RecipeCalc {

    // Um nutriente só sai da receita se os ingredientes que o declaram pesarem pelo menos
    // 60% do total. Abaixo disto o valor descreve parte do prato e passaria por descrever
    // o prato todo — em micronutrientes, quase sempre por defeito.
    const val MIN_COVERAGE = 0.6

    /**
     * Soma os ingredientes e reduz a valores por 100 g. `yieldGrams` é o peso final: a
     * água que evapora concentra tudo, e sem esse valor a receita fica pelo peso cru.
     */
    fun compute(ingredients: List<IngredientNutrition>, yieldGrams: Double?): RecipeNutrition {
        var kcal = 0.0
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0
        var rawGrams = 0.0

        val totals = HashMap<String, Double>()
        val covered = HashMap<String, Double>()

        // `covered` acompanha `totals` para saber sobre que peso da receita cada nutriente
        // foi de facto declarado. Um ingrediente sem o campo não entra em nenhum dos dois.
        fun add(key: String, per100: Double?, grams: Double) {
            if (per100 == null) return
            totals[key] = (totals[key] ?: 0.0) + per100 * grams / 100.0
            covered[key] = (covered[key] ?: 0.0) + grams
        }

        for (i in ingredients) {
            val factor = i.grams / 100.0
            kcal += i.kcalPer100 * factor
            protein += i.proteinPer100 * factor
            carbs += i.carbsPer100 * factor
            fat += i.fatPer100 * factor
            rawGrams += i.grams
            add(KEY_SUGARS, i.sugarsPer100, i.grams)
            add(KEY_SATFAT, i.satFatPer100, i.grams)
            add(KEY_FIBER, i.fiberPer100, i.grams)
            add(KEY_SODIUM, i.sodiumMgPer100, i.grams)
            for ((k, v) in i.microsPer100) add(k, v, i.grams)
        }
        val basis = yieldGrams?.takeIf { it > 0 } ?: rawGrams

        fun resolve(key: String): Double? {
            if (basis <= 0 || rawGrams <= 0) return null
            // A cobertura mede-se sobre o peso cru, que é o que os ingredientes somam. O
            // peso final só entra na divisão, para não penalizar receitas que perdem água.
            if ((covered[key] ?: 0.0) / rawGrams < MIN_COVERAGE) return null
            return (totals[key] ?: return null) / basis * 100
        }

        val micros = buildMap {
            for (key in totals.keys) {
                // Fibra, açúcares, gordura saturada e sódio têm campo próprio no resultado;
                // deixá-los também no mapa duplicava-os no ecrã dos micronutrientes.
                if (key in SECONDARY_KEYS) continue
                resolve(key)?.let { if (it > 0) put(key, it) }
            }
        }
        return RecipeNutrition(
            totalKcal = kcal.roundToInt(),
            totalProteinG = protein,
            totalCarbsG = carbs,
            totalFatG = fat,
            basisGrams = basis,
            sugarsPer100 = resolve(KEY_SUGARS),
            satFatPer100 = resolve(KEY_SATFAT),
            fiberPer100 = resolve(KEY_FIBER),
            sodiumMgPer100 = resolve(KEY_SODIUM),
            microsPer100 = micros,
        )
    }

    // Prefixo duplo para não colidirem com nenhum código de micronutriente real, já que
    // partilham o mesmo mapa durante a soma.
    private const val KEY_SUGARS = "__sugars"
    private const val KEY_SATFAT = "__satfat"
    private const val KEY_FIBER = "__fiber"
    private const val KEY_SODIUM = "__sodium"
    private val SECONDARY_KEYS = setOf(KEY_SUGARS, KEY_SATFAT, KEY_FIBER, KEY_SODIUM)
}
