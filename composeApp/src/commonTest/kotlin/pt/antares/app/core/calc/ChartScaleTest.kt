package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartScaleTest {

    @Test
    fun `a escala tem folga acima e abaixo dos dados`() {
        val s = ChartScale.of(listOf(80.0, 85.0))
        assertTrue(s.min < 80.0, "o mínimo tem de ficar abaixo do menor valor")
        assertTrue(s.max > 85.0, "o máximo tem de ficar acima do maior valor")
    }

    @Test
    fun `o menor valor nunca fica colado ao fundo`() {
        val s = ChartScale.of(listOf(80.0, 85.0))
        val f = s.fraction(80.0)
        assertTrue(f > 0.0 && f < 0.5, "o menor valor caiu em $f — devia ter folga")
    }

    @Test
    fun `um valor fora da escala encosta em vez de sair do desenho`() {
        val s = ChartScale.of(listOf(80.0, 85.0))
        assertEquals(0.0, s.fraction(-999.0))
        assertEquals(1.0, s.fraction(999.0))
    }

    @Test
    fun `uma serie constante poe a linha ao meio`() {

        val s = ChartScale.of(listOf(80.0, 80.0, 80.0))
        assertEquals(0.5, s.fraction(80.0), 0.001)
        assertTrue(s.span > 0.0, "uma série constante tem de abrir uma janela")
    }

    @Test
    fun `um unico ponto tambem fica ao meio`() {
        val s = ChartScale.of(listOf(62.5))
        assertEquals(0.5, s.fraction(62.5), 0.001)
    }

    @Test
    fun `uma serie vazia nao rebenta`() {
        val s = ChartScale.of(emptyList())
        assertTrue(s.span > 0.0)
        assertTrue(s.ticks.isNotEmpty())
    }

    @Test
    fun `valores nao finitos sao ignorados`() {
        val s = ChartScale.of(listOf(80.0, Double.NaN, 85.0, Double.POSITIVE_INFINITY))
        assertTrue(s.min.isFinite() && s.max.isFinite())
        assertTrue(s.min < 80.0 && s.max > 85.0)
    }

    @Test
    fun `as linhas da grelha caem dentro da escala`() {
        val s = ChartScale.of(listOf(78.3, 84.7))
        assertTrue(s.ticks.isNotEmpty())
        for (t in s.ticks) {
            assertTrue(t >= s.min && t <= s.max, "linha em $t está fora de [${s.min}, ${s.max}]")
        }
    }

    @Test
    fun `o passo da grelha e sempre 1, 2 ou 5 vezes uma potencia de dez`() {

        val casos = listOf(
            listOf(78.0, 85.0),
            listOf(0.0, 1.0),
            listOf(1200.0, 2600.0),
            listOf(0.02, 0.09),
            listOf(-5.0, 5.0),
        )
        for (valores in casos) {
            val s = ChartScale.of(valores)
            if (s.ticks.size < 2) continue
            val passo = s.ticks[1] - s.ticks[0]
            val magnitude = 10.0.pow(kotlin.math.floor(kotlin.math.log10(passo)))
            val mantissa = passo / magnitude
            assertTrue(
                abs(mantissa - 1) < 0.001 || abs(mantissa - 2) < 0.001 || abs(mantissa - 5) < 0.001,
                "passo $passo (mantissa $mantissa) não é 1, 2 nem 5 × potência de dez, em $valores",
            )
        }
    }

    @Test
    fun `as linhas estao por ordem crescente e sem repetidos`() {
        val s = ChartScale.of(listOf(60.0, 120.0))
        assertEquals(s.ticks.sorted(), s.ticks)
        assertEquals(s.ticks.toSet().size, s.ticks.size)
    }

    @Test
    fun `nunca desenha mais linhas do que se conseguem ler`() {

        val casos = listOf(
            listOf(0.0, 1_000_000.0),
            listOf(0.0, 0.0001),
            listOf(-1_000.0, 1_000.0),
        )
        for (valores in casos) {
            val s = ChartScale.of(valores)
            assertTrue(s.ticks.size <= 12, "${s.ticks.size} linhas em $valores")
        }
    }

    @Test
    fun `varrimento - a escala contem sempre os dados e as fracoes ficam no intervalo`() {
        var vistos = 0
        for (baixo in 0..200 step 7) {
            for (amplitude in 1..60 step 3) {
                val lo = baixo.toDouble() / 2
                val hi = lo + amplitude.toDouble() / 4
                val s = ChartScale.of(listOf(lo, hi))
                assertTrue(s.min <= lo, "escala não contém $lo")
                assertTrue(s.max >= hi, "escala não contém $hi")
                assertTrue(s.fraction(lo) in 0.0..1.0)
                assertTrue(s.fraction(hi) in 0.0..1.0)
                assertTrue(s.fraction(lo) < s.fraction(hi), "a ordem inverteu-se")
                vistos++
            }
        }
        assertTrue(vistos > 500, "o varrimento encolheu: $vistos")
    }
}
