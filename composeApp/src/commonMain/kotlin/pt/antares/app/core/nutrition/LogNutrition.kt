package pt.antares.app.core.nutrition

import kotlinx.serialization.json.Json
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex

data class LogNutrition(val breakdown: NutritionBreakdown?) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        val EMPTY = LogNutrition(null)

        fun ofLogs(
            logs: List<Pair<String?, Double>>,
            reference: EfsaReference?,
            sex: Sex,
            stage: LifeStage? = null,
        ): LogNutrition {
            val totals = HashMap<String, Double>()
            for ((microsJson, grams) in logs) {
                val per100 = microsJson
                    ?.let { runCatching { json.decodeFromString<Map<String, Double>>(it) }.getOrNull() }
                    ?: continue
                for ((key, value) in per100) {
                    if (key !in Nutrients.ALL) continue
                    totals[key] = (totals[key] ?: 0.0) + value * grams / 100.0
                }
            }
            if (totals.isEmpty()) return EMPTY
            return LogNutrition(
                NutritionFacts.build(totals, 100.0, reference, sex, stage).takeIf { !it.isEmpty },
            )
        }

        fun of(
            microsPer100Json: String?,
            grams: Double,
            reference: EfsaReference?,
            sex: Sex,
            stage: LifeStage? = null,
        ): LogNutrition {
            val per100 = microsPer100Json
                ?.let { runCatching { json.decodeFromString<Map<String, Double>>(it) }.getOrNull() }
                ?: return EMPTY

            val known = per100.filterKeys { it in Nutrients.ALL }
            if (known.isEmpty()) return EMPTY

            return LogNutrition(
                NutritionFacts.build(known, grams, reference, sex, stage).takeIf { !it.isEmpty },
            )
        }
    }
}
