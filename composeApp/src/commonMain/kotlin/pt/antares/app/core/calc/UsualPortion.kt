package pt.antares.app.core.calc

import kotlin.math.roundToInt

/**
 * A porção habitual de um alimento, para o campo das gramas já vir preenchido. É o valor
 * que a pessoa costuma repetir, não a média — quem come sempre 30 g não quer ver 42.
 */
object UsualPortion {

    // Agrupa-se de 5 em 5 gramas: 28 g e 31 g são a mesma colher pesada em dias diferentes,
    // e sem agrupamento nenhuma quantidade se repetia o suficiente para ganhar.
    const val PASSO_G = 5.0

    const val MINIMO_REGISTOS = 3

    fun of(amounts: List<Double>): Double? {
        val validas = amounts.filter { it > 0.0 }
        if (validas.size < MINIMO_REGISTOS) return null

        val grupos = validas.groupBy { (it / PASSO_G).roundToInt() }

        // Empate resolve-se pelo grupo que apareceu primeiro na lista. Como esta chega por
        // ordem do mais recente, o desempate favorece o hábito atual e não o antigo.
        val vencedor = grupos.entries
            .sortedWith(
                compareByDescending<Map.Entry<Int, List<Double>>> { it.value.size }
                    .thenBy { entrada -> validas.indexOfFirst { (it / PASSO_G).roundToInt() == entrada.key } },
            )
            .first()

        // Devolve-se a mediana das quantidades reais do grupo, e não o centro do intervalo:
        // o valor sugerido é um que a pessoa mesmo registou.
        return mediana(vencedor.value)
    }

    private fun mediana(valores: List<Double>): Double {
        val ordenados = valores.sorted()
        val meio = ordenados.size / 2
        return if (ordenados.size % 2 == 1) {
            ordenados[meio]
        } else {
            (ordenados[meio - 1] + ordenados[meio]) / 2.0
        }
    }

    /**
     * Quanto pesa **uma unidade tua**, quando ela não é a da tabela.
     *
     * A tabela diz que uma fatia de queijo tem 30 g. A tua faca corta 45, e o teu pão é o do
     * teu padeiro. A porção da tabela é a mediana de uma medição feita noutro sítio — e a
     * tua é uma medição do que tu comes, o que a torna melhor para ti.
     *
     * **Só se aprende o que se repete.** Quem registou três vezes o mesmo número tem um
     * hábito; quem registou três números diferentes tem três refeições, e inventar-lhe uma
     * unidade era pôr no ecrã um número que ninguém mediu.
     *
     * Devolve nulo quando não há hábito, quando o alimento não tem porção na tabela, ou
     * quando o hábito **é** o da tabela — nesse caso já está tudo dito, e repetir o mesmo
     * número com outro nome só ocupa espaço.
     */
    fun unidadeDe(amounts: List<Double>, gramasDaTabela: Double?): Double? {
        val tabela = gramasDaTabela?.takeIf { it > 0 } ?: return null
        val habito = of(amounts) ?: return null

        val razao = habito / tabela

        // **Não se divide o hábito por nada.** Uma primeira versão dividia-o pelo número de
        // unidades da tabela que lá cabiam, e um hábito de 45 g com uma fatia de 30 saía
        // como «a tua fatia tem 22,5 g» — meia fatia, que ninguém corta nem come.
        //
        // Acima de duas vezes e meia a pessoa está a comer várias unidades, e aí a tabela já
        // diz quanto pesa uma. Abaixo de quatro décimos não é a mesma comida cortada de
        // outra maneira: é outra coisa, ou um zero a mais que se repetiu.
        if (razao > LIMITE_SUPERIOR || razao < LIMITE_INFERIOR) return null

        // Um hábito a menos de um décimo da tabela é a mesma medida noutra balança. Duas
        // linhas a dizer o mesmo número ocupam a fila de atalhos sem acrescentar nada.
        if (kotlin.math.abs(razao - 1.0) < DIFERENCA_MINIMA) return null

        return habito
    }

    /** Menos de 10 % de diferença é a mesma medida noutra balança. */
    private const val DIFERENCA_MINIMA = 0.1

    // Uma unidade ao dobro ou a metade ainda é a mesma coisa cortada de outra maneira. Fora
    // disto é outra comida, ou várias unidades de uma vez.
    private const val LIMITE_SUPERIOR = 2.5
    private const val LIMITE_INFERIOR = 0.4
}
