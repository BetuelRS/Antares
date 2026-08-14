package pt.antares.app.core.nutrition

import kotlinx.serialization.json.Json
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex

/**
 * Lê os micronutrientes guardados em JSON nos registos do diário e transforma-os numa
 * ficha nutricional. `breakdown` a null quer dizer que não há nada para mostrar — o JSON
 * pode faltar, estar corrompido, ou só trazer chaves que a app não conhece.
 */
data class LogNutrition(val breakdown: NutritionBreakdown?) {
    companion object {
        // JSON tolerante e `runCatching` em cada leitura: estes textos foram escritos por
        // versões anteriores da app e por respostas de fora, e um deles malformado não pode
        // deitar abaixo o dia inteiro.
        private val json = Json { ignoreUnknownKeys = true }

        val EMPTY = LogNutrition(null)

        /** Soma vários registos num só. Recebe pares de JSON e gramas desse registo. */
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
            // O total já está em quantidades absolutas, por isso entra com 100 g: é a
            // maneira de o [NutritionFacts] o deixar passar sem voltar a multiplicar.
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

            // Filtra pelas chaves conhecidas: um JSON antigo pode trazer nomes que já não
            // existem, e passá-los adiante daria linhas sem etiqueta no ecrã.
            val known = per100.filterKeys { it in Nutrients.ALL }
            if (known.isEmpty()) return EMPTY

            return LogNutrition(
                NutritionFacts.build(known, grams, reference, sex, stage).takeIf { !it.isEmpty },
            )
        }
    }
}
