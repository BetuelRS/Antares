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

    fun fraction(value: Double): Double {
        if (span <= 0.0) return 0.5
        return ((value - min) / span).coerceIn(0.0, 1.0)
    }

    companion object {

        const val TARGET_TICKS = 4

        const val PADDING_FRACTION = 0.12

        fun of(values: List<Double>): ChartScale {
            val vivos = values.filter { it.isFinite() }
            if (vivos.isEmpty()) return ChartScale(0.0, 1.0, listOf(0.0, 1.0))

            val dataMin = vivos.min()
            val dataMax = vivos.max()

            if (dataMax - dataMin < FLAT_EPSILON) {

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

        fun niceTicks(lo: Double, hi: Double): List<Double> {
            val span = hi - lo
            if (span <= 0.0) return listOf(lo)
            val cru = span / TARGET_TICKS
            val magnitude = 10.0.pow(floor(log10(cru)))
            val passo = when {
                cru / magnitude >= 5 -> 5 * magnitude
                cru / magnitude >= 2 -> 2 * magnitude
                else -> magnitude
            }
            val primeiro = ceil(lo / passo) * passo
            val out = mutableListOf<Double>()
            var t = primeiro

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
