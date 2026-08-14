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
}
