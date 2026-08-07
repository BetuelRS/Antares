package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeightTrendTest {

    @Test
    fun `vazio devolve null`() {
        assertNull(WeightTrend.trendNow(emptyList()))
    }

    @Test
    fun `uma entrada devolve a propria`() {
        assertEquals(80.0, WeightTrend.trendNow(listOf(0L to 80.0)))
    }

    @Test
    fun `serie tem o mesmo tamanho da entrada`() {
        val series = WeightTrend.trendSeries(
            listOf(0L to 80.0, 1L to 79.5, 2L to 79.8, 3L to 79.2),
        )
        assertEquals(4, series.size)
        assertEquals(80.0, series.first())
    }

    @Test
    fun `a tendencia amortece um salto de dois quilos num dia`() {

        val trend = WeightTrend.trendNow(listOf(0L to 80.0, 1L to 82.0))!!
        assertTrue(trend > 80.0 && trend < 80.4, "amorteceu de menos: $trend")
    }

    private fun dailyDecline(days: Int, start: Double = 85.0, perDay: Double): List<Pair<Long, Double>> =
        (0 until days).map { it.toLong() to start - perDay * it }

    @Test
    fun `ritmo de meio quilo por semana e reconhecido`() {
        val entries = dailyDecline(days = 60, perDay = 0.5 / 7)
        val rate = WeightTrend.weeklyRateKg(entries)!!
        assertTrue(abs(rate + 0.5) < 0.05, "esperava ≈ −0,5 kg/semana, deu $rate")
    }

    @Test
    fun `o ruido diario nao vira o ritmo do avesso`() {

        val entries = dailyDecline(days = 60, perDay = 0.5 / 7)
            .mapIndexed { i, (day, w) -> day to w + if (i % 2 == 0) 0.8 else -0.8 }
        val rate = WeightTrend.weeklyRateKg(entries)!!
        assertTrue(abs(rate + 0.5) < 0.2, "esperava perto de −0,5, deu $rate")
    }

    @Test
    fun `historico curto nao tem ritmo`() {

        assertNull(WeightTrend.weeklyRateKg(dailyDecline(days = 4, perDay = 0.1)))
        assertNull(WeightTrend.weeklyRateKg(listOf(0L to 80.0)))
        assertNull(WeightTrend.weeklyRateKg(emptyList()))
    }

    @Test
    fun `ganhar peso da ritmo positivo`() {
        val entries = dailyDecline(days = 60, perDay = -0.25 / 7)
        assertTrue(WeightTrend.weeklyRateKg(entries)!! > 0)
    }

    @Test
    fun `a janela limita o que conta`() {

        val fast = (0 until 30).map { it.toLong() to 90.0 - 0.2 * it }
        val flat = (30 until 60).map { it.toLong() to 84.0 }
        val rate = WeightTrend.weeklyRateKg(fast + flat, windowDays = 21)!!

        assertTrue(abs(rate) < 0.3, "devia estar quase parado, deu $rate")
    }

    @Test
    fun `um plateau longo acaba por ler zero`() {

        val fast = (0 until 30).map { it.toLong() to 90.0 - 0.2 * it }
        val flat = (30 until 120).map { it.toLong() to 84.0 }
        val rate = WeightTrend.weeklyRateKg(fast + flat, windowDays = 21)!!
        assertTrue(abs(rate) < 0.01, "devia estar parado, deu $rate")
    }

    @Test
    fun `o ritmo mede a mesma linha que o grafico desenha`() {

        val entries = dailyDecline(days = 30, perDay = 0.1)
        val serie = WeightTrend.trendSeries(entries)
        val spanDays = entries.last().first - entries.first().first
        val esperado = (serie.last() - serie.first()) / spanDays * 7.0
        val rate = WeightTrend.weeklyRateKg(entries, windowDays = 999)!!
        assertTrue(abs(rate - esperado) < 1e-9, "ritmo $rate ≠ linha $esperado")
    }

    @Test
    fun `depois de tres meses a tendencia recomeca`() {

        val antes = (0L..29L).map { it to 90.0 }
        val depois = listOf(120L to 75.0)
        assertEquals(75.0, WeightTrend.trendNow(antes + depois)!!, 0.0001)
    }

    @Test
    fun `um buraco curto nao reinicia nada`() {

        val trend = WeightTrend.trendNow(listOf(0L to 80.0, 7L to 79.0))!!
        assertTrue(trend > 79.0 && trend < 80.0, "devia amortecer, deu $trend")
    }

    @Test
    fun `a fronteira do reinicio e a constante documentada`() {
        val gap = WeightTrend.TREND_RESET_GAP_DAYS
        val naFronteira = listOf(0L to 90.0, gap.toLong() to 75.0)
        val mesmoAntes = listOf(0L to 90.0, (gap - 1).toLong() to 75.0)
        assertEquals(75.0, WeightTrend.trendNow(naFronteira)!!, 0.0001)
        assertTrue(WeightTrend.trendNow(mesmoAntes)!! > 75.0)
    }

    @Test
    fun `a serie com buracos tem o mesmo tamanho da entrada`() {
        val entries = listOf(0L to 80.0, 1L to 79.0, 100L to 70.0)
        assertEquals(3, WeightTrend.trendSeries(entries).size)
        assertEquals(70.0, WeightTrend.trendSeries(entries).last(), 0.0001)
    }

    @Test
    fun `os pares da tendencia trazem os dias das pesagens`() {
        val entries = listOf(10L to 80.0, 17L to 79.0, 24L to 78.0)
        val pares = WeightTrend.trendPairs(entries)
        assertEquals(entries.map { it.first }, pares.map { it.first })
        assertEquals(WeightTrend.trendSeries(entries), pares.map { it.second })
    }

    @Test
    fun `sem pesagens nao ha pares`() {
        assertEquals(emptyList(), WeightTrend.trendPairs(emptyList()))
    }
}
