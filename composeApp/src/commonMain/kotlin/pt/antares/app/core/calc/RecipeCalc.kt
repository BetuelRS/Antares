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

    const val MIN_COVERAGE = 0.6

    fun compute(ingredients: List<IngredientNutrition>, yieldGrams: Double?): RecipeNutrition {
        var kcal = 0.0
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0
        var rawGrams = 0.0

        val totals = HashMap<String, Double>()
        val covered = HashMap<String, Double>()

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
            if ((covered[key] ?: 0.0) / rawGrams < MIN_COVERAGE) return null
            return (totals[key] ?: return null) / basis * 100
        }

        val micros = buildMap {
            for (key in totals.keys) {
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

    private const val KEY_SUGARS = "__sugars"
    private const val KEY_SATFAT = "__satfat"
    private const val KEY_FIBER = "__fiber"
    private const val KEY_SODIUM = "__sodium"
    private val SECONDARY_KEYS = setOf(KEY_SUGARS, KEY_SATFAT, KEY_FIBER, KEY_SODIUM)
}
