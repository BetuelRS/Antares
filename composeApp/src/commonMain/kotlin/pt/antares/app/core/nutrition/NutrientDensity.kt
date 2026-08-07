package pt.antares.app.core.nutrition

data class NutrientRich(
    val foodId: String,
    val name: String,
    val kcal: Int,
    val perKcalPct: Int,
    val per100gPct: Int,
)

object NutrientDensity {

    const val MIN_PER_KCAL_PCT = 5

    const val MAX_PER_100G_PCT = 400

    fun rank(
        foods: List<Triple<String, String, Int>>,
        microsByFood: Map<String, Map<String, Double>>,
        key: String,
        drv: Double,
        limit: Int = 40,
    ): List<NutrientRich> {
        if (drv <= 0) return emptyList()
        return foods.mapNotNull { (id, name, kcal) ->
            if (kcal <= 0) return@mapNotNull null
            val per100g = microsByFood[id]?.get(key)?.takeIf { it > 0 } ?: return@mapNotNull null

            val perKcal = per100g * 100.0 / kcal
            val perKcalPct = (perKcal / drv * 100).toInt()
            if (perKcalPct < MIN_PER_KCAL_PCT) return@mapNotNull null
            val per100gPct = (per100g / drv * 100).toInt()
            if (per100gPct > MAX_PER_100G_PCT) return@mapNotNull null
            NutrientRich(
                foodId = id,
                name = name,
                kcal = kcal,
                perKcalPct = perKcalPct,
                per100gPct = per100gPct,
            )
        }
            .sortedWith(compareByDescending<NutrientRich> { it.perKcalPct }.thenBy { it.name })
            .take(limit)
    }
}
