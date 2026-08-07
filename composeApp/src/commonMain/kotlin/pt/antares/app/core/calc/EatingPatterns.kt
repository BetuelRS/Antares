package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.roundToInt

object EatingPatterns {

    const val MIN_LOGGED_DAYS = 14

    const val MIN_DAYS_PER_SIDE = 3

    const val MIN_KCAL_DIFFERENCE = 200

    const val MIN_PROTEIN_DIFFERENCE = 15

    const val CONCENTRATION_THRESHOLD = 0.45

    enum class Kind {

        WEEKEND_HIGHER,

        WEEKEND_LOWER,

        WEEKEND_PROTEIN_DROP,

        MEAL_CONCENTRATION,
    }

    data class Pattern(
        val kind: Kind,
        val value: Int,
        val label: String? = null,
    )

    data class Day(
        val epochDay: Long,
        val kcal: Double,
        val proteinG: Double,

        val kcalBySlot: Map<String, Double> = emptyMap(),
    )

    fun isWeekend(epochDay: Long): Boolean = ((epochDay + 3) % 7 + 7) % 7 >= 5

    fun detect(days: List<Day>): List<Pattern> {
        val comRegisto = days.filter { it.kcal > 0 }
        if (comRegisto.size < MIN_LOGGED_DAYS) return emptyList()

        val out = mutableListOf<Pattern>()
        val fds = comRegisto.filter { isWeekend(it.epochDay) }
        val semana = comRegisto.filterNot { isWeekend(it.epochDay) }

        if (fds.size >= MIN_DAYS_PER_SIDE && semana.size >= MIN_DAYS_PER_SIDE) {
            val deltaKcal = fds.map { it.kcal }.average() - semana.map { it.kcal }.average()
            if (abs(deltaKcal) >= MIN_KCAL_DIFFERENCE) {
                out += Pattern(
                    kind = if (deltaKcal > 0) Kind.WEEKEND_HIGHER else Kind.WEEKEND_LOWER,
                    value = abs(deltaKcal).roundToInt(),
                )
            }

            val deltaProteina = semana.map { it.proteinG }.average() - fds.map { it.proteinG }.average()

            if (deltaProteina >= MIN_PROTEIN_DIFFERENCE) {
                out += Pattern(Kind.WEEKEND_PROTEIN_DROP, deltaProteina.roundToInt())
            }
        }

        concentration(comRegisto)?.let { out += it }
        return out
    }

    private fun concentration(days: List<Day>): Pattern? {
        val totalPorSlot = HashMap<String, Double>()
        var total = 0.0
        for (dia in days) {
            for ((slot, kcal) in dia.kcalBySlot) {
                totalPorSlot[slot] = (totalPorSlot[slot] ?: 0.0) + kcal
                total += kcal
            }
        }
        if (total <= 0.0) return null
        val maior = totalPorSlot.maxByOrNull { it.value } ?: return null
        val fracao = maior.value / total
        if (fracao < CONCENTRATION_THRESHOLD) return null
        return Pattern(
            kind = Kind.MEAL_CONCENTRATION,
            value = (fracao * 100).roundToInt(),
            label = maior.key,
        )
    }
}
