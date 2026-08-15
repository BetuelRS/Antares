package pt.antares.app.core.nutrition

/**
 * O micronutriente que mais falta ao dia. Um só, e não uma lista: quem vê doze lacunas não
 * corrige nenhuma.
 */
data class DailyGap(
    val key: String,
    val consumed: Double,
    val reference: Double,
) {
    val missing: Double get() = (reference - consumed).coerceAtLeast(0.0)

    val fraction: Double get() = if (reference > 0) (consumed / reference).coerceIn(0.0, 1.0) else 1.0

    companion object {

        // Só se avisa abaixo de metade da referência. Acima disso ainda é uma falha normal
        // de um dia, e um aviso diário deixaria de ser lido.
        const val LIMIAR = 0.5

        /**
         * `minMeasuredFraction` é a defesa contra o falso alarme: um nutriente só é
         * candidato se pelo menos metade das calorias do dia tivessem análise para ele.
         * Sem isto, a lacuna maior era sempre a do nutriente que menos alimentos declaram.
         */
        fun worst(
            totals: MicroTotals,
            referenceFor: (String) -> Double?,
            minMeasuredFraction: Double = 0.5,
        ): DailyGap? {
            if (totals.totalKcal <= 0) return null

            return totals.byKey.keys
                .asSequence()
                .filter { it in Nutrients.VITAMINS || it in Nutrients.MINERALS }
                .filter { it !in Nutrients.TETOS }
                .mapNotNull { chave ->
                    val referencia = referenceFor(chave)?.takeIf { it > 0 } ?: return@mapNotNull null

                    val medido = totals.measuredKcalByKey[chave] ?: 0.0
                    if (medido / totals.totalKcal < minMeasuredFraction) return@mapNotNull null
                    DailyGap(chave, totals.byKey[chave] ?: 0.0, referencia)
                }
                .filter { it.fraction < LIMIAR }
                // A fração e não a quantidade em falta: as unidades são incomparáveis entre
                // nutrientes, e miligramas nunca se medem contra microgramas.
                .minByOrNull { it.fraction }
        }
    }
}
