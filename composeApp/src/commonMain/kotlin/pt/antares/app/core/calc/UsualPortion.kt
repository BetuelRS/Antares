package pt.antares.app.core.calc

import kotlin.math.roundToInt

object UsualPortion {

    const val PASSO_G = 5.0

    const val MINIMO_REGISTOS = 3

    fun of(amounts: List<Double>): Double? {
        val validas = amounts.filter { it > 0.0 }
        if (validas.size < MINIMO_REGISTOS) return null

        val grupos = validas.groupBy { (it / PASSO_G).roundToInt() }

        val vencedor = grupos.entries
            .sortedWith(
                compareByDescending<Map.Entry<Int, List<Double>>> { it.value.size }
                    .thenBy { entrada -> validas.indexOfFirst { (it / PASSO_G).roundToInt() == entrada.key } },
            )
            .first()

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
