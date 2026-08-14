package pt.antares.app.core.calc

import kotlin.math.abs

/**
 * Encontra a última vez que a balança marcou este mesmo peso. Serve para separar peso de
 * composição: o número pode ser o mesmo e a cintura ter mudado.
 */
object BeenHereCalc {

    const val SAME_WEIGHT_TOLERANCE_KG = 0.5

    // Dois meses. Sem isto, a pesagem da semana passada qualificava-se, e dizer a alguém
    // que já esteve neste peso há quatro dias não é informação nenhuma.
    const val MIN_DAYS_APART = 60

    data class Visit(
        val epochDay: Long,
        val weightKg: Double,
        val daysAgo: Long,
        val waistCm: Double? = null,
        val bodyFatPct: Double? = null,
    ) {

        // Sem medidas dessa altura a visita não tem nada para contrapor ao peso, e o ecrã
        // fica-se por uma data em vez de prometer uma comparação vazia.
        val hasComparison: Boolean get() = waistCm != null || bodyFatPct != null
    }

    fun lastVisit(
        currentWeightKg: Double,
        today: Long,
        weighIns: List<Pair<Long, Double>>,
        waistByDay: List<Pair<Long, Double>> = emptyList(),
        bodyFatByDay: List<Pair<Long, Double>> = emptyList(),
    ): Visit? {
        val candidato = weighIns
            .filter { (dia, kg) ->
                today - dia >= MIN_DAYS_APART &&
                    abs(kg - currentWeightKg) <= SAME_WEIGHT_TOLERANCE_KG
            }

            // A mais recente das que passaram o filtro, não a mais antiga: a comparação
            // interessa contra a última vez, não contra o início de tudo.
            .maxByOrNull { it.first }
            ?: return null

        val (dia, kg) = candidato
        return Visit(
            epochDay = dia,
            weightKg = kg,
            daysAgo = today - dia,
            waistCm = nearestValue(waistByDay, dia),
            bodyFatPct = nearestValue(bodyFatByDay, dia),
        )
    }

    /**
     * O valor medido mais perto desse dia, dentro de três semanas. As medidas de fita não
     * caem no mesmo dia das pesagens, e exigir coincidência exata devolvia quase sempre null.
     */
    fun nearestValue(
        series: List<Pair<Long, Double>>,
        targetDay: Long,
        windowDays: Long = NEAREST_WINDOW_DAYS,
    ): Double? = series
        .filter { abs(it.first - targetDay) <= windowDays }
        .minByOrNull { abs(it.first - targetDay) }
        ?.second

    const val NEAREST_WINDOW_DAYS = 21L
}
