package pt.antares.app.core.calc

import kotlin.math.roundToInt

/** Um treino reduzido ao que a comparação compara: quanto tempo, quanto peso, quantas séries. */
data class TreinoComparavel(
    val duracaoMin: Int,
    val volume: Double,
    val series: Int,
)

/**
 * A diferença entre o treino que acabou e um termo de comparação.
 *
 * Guarda a diferença **e o termo de que ela saiu**. O ecrã mostra só a diferença — «+436 kg»
 * —, e o termo fica aqui porque sem ele a média é um número sem prova: é o que deixa afirmar,
 * num teste, que a média de três treinos arredondou os minutos antes de subtrair e não depois.
 */
data class DiferencaDeTreino(
    val referencia: TreinoComparavel,
    val duracaoMin: Int,
    val volume: Double,
    val series: Int,
)

/**
 * O que o resumo pós-treino mostra por baixo dos números do dia.
 *
 * Os dois termos existem porque cada um mente de uma maneira quando está sozinho. A **última
 * vez** é a que se consegue ir ver — «+436 kg» aponta para um treino concreto que está no
 * histórico —, e por isso é a que o esboço 10 desenha; mas um dia mau da última vez faz o de
 * hoje parecer um salto que não houve. A **média das últimas três** não se deixa enganar por
 * um dia, e em troca não aponta para lado nenhum.
 *
 * Decisão do dono a 2026-09-05: mostram-se as duas, a última em destaque e a média por baixo.
 */
data class ComparacaoDeTreino(
    val ultimaVez: DiferencaDeTreino?,
    val media: DiferencaDeTreino?,
)

/**
 * A comparação do resumo pós-treino.
 *
 * **Compara com treinos da mesma rotina, e mais nada.** Um treino livre não tem rotina e por
 * isso não tem termo de comparação — quem chama devolve-lhe a comparação vazia, e o ecrã diz
 * porquê em vez de esconder a secção. Comparar dois treinos livres seria comparar um dia de
 * pernas com um de braços, que é a queixa que a `estudo/areas/10` faz ao volume como métrica
 * única de uma linha de histórico.
 */
object ComparacaoDoTreino {

    /**
     * Quantos treinos entram na média.
     *
     * Três é o que o plano pergunta e o dono escolheu. **A média só aparece com os três**: com
     * dois, escrever «média das últimas três» era mentir sobre o que se somou, e escrever
     * «média das últimas duas» é a mediana de um par — o mesmo que a última vez, com mais uma
     * palavra. Abaixo de três, o ecrã fica só com a última vez.
     */
    const val TREINOS_DA_MEDIA = 3

    /**
     * [anteriores] chega **do mais recente para o mais antigo**, já filtrada à rotina deste
     * treino e já sem ele lá dentro. Ordenar e filtrar é da consulta: aqui não há data nenhuma
     * para desempatar, e uma função que recebesse a lista toda teria de saber o que é uma
     * sessão.
     */
    fun de(hoje: TreinoComparavel, anteriores: List<TreinoComparavel>): ComparacaoDeTreino =
        ComparacaoDeTreino(
            ultimaVez = anteriores.firstOrNull()?.let { diferenca(hoje, it) },
            media = anteriores.take(TREINOS_DA_MEDIA)
                .takeIf { it.size == TREINOS_DA_MEDIA }
                ?.let { diferenca(hoje, media(it)) },
        )

    private fun media(treinos: List<TreinoComparavel>): TreinoComparavel = TreinoComparavel(
        // Os minutos e as séries são contagens inteiras: a média arredonda-se antes de a
        // diferença ser feita, senão «−4 min» sairia de uma subtracção com casas decimais que
        // nunca chegam ao ecrã e o número mostrado não fecharia com os números mostrados.
        duracaoMin = (treinos.sumOf { it.duracaoMin }.toDouble() / treinos.size).roundToInt(),
        volume = treinos.sumOf { it.volume } / treinos.size,
        series = (treinos.sumOf { it.series }.toDouble() / treinos.size).roundToInt(),
    )

    private fun diferenca(hoje: TreinoComparavel, referencia: TreinoComparavel) = DiferencaDeTreino(
        referencia = referencia,
        duracaoMin = hoje.duracaoMin - referencia.duracaoMin,
        volume = hoje.volume - referencia.volume,
        series = hoje.series - referencia.series,
    )
}
