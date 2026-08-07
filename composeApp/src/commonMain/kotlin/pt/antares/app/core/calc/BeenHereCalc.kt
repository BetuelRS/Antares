package pt.antares.app.core.calc

import kotlin.math.abs

object BeenHereCalc {

    const val SAME_WEIGHT_TOLERANCE_KG = 0.5

    const val MIN_DAYS_APART = 60

    data class Visit(
        val epochDay: Long,
        val weightKg: Double,
        val daysAgo: Long,
        val waistCm: Double? = null,
        val bodyFatPct: Double? = null,
    ) {

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
