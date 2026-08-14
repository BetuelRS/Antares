package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.exp

object WeightTrend {

    // Abaixo de uma semana o ruído de água e digestão é maior do que a mudança
    // real, e o ritmo semanal deixa de querer dizer alguma coisa.
    const val MIN_SPAN_DAYS = 7

    // Seis semanas. Acima disto a pessoa esteve fora, não emagreceu devagar.
    const val TREND_RESET_GAP_DAYS = 42

    // Abaixo de 100 g por semana o peso não está a mexer, está a oscilar.
    const val STALL_THRESHOLD_KG_WEEK = 0.1

    /**
     * Quantas semanas seguidas, a contar da última pesagem para trás, a tendência
     * ficou parada. É o que distingue, no [AdaptiveTdee], adaptação metabólica de
     * comida por registar.
     */
    fun consecutiveStallWeeks(entries: List<Pair<Long, Double>>): Int {
        if (entries.size < 2) return 0

        // Compara-se a tendência e não os pesos crus: dois dias mal escolhidos
        // dariam qualquer resposta.
        val ema = trendSeries(entries)
        val lastDay = entries.last().first
        var weeks = 0
        while (true) {
            val endDay = lastDay - weeks * 7L
            val startDay = endDay - 7L

            // A pesagem que vale para cada extremo é a última que existe até esse
            // dia — raramente há uma pesagem em cima da fronteira.
            val endIdx = entries.indexOfLast { it.first <= endDay }
            val startIdx = entries.indexOfLast { it.first <= startDay }

            // `startIdx == endIdx` significa que a semana inteira caiu no intervalo
            // entre duas pesagens: não há nada para comparar, e não é uma paragem.
            if (endIdx < 0 || startIdx < 0 || startIdx == endIdx) return weeks

            if (abs(ema[endIdx] - ema[startIdx]) > STALL_THRESHOLD_KG_WEEK) return weeks
            weeks++

            // Travão: quem tem anos de histórico parado não ganha nada em contá-los
            // todos, e o ciclo percorre a lista a cada volta.
            if (weeks > MAX_STALL_WEEKS_COUNTED) return weeks
        }
    }

    private const val MAX_STALL_WEEKS_COUNTED = 12

    /**
     * Ritmo em kg por semana, ou null quando o histórico dentro da janela é curto
     * de mais para a resposta significar alguma coisa.
     */
    fun weeklyRateKg(entries: List<Pair<Long, Double>>, windowDays: Int = 28): Double? {
        if (entries.size < 2) return null

        val ema = trendSeries(entries)
        val lastDay = entries.last().first

        // Se nenhuma pesagem cair dentro da janela, usa-se todo o histórico em vez
        // de devolver nada.
        val startIndex = entries.indexOfFirst { lastDay - it.first <= windowDays }
            .takeIf { it >= 0 } ?: 0

        // O intervalo real entre as pontas, que é menor do que a janela pedida
        // para quem começou a pesar-se há pouco.
        val spanDays = lastDay - entries[startIndex].first
        if (spanDays < MIN_SPAN_DAYS) return null

        return (ema.last() - ema[startIndex]) / spanDays * 7.0
    }

    // Constante de tempo da suavização. Dez dias faz uma pesagem isolada mexer
    // pouco e um mês render-se quase todo ao valor novo.
    const val TAU_DAYS = 10.0

    /**
     * Peso que a pesagem nova tem na tendência, em função dos dias desde a
     * anterior. Sempre entre 0 e 1.
     */
    fun alphaForGap(gapDays: Long): Double {
        // Mesmo dia, ou relógio andado para trás: a pesagem nova substitui em vez
        // de suavizar. Sem isto, duas pesagens no mesmo dia inventavam suavização.
        if (gapDays <= 0L) return 1.0

        return 1.0 - exp(-gapDays.toDouble() / TAU_DAYS)
    }

    /**
     * A tendência ao longo do tempo, um ponto por pesagem e na mesma ordem da
     * lista recebida. É esta série que os gráficos desenham a cheio.
     */
    fun trendSeries(entries: List<Pair<Long, Double>>): List<Double> {
        if (entries.isEmpty()) return emptyList()

        val out = ArrayList<Double>(entries.size)

        // A primeira pesagem é a própria tendência: não há passado para a suavizar.
        var trend = entries.first().second
        out += trend

        for (i in 1 until entries.size) {
            val gap = entries[i].first - entries[i - 1].first

            trend = if (gap >= TREND_RESET_GAP_DAYS) {
                // Ausência longa: recomeça no valor novo em vez de desenhar uma
                // rampa entre dois pesos que nunca se seguiram.
                entries[i].second
            } else {
                val a = alphaForGap(gap)
                a * entries[i].second + (1 - a) * trend
            }
            out += trend
        }
        return out
    }

    fun trendNow(entries: List<Pair<Long, Double>>): Double? =
        trendSeries(entries).lastOrNull()

    fun trendPairs(entries: List<Pair<Long, Double>>): List<Pair<Long, Double>> =
        entries.map { it.first }.zip(trendSeries(entries))
}
