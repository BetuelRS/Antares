package pt.antares.app.core.calc

import pt.antares.app.core.util.weekStartEpochDay

data class StreakResult(
    val current: Int,

    // O dia perdoado mais recente, para o ecrã poder dizer que houve um perdão em vez de
    // apresentar uma sequência que a pessoa sabe não ter cumprido.
    val freezeUsedAtDay: Long?,
)

object LoggingStreak {

    /** Sequência estrita, sem perdões. É a que os marcos e os troféus usam. */
    fun current(loggedDays: Set<Long>, today: Long): Int {
        if (loggedDays.isEmpty()) return 0

        // Ontem serve de âncora tal como hoje: às nove da manhã ainda não se registou nada
        // e a sequência não pode aparecer a zero.
        val anchor = when {
            today in loggedDays -> today
            (today - 1) in loggedDays -> today - 1
            else -> return 0
        }

        var day = anchor
        var count = 0
        while (day in loggedDays) {
            count++
            day--
        }
        return count
    }

    /**
     * A mesma sequência, mas com um dia perdoado por semana de calendário. Um esquecimento
     * não apaga meses de hábito; dois na mesma semana já são o hábito a desfazer-se.
     */
    fun currentWithFreeze(loggedDays: Set<Long>, today: Long): StreakResult {
        if (loggedDays.isEmpty()) return StreakResult(0, null)
        val anchor = when {
            today in loggedDays -> today
            (today - 1) in loggedDays -> today - 1
            else -> return StreakResult(0, null)
        }

        var day = anchor
        var count = 0
        var mostRecentFreeze: Long? = null
        val frozenWeeks = HashSet<Long>()
        while (true) {
            if (day in loggedDays) {
                count++
                day--
            } else {
                // Só se perdoa o buraco de um dia: se o dia anterior também falta, a
                // sequência interrompeu-se mesmo e não há nada a emendar.
                val isolatedGap = (day - 1) in loggedDays
                // O perdão é por semana de calendário e não por contagem, para não se
                // acumular: quem falha à sexta e à segunda gasta dois perdões distintos.
                val week = weekStartEpochDay(day)
                if (isolatedGap && week !in frozenWeeks) {
                    frozenWeeks.add(week)
                    if (mostRecentFreeze == null) mostRecentFreeze = day
                    day--
                } else {
                    break
                }
            }
        }
        return StreakResult(count, mostRecentFreeze)
    }

    fun longest(loggedDays: Set<Long>): Int {
        if (loggedDays.isEmpty()) return 0
        val sorted = loggedDays.toSortedSet()
        var best = 1
        var run = 1
        var previous: Long? = null
        for (day in sorted) {
            if (previous != null && day == previous + 1) {
                run++
            } else if (previous != null) {
                run = 1
            }
            if (run > best) best = run
            previous = day
        }
        return best
    }
}
