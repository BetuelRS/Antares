package pt.antares.app.core.calc

import kotlinx.datetime.TimeZone
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.toEpochDay

data class FinishedFast(
    val startedAt: Long,
    val endedAt: Long,
    val completed: Boolean,
)

data class FastingStatsResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val completedCount: Int,
    val brokenCount: Int,
    val completionRate: Float,
    val averageDurationMs: Long,
)

object FastingStats {

    /**
     * Estatísticas de jejum. O fuso é obrigatório porque as sequências contam-se em dias
     * locais: um jejum que acaba às 23h de sábado em Lisboa é sábado, não domingo em UTC.
     */
    fun compute(
        sessions: List<FinishedFast>,
        now: Long,
        zone: TimeZone,
    ): FastingStatsResult {
        val completed = sessions.count { it.completed }
        val broken = sessions.size - completed
        val rate = if (sessions.isEmpty()) 0f else completed.toFloat() / sessions.size
        // A média inclui os jejuns interrompidos: interessa quanto tempo a pessoa jejuou
        // de facto, não quanto tempo tencionava. O `coerceAtLeast` protege de relógios
        // acertados para trás a meio de uma sessão.
        val avg = if (sessions.isEmpty()) {
            0L
        } else {
            sessions.sumOf { (it.endedAt - it.startedAt).coerceAtLeast(0L) } / sessions.size
        }

        // A sequência conta-se pelo dia em que o jejum acabou, e num conjunto: dois jejuns
        // no mesmo dia contam uma vez só.
        val completedDays: Set<Long> = sessions
            .asSequence()
            .filter { it.completed }
            .map { epochMillisToLocalDate(it.endedAt, zone).toEpochDay() }
            .toSet()

        val today = epochMillisToLocalDate(now, zone).toEpochDay()
        return FastingStatsResult(
            currentStreak = currentStreak(completedDays, today),
            longestStreak = longestStreak(completedDays),
            completedCount = completed,
            brokenCount = broken,
            completionRate = rate,
            averageDurationMs = avg,
        )
    }

    private fun currentStreak(days: Set<Long>, today: Long): Int {
        // Ontem também serve de âncora: quem ainda não jejuou hoje não perdeu a sequência,
        // porque o dia ainda vai a meio.
        var anchor = when {
            today in days -> today
            (today - 1) in days -> today - 1
            else -> return 0
        }
        var streak = 0
        while (anchor in days) {
            streak++
            anchor--
        }
        return streak
    }

    private fun longestStreak(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        val sorted = days.sorted()
        var best = 1
        var run = 1
        var prev = sorted.first()
        for (d in sorted.drop(1)) {
            run = if (d == prev + 1) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        return best
    }
}
