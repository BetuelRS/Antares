package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hábitos que só aparecem ao fim de semanas. A app não julga nenhum deles — descreve o
 * que se repete, e é a pessoa que decide se quer mudar.
 */
object EatingPatterns {

    // Duas semanas. Abaixo disto qualquer diferença entre fim de semana e semana pode ser
    // um jantar fora, e a app estaria a chamar hábito a um acaso.
    const val MIN_LOGGED_DAYS = 14

    // Três dias de cada lado: com um ou dois, a média é o próprio dia.
    const val MIN_DAYS_PER_SIDE = 3

    // Limiares de relevância, não de significância estatística. 200 kcal por dia é o que
    // se nota ao fim do mês; menos do que isso não vale um aviso no ecrã.
    const val MIN_KCAL_DIFFERENCE = 200

    const val MIN_PROTEIN_DIFFERENCE = 15

    // Quase metade das calorias numa só refeição. Interessa por causa da saciedade e da
    // proteína ao longo do dia, não porque a hora engorde.
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

    // O dia 0 da era é uma quinta-feira; somar 3 põe a segunda em zero, e 5 e 6 ficam a
    // ser sábado e domingo. O segundo `% 7` trata dos restos negativos antes de 1970.
    fun isWeekend(epochDay: Long): Boolean = ((epochDay + 3) % 7 + 7) % 7 >= 5

    fun detect(days: List<Day>): List<Pattern> {
        // Dia sem calorias é dia por registar, não dia de jejum: entrava nas médias como
        // zero e inventava uma quebra ao fim de semana em quem só se esquece de apontar.
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

            // Só a queda ao fim de semana é padrão: comer mais proteína ao sábado não é
            // um problema que valha a pena nomear, e a subtração já está nessa ordem.
            if (deltaProteina >= MIN_PROTEIN_DIFFERENCE) {
                out += Pattern(Kind.WEEKEND_PROTEIN_DROP, deltaProteina.roundToInt())
            }
        }

        concentration(comRegisto)?.let { out += it }
        return out
    }

    /** A fração é do total de todos os dias, não a média das frações diárias. */
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
