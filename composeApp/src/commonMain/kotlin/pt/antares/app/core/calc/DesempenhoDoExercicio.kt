package pt.antares.app.core.calc

/** Uma série de trabalho já feita, com o treino a que pertence e quando ele foi. */
data class SerieFeita(
    val weightKg: Double,
    val reps: Int,
    val sessionId: String,
    val startedAt: Long,
)

/**
 * O que a pessoa já fez neste exercício, para o cartão do detalhe da biblioteca.
 *
 * A `estudo/areas/09-treino-biblioteca.md` aponta isto como o quarto problema da área: o
 * detalhe mostra as instruções e um gráfico, e não diz **melhor série, 1RM estimado, quantas
 * vezes e a última vez** — números que a app calcula noutros ecrãs e que nunca chegam aqui.
 *
 * A melhor série é a de maior **peso × repetições** e não a de maior peso: uma série de 100 kg
 * × 1 e uma de 80 kg × 8 são coisas diferentes, e a segunda é a que descreve o trabalho. É o
 * mesmo critério do `bestWeightReps` do [PrDetector], e é de propósito — dois critérios para
 * «a melhor série» dariam dois números diferentes no mesmo ecrã.
 */
data class DesempenhoDoExercicio(
    val melhorPesoKg: Double,
    val melhorReps: Int,
    val umRmKg: Double?,
    val vezes: Int,
    val ultimaEm: Long,
)

object Desempenho {

    /**
     * Devolve `null` sem séries de trabalho nenhumas — e é `null` e não um cartão a zeros,
     * porque «nunca fizeste isto» e «fizeste isto e deu zero» são coisas diferentes, e a
     * segunda não acontece.
     *
     * As vezes contam **treinos distintos** e não séries: cinco séries de supino num treino
     * são uma vez que se fez supino.
     */
    fun de(series: List<SerieFeita>): DesempenhoDoExercicio? {
        val validas = series.filter { it.weightKg > 0.0 && it.reps > 0 }
        if (validas.isEmpty()) return null

        // O desempate pelo peso importa: 60 × 10 e 100 × 6 dão os mesmos 600, e a série que
        // se quer ver é a mais pesada das duas.
        val melhor = validas.maxWith(
            compareBy({ it.weightKg * it.reps }, { it.weightKg }),
        )

        return DesempenhoDoExercicio(
            melhorPesoKg = melhor.weightKg,
            melhorReps = melhor.reps,
            umRmKg = validas.mapNotNull { OneRepMax.epley(it.weightKg, it.reps) }.maxOrNull(),
            vezes = validas.map { it.sessionId }.toSet().size,
            ultimaEm = validas.maxOf { it.startedAt },
        )
    }
}
