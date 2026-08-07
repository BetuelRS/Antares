package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressRangeTest {

    private val hoje = 20_000L

    private fun serie(vararg dias: Long): List<Pair<Long, Double>> =
        dias.map { it to 80.0 }

    @Test
    fun `trinta dias deixa entrar o limite e corta o que e mais antigo`() {
        val cortada = ProgressRange.DAYS_30.clip(
            serie(hoje - 31, hoje - 30, hoje - 1, hoje),
            hoje,
        )
        assertEquals(listOf(hoje - 30, hoje - 1, hoje), cortada.map { it.first })
    }

    @Test
    fun `tres meses e um ano cortam nos seus limites`() {
        val pontos = serie(hoje - 400, hoje - 200, hoje - 80, hoje)
        assertEquals(2, ProgressRange.MONTHS_3.clip(pontos, hoje).size)
        assertEquals(3, ProgressRange.YEAR.clip(pontos, hoje).size)
    }

    @Test
    fun `sempre nao corta nada`() {
        val pontos = serie(0, 1, hoje - 5000, hoje)
        assertEquals(pontos, ProgressRange.ALL.clip(pontos, hoje))
    }

    @Test
    fun `uma janela sem pesagens fica vazia, e nao inventa um ponto`() {

        assertTrue(ProgressRange.DAYS_30.clip(serie(hoje - 90, hoje - 70), hoje).isEmpty())
    }

    @Test
    fun `o corte e depois da tendencia, nunca antes`() {

        val pesagens = (0..90).map { (hoje - 90 + it) to (90.0 - it * 0.1) }

        val tendenciaInteira = WeightTrend.trendPairs(pesagens)
        val cortadaDepois = ProgressRange.DAYS_30.clip(tendenciaInteira, hoje)
        val cortadaAntes = WeightTrend.trendPairs(ProgressRange.DAYS_30.clip(pesagens, hoje))

        val primeiroCerto = cortadaDepois.first().second
        val primeiroErrado = cortadaAntes.first().second
        val pesoCruNaBorda = ProgressRange.DAYS_30.clip(pesagens, hoje).first().second

        assertEquals(pesoCruNaBorda, primeiroErrado, 1e-9)

        assertTrue(
            primeiroCerto > primeiroErrado + 0.5,
            "cortada depois: $primeiroCerto · recalculada na janela: $primeiroErrado",
        )
    }
}
