package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrendIsTimeAwareTest {

    private val d0 = 20_000L

    @Test
    fun `um dia mexe pouco, um mes mexe muito`() {
        val umDia = WeightTrend.alphaForGap(1)
        val umMes = WeightTrend.alphaForGap(30)
        assertTrue(umDia < 0.15, "um dia devia mexer pouco, mexeu $umDia")
        assertTrue(umMes > 0.90, "um mês devia render-se ao valor novo, deu $umMes")
        assertTrue(umDia < umMes)
    }

    @Test
    fun `o alfa fica sempre entre zero e um`() {
        for (gap in 0..400) {
            val a = WeightTrend.alphaForGap(gap.toLong())
            assertTrue(a in 0.0..1.0, "gap $gap deu alfa $a")
        }
    }

    @Test
    fun `pesagens no mesmo dia nao inventam suavizacao`() {

        assertEquals(1.0, WeightTrend.alphaForGap(0))
    }

    @Test
    fun `quem se pesa uma vez por semana ja NAO tem a tendencia colada ao peso`() {

        val semanais = (0 until 8).map { (d0 + it * 7L) to (90.0 - it) }
        val tendencia = WeightTrend.trendSeries(semanais)
        val ultimoPeso = semanais.last().second
        val ultimaTendencia = tendencia.last()
        assertTrue(
            ultimaTendencia - ultimoPeso > 0.4,
            "a tendência ($ultimaTendencia) está colada ao peso ($ultimoPeso)",
        )
    }

    @Test
    fun `quem se pesa todos os dias tem tendencia suave`() {
        val diarias = (0 until 30).map { (d0 + it) to (90.0 - it * 0.1) }
        val tendencia = WeightTrend.trendSeries(diarias)

        val delta = tendencia.last() - diarias.last().second
        assertTrue(delta > 0.0 && delta < 1.5, "atraso irrealista: $delta")
    }

    @Test
    fun `a mesma perda lida em ritmos diferentes da tendencias parecidas`() {

        val diario = (0..28).map { (d0 + it) to (90.0 - it * 4.0 / 28) }
        val semanal = (0..4).map { (d0 + it * 7L) to (90.0 - it * 1.0) }
        val fimDiario = WeightTrend.trendSeries(diario).last()
        val fimSemanal = WeightTrend.trendSeries(semanal).last()
        assertTrue(
            abs(fimDiario - fimSemanal) < 0.7,
            "leituras muito diferentes: diário $fimDiario vs semanal $fimSemanal",
        )
    }

    @Test
    fun `uma pesagem so da tendencia igual ao peso, e esta certo`() {

        assertEquals(80.0, WeightTrend.trendNow(listOf(d0 to 80.0)))
    }

    @Test
    fun `sem pesagens nao ha tendencia`() {
        assertEquals(null, WeightTrend.trendNow(emptyList()))
        assertEquals(emptyList(), WeightTrend.trendSeries(emptyList()))
    }

    @Test
    fun `uma ausencia longa recomeca em vez de desenhar uma rampa`() {

        val entries = listOf(d0 to 80.0, (d0 + 120) to 90.0)
        assertEquals(90.0, WeightTrend.trendSeries(entries).last())
    }

    @Test
    fun `a serie tem um ponto por pesagem`() {
        val entries = (0 until 12).map { (d0 + it * 3L) to (80.0 + it * 0.1) }
        assertEquals(entries.size, WeightTrend.trendSeries(entries).size)
    }

    @Test
    fun `a tendencia fica sempre entre o minimo e o maximo dos pesos`() {

        val entries = (0 until 40).map { (d0 + it * 2L) to (85.0 + (it % 5) - 2) }
        val pesos = entries.map { it.second }
        for (t in WeightTrend.trendSeries(entries)) {
            assertTrue(t >= pesos.min() - 1e-9 && t <= pesos.max() + 1e-9, "tendência $t fora dos dados")
        }
    }

    @Test
    fun `peso constante da tendencia constante`() {
        val entries = (0 until 20).map { (d0 + it) to 80.0 }
        assertTrue(WeightTrend.trendSeries(entries).all { abs(it - 80.0) < 1e-9 })
    }

    @Test
    fun `varrimento - nenhuma frequencia de pesagem produz tendencia absurda`() {
        var vistos = 0
        for (passo in 1..21) {
            for (declive in listOf(-0.2, -0.05, 0.0, 0.05, 0.2)) {
                val entries = (0 until 20).map { (d0 + it * passo.toLong()) to (85.0 + it * declive) }
                val t = WeightTrend.trendSeries(entries)
                assertEquals(entries.size, t.size)
                assertTrue(t.all { it.isFinite() }, "tendência não finita com passo $passo")
                val pesos = entries.map { it.second }
                assertTrue(
                    t.all { it >= pesos.min() - 1e-9 && it <= pesos.max() + 1e-9 },
                    "tendência fora dos dados com passo $passo, declive $declive",
                )
                vistos++
            }
        }
        assertTrue(vistos > 100, "varrimento encolheu: $vistos")
    }
}
