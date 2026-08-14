package pt.antares.app.core.calc

/**
 * As calorias vistas à semana em vez de ao dia. Um jantar fora deixa de ser um dia
 * falhado e passa a ser uma parcela de um orçamento que ainda dá para gerir.
 */
data class WeeklyBudget(

    val targetPerDay: Int,

    // Dia da semana em curso, de 1 a 7: hoje conta como decorrido.
    val daysElapsed: Int,

    val loggedDays: Int,

    val consumed: Int,
) {
    val weeklyTarget: Int get() = targetPerDay * 7

    // Pode vir negativo, e é isso que se quer mostrar: quem já passou o orçamento precisa
    // de ver quanto, e não um zero que esconde a dívida.
    val remaining: Int get() = weeklyTarget - consumed

    val daysAfterToday: Int get() = (7 - daysElapsed).coerceAtLeast(0)

    // Reparte-se pelos dias que ainda vêm, sem contar hoje: o dia de hoje já está a ser
    // vivido, e incluí-lo dava uma margem que a pessoa não tem para o resto da semana.
    // Null no domingo, quando já não há por onde repartir.
    val perDayLeft: Int?
        get() = if (daysAfterToday > 0) remaining / daysAfterToday else null

    // Só com todos os dias decorridos registados é que o orçamento descreve a semana; caso
    // contrário há calorias comidas fora da conta e a folga é falsa.
    val complete: Boolean get() = loggedDays >= daysElapsed

    companion object {

        /** `isoDayOfWeek` é 1 na segunda e 7 no domingo, como o calendário ISO. */
        fun of(
            targetPerDay: Int,
            isoDayOfWeek: Int,
            loggedDays: Int,
            consumed: Int,
        ): WeeklyBudget = WeeklyBudget(
            targetPerDay = targetPerDay,
            daysElapsed = isoDayOfWeek.coerceIn(1, 7),
            loggedDays = loggedDays.coerceAtLeast(0),
            consumed = consumed.coerceAtLeast(0),
        )
    }
}
