package pt.antares.app.core.calc

import pt.antares.app.core.util.weekStartEpochDay
import kotlin.math.roundToInt

/**
 * Séries de trabalho por músculo, por semana — e a faixa que diz se são poucas.
 *
 * O volume por músculo, que é o que este ecrã mostrava, responde a outra pergunta: mede
 * peso × repetições, não é comparável entre grupos musculares — um dia de pernas tem sempre
 * mais volume do que um de braços —, e por isso não diz se se está a treinar o suficiente.
 * A contagem de séries diz, e é a métrica que a `estudo/areas/10` pede e o esboço 10 desenha.
 *
 * **Uma série conta inteira para cada músculo primário**, sem repartir. É a mesma convenção
 * do [MuscleVolume], e pela mesma razão escrita lá: os secundários já ficaram de fora, e
 * dividir subavaliava o trabalho de cada um nos compostos. O total da coluna é maior do que o
 * número de séries do período, e isso é esperado.
 */
object SeriesPorMusculo {

    /**
     * A faixa comum na literatura de hipertrofia para crescer: 10 a 20 séries de trabalho por
     * músculo por semana.
     *
     * **É uma orientação e a app di-lo.** Não é um alvo calculado a partir de nada desta
     * pessoa — ao contrário da meta de calorias, que sai do peso, da altura e da idade dela.
     * Fica uma faixa e não uma linha exactamente por isso: uma linha convidava a lê-la como
     * um número que a app apurou.
     *
     * A regra que a 2.22.0 fixou continua a valer — a app não escreve números que não
     * consegue contar. A diferença é o que se está a afirmar: ali eram cento e onze frações
     * inventadas, uma por exercício, sem fonte nenhuma; aqui é **um** intervalo publicado, e
     * a app mostra-o dizendo o que é.
     */
    const val FAIXA_MIN = 10
    const val FAIXA_MAX = 20

    private const val DIAS_POR_SEMANA = 7.0

    /**
     * Quantas séries cada músculo levou no período.
     *
     * Recebe uma lista com os músculos primários **de cada série** — uma entrada por série
     * gravada. Séries sem músculo declarado vão para o balde [MuscleVolume.OTHER], como no
     * volume: sem ele, o trabalho desses exercícios desaparecia do mapa sem nada o dizer.
     */
    fun contar(musculosPorSerie: List<List<String>>): Map<String, Int> {
        val out = mutableMapOf<String, Int>()
        for (musculos in musculosPorSerie) {
            for (m in musculos.ifEmpty { listOf(MuscleVolume.OTHER) }) {
                out[m] = (out[m] ?: 0) + 1
            }
        }
        return out
    }

    /**
     * A média semanal do período, arredondada à unidade — meia série não existe, e escrever
     * «13,4 séries» dava precisão a uma contagem de números inteiros.
     *
     * Devolve `null` quando o período é mais curto do que uma semana: a faixa é semanal, e
     * multiplicar um dia por sete para a alcançar era inventar seis dias que não aconteceram.
     * É a mesma recusa que o resto do motor faz — devolver nada em vez de um número mau.
     */
    fun porSemana(total: Int, diasDoPeriodo: Int): Int? {
        if (diasDoPeriodo < DIAS_POR_SEMANA) return null
        return (total / (diasDoPeriodo / DIAS_POR_SEMANA)).roundToInt()
    }

    // Não há função que diga se um músculo está abaixo, dentro ou acima da faixa, e é de
    // propósito: o ecrã desenha a faixa por trás da barra e deixa a comparação ao olho.
    // Uma classificação em três estados só serviria para pintar a barra de outra cor, que é
    // exactamente o que esta faixa não faz — a cor diz categoria, a forma diz estado.
}

/**
 * Quantos treinos por semana, semana a semana.
 *
 * A semana é a **ISO**, de segunda a domingo, e é a mesma do cartão «Esta semana» do painel
 * de treino, do relatório do treinador e da grelha do progresso. A 2.20.0 escolheu-a e
 * escreveu porquê: é o que faz os três concordarem. Este ecrã contava sete dias para trás a
 * partir de agora, e por isso «semana» queria dizer duas coisas dentro do mesmo separador.
 */
object FrequenciaDeTreino {

    /**
     * As [semanas] mais recentes, da mais antiga para a mais nova, com zero nas que não
     * tiveram treino nenhum.
     *
     * As semanas vazias entram de propósito: um gráfico que só desenha as semanas com treino
     * transforma uma paragem de um mês num traço contínuo, que é o contrário do que ele serve
     * para mostrar.
     */
    fun porSemana(
        iniciosDeTreino: List<Long>,
        hojeEpochDay: Long,
        semanas: Int,
    ): List<Int> {
        if (semanas <= 0) return emptyList()
        val estaSemana = weekStartEpochDay(hojeEpochDay)
        val primeira = estaSemana - (semanas - 1) * DIAS_POR_SEMANA

        val contagem = mutableMapOf<Long, Int>()
        for (dia in iniciosDeTreino) {
            val inicio = weekStartEpochDay(dia)
            if (inicio < primeira || inicio > estaSemana) continue
            contagem[inicio] = (contagem[inicio] ?: 0) + 1
        }
        return (0 until semanas).map { contagem[primeira + it * DIAS_POR_SEMANA] ?: 0 }
    }

    /**
     * A média por semana da série. Uma casa decimal, porque aqui a fração quer dizer alguma
     * coisa: «3,4 treinos por semana» distingue-se de 3 e de 4, e é isso que se compara.
     */
    fun media(porSemana: List<Int>): Double {
        if (porSemana.isEmpty()) return 0.0
        return (porSemana.sum() * UMA_CASA / porSemana.size).roundToInt() / UMA_CASA
    }

    private const val DIAS_POR_SEMANA = 7L

    /** O factor de uma casa decimal: multiplicar, arredondar ao inteiro, e dividir de volta. */
    private const val UMA_CASA = 10.0
}
