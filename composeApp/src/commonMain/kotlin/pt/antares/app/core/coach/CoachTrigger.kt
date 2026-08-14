package pt.antares.app.core.coach

import pt.antares.app.core.util.weekStartEpochDay

/**
 * Decide de que semana é o relatório e se já há razão para o gerar. Só o quando; o que ele
 * diz é do [CoachRepository].
 */
object CoachTrigger {

    // Quatro dias registados. Abaixo disto o relatório descreveria uma semana que a app
    // quase não viu.
    const val MIN_LOGGED_DAYS = 4

    // Sempre a semana passada, nunca a corrente: uma semana a meio ainda vai mudar, e o
    // relatório é escrito uma vez só.
    fun targetWeekStart(todayEpochDay: Long): Long = weekStartEpochDay(todayEpochDay) - 7

    /**
     * A semana que um pedido manual deve olhar. Se a passada tem poucos registos e a atual
     * já tem, é a atual: quem carrega no botão quer ver alguma coisa, e a semana com dados
     * é a única que tem algo para dizer.
     */
    fun manualWeekStart(
        todayEpochDay: Long,
        loggedDaysPreviousWeek: Int,
        loggedDaysCurrentWeek: Int,
    ): Long {
        val current = weekStartEpochDay(todayEpochDay)
        val previous = current - 7
        return if (loggedDaysPreviousWeek < MIN_LOGGED_DAYS && loggedDaysCurrentWeek >= MIN_LOGGED_DAYS) {
            current
        } else {
            previous
        }
    }

    fun shouldGenerate(
        todayEpochDay: Long,
        lastReportWeekStart: Long?,
        loggedDaysLastWeek: Int,
    ): Boolean {
        if (loggedDaysLastWeek < MIN_LOGGED_DAYS) return false
        val target = targetWeekStart(todayEpochDay)

        // `<` e não `!=`: com um relógio adiantado e depois corrigido, um relatório de uma
        // semana futura não pode fazer a app gerar tudo outra vez.
        return lastReportWeekStart == null || lastReportWeekStart < target
    }
}
