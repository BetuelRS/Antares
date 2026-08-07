package pt.antares.app.core.nutrition

data class DailyGap(
    val key: String,
    val consumed: Double,
    val reference: Double,
) {
    val missing: Double get() = (reference - consumed).coerceAtLeast(0.0)

    val fraction: Double get() = if (reference > 0) (consumed / reference).coerceIn(0.0, 1.0) else 1.0

    companion object {

        const val LIMIAR = 0.5

        fun worst(
            totals: MicroTotals,
            referenceFor: (String) -> Double?,
            minMeasuredFraction: Double = 0.5,
        ): DailyGap? {
            if (totals.totalKcal <= 0) return null

            return totals.byKey.keys
                .asSequence()
                .filter { it in Nutrients.VITAMINS || it in Nutrients.MINERALS }
                .filter { it != Nutrients.SODIUM }
                .mapNotNull { chave ->
                    val referencia = referenceFor(chave)?.takeIf { it > 0 } ?: return@mapNotNull null

                    val medido = totals.measuredKcalByKey[chave] ?: 0.0
                    if (medido / totals.totalKcal < minMeasuredFraction) return@mapNotNull null
                    DailyGap(chave, totals.byKey[chave] ?: 0.0, referencia)
                }
                .filter { it.fraction < LIMIAR }
                .minByOrNull { it.fraction }
        }
    }
}
