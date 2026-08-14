package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

data class ChartScale(
    val min: Double,
    val max: Double,

    val ticks: List<Double>,
) {
    val span: Double get() = max - min

    /** Posição do valor entre 0 e 1, pronta a multiplicar pela altura do gráfico. */
    fun fraction(value: Double): Double {
        // Escala degenerada desenha tudo a meio, em vez de dividir por zero.
        if (span <= 0.0) return 0.5
        return ((value - min) / span).coerceIn(0.0, 1.0)
    }

    companion object {

        const val TARGET_TICKS = 4

        // 12% de folga em cima e em baixo para os pontos extremos não ficarem colados à
        // moldura, onde o traço fica meio cortado.
        const val PADDING_FRACTION = 0.12

        /**
         * Escala a partir dos dados, nunca a partir do zero: numa série de pesos entre 80
         * e 82 kg, começar em zero espalmava a linha até ela deixar de dizer nada.
         */
        fun of(values: List<Double>): ChartScale {
            // NaN e infinito vêm de divisões em séries vazias; um deles contamina o mínimo
            // e o máximo e a escala inteira deixa de existir.
            val vivos = values.filter { it.isFinite() }
            if (vivos.isEmpty()) return ChartScale(0.0, 1.0, listOf(0.0, 1.0))

            val dataMin = vivos.min()
            val dataMax = vivos.max()

            // Série constante — um só ponto, ou o mesmo peso repetido. A folga proporcional
            // daria zero e o gráfico ficava sem altura nenhuma.
            if (dataMax - dataMin < FLAT_EPSILON) {

                // A janela abre 2% do valor, com um mínimo em unidades absolutas para que
                // séries perto de zero também tenham escala.
                val meia = maxOf(abs(dataMin) * FLAT_WINDOW_FRACTION, FLAT_WINDOW_MIN)
                val lo = dataMin - meia
                val hi = dataMax + meia
                return ChartScale(lo, hi, niceTicks(lo, hi))
            }

            val folga = (dataMax - dataMin) * PADDING_FRACTION
            val lo = dataMin - folga
            val hi = dataMax + folga
            return ChartScale(lo, hi, niceTicks(lo, hi))
        }

        /**
         * Marcas em números redondos — 1, 2 ou 5 vezes uma potência de dez — para o eixo
         * mostrar 80, 82, 84 e não 80,3, 82,7. O número de marcas é aproximado.
         */
        fun niceTicks(lo: Double, hi: Double): List<Double> {
            val span = hi - lo
            if (span <= 0.0) return listOf(lo)
            val cru = span / TARGET_TICKS
            // A potência de dez logo abaixo do passo cru; a razão que sobra escolhe entre
            // 1, 2 e 5, os únicos passos que a leitura humana trata como redondos.
            val magnitude = 10.0.pow(floor(log10(cru)))
            val passo = when {
                cru / magnitude >= 5 -> 5 * magnitude
                cru / magnitude >= 2 -> 2 * magnitude
                else -> magnitude
            }
            val primeiro = ceil(lo / passo) * passo
            val out = mutableListOf<Double>()
            var t = primeiro

            // A tolerância no topo apanha a marca que calha exatamente no máximo e que a
            // soma repetida deixa uns bits acima. O `MAX_TICKS` trava o ciclo se o passo
            // vier a zero por causa de valores extremos.
            while (t <= hi + passo * 1e-9 && out.size < MAX_TICKS) {
                out += t
                t += passo
            }
            return out
        }

        private const val FLAT_EPSILON = 1e-9
        private const val FLAT_WINDOW_FRACTION = 0.02
        private const val FLAT_WINDOW_MIN = 0.5
        private const val MAX_TICKS = 12
    }
}
