package pt.antares.app.core.calc

import pt.antares.app.core.util.MINUTES_PER_DAY

/**
 * A janela alimentar de um dia: da primeira à última refeição com hora.
 *
 * É o número que uma app de jejum devia dar e quase nenhuma dá — as outras medem o jejum
 * que a pessoa **declara**, com um botão que se carrega e se esquece; esta mede o que ela
 * **regista ter comido**. Quando os dois discordam, é este que está certo.
 */
data class Janela(
    val primeiraMin: Int,
    val ultimaMin: Int,

    // Quantos registos do dia não tinham hora. Vai para o ecrã porque uma janela calculada
    // sobre metade do dia não é a janela do dia, e quem a lê tem de saber isso.
    val semHora: Int,
) {
    val duracaoMin: Int get() = ultimaMin - primeiraMin

    /** O resto do dia. É o jejum de facto, mesmo em quem nunca carregou no botão. */
    val jejumMin: Int get() = MINUTES_PER_DAY - duracaoMin
}

object EatingWindow {

    /**
     * Duas refeições. Com uma só não há janela nenhuma — há um instante —, e devolver zero
     * dizia à pessoa que comeu tudo num segundo.
     */
    const val MIN_REFEICOES = 2

    /**
     * A janela do dia, ou `null` se não houver horas que cheguem.
     *
     * Recebe as horas tal como estão na base, nulos incluídos, porque é a contagem dos
     * nulos que diz se o resultado é de confiar.
     */
    fun doDia(horas: List<Int?>): Janela? {
        val comHora = horas.filterNotNull().sorted()
        if (comHora.size < MIN_REFEICOES) return null
        return Janela(
            primeiraMin = comHora.first(),
            ultimaMin = comHora.last(),
            semHora = horas.size - comHora.size,
        )
    }

    /**
     * A janela típica de vários dias, pela **mediana** de cada extremo e não pela média:
     * um jantar de aniversário às duas da manhã puxaria a média uma hora para a frente e
     * ficaria a descrever um hábito que não existe.
     *
     * Só entram dias com janela própria, e é preciso [MIN_DIAS] deles para haver hábito.
     */
    fun tipica(porDia: List<List<Int?>>): Janela? {
        val janelas = porDia.mapNotNull { doDia(it) }
        if (janelas.size < MIN_DIAS) return null
        return Janela(
            primeiraMin = mediana(janelas.map { it.primeiraMin }),
            ultimaMin = mediana(janelas.map { it.ultimaMin }),
            semHora = janelas.sumOf { it.semHora },
        )
    }

    // Uma semana. Abaixo disto qualquer janela é a de uns dias, não um hábito.
    const val MIN_DIAS = 7

    private fun mediana(valores: List<Int>): Int {
        val ordenados = valores.sorted()
        val meio = ordenados.size / 2
        return if (ordenados.size % 2 == 1) {
            ordenados[meio]
        } else {
            (ordenados[meio - 1] + ordenados[meio]) / 2
        }
    }
}

