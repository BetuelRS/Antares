package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.exp

object WeightTrend {

    const val MIN_SPAN_DAYS = 7

    const val TREND_RESET_GAP_DAYS = 42

    const val STALL_THRESHOLD_KG_WEEK = 0.1

    fun consecutiveStallWeeks(entries: List<Pair<Long, Double>>): Int {
        if (entries.size < 2) return 0
        val ema = trendSeries(entries)
        val lastDay = entries.last().first
        var weeks = 0
        while (true) {
            val endDay = lastDay - weeks * 7L
            val startDay = endDay - 7L
            val endIdx = entries.indexOfLast { it.first <= endDay }
            val startIdx = entries.indexOfLast { it.first <= startDay }

            if (endIdx < 0 || startIdx < 0 || startIdx == endIdx) return weeks
            if (abs(ema[endIdx] - ema[startIdx]) > STALL_THRESHOLD_KG_WEEK) return weeks
            weeks++
            if (weeks > MAX_STALL_WEEKS_COUNTED) return weeks
        }
    }

    private const val MAX_STALL_WEEKS_COUNTED = 12

    fun weeklyRateKg(entries: List<Pair<Long, Double>>, windowDays: Int = 28): Double? {
        if (entries.size < 2) return null

        val ema = trendSeries(entries)
        val lastDay = entries.last().first

        val startIndex = entries.indexOfFirst { lastDay - it.first <= windowDays }
            .takeIf { it >= 0 } ?: 0
        val spanDays = lastDay - entries[startIndex].first
        if (spanDays < MIN_SPAN_DAYS) return null

        return (ema.last() - ema[startIndex]) / spanDays * 7.0
    }

    const val TAU_DAYS = 10.0

    fun alphaForGap(gapDays: Long): Double {
        if (gapDays <= 0L) return 1.0
        return 1.0 - exp(-gapDays.toDouble() / TAU_DAYS)
    }

    fun trendSeries(entries: List<Pair<Long, Double>>): List<Double> {
        if (entries.isEmpty()) return emptyList()
        val out = ArrayList<Double>(entries.size)
        var trend = entries.first().second
        out += trend
        for (i in 1 until entries.size) {
            val gap = entries[i].first - entries[i - 1].first

            trend = if (gap >= TREND_RESET_GAP_DAYS) {
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
