package pt.antares.app.core.notifications

import pt.antares.app.core.model.MealSlot

/**
 * Quando é que a app pode interromper. As horas contam-se em minutos desde a meia-noite —
 * um inteiro compara-se sem fusos nem formatos.
 */
object NotificationRules {

    // Das dez da noite às oito da manhã.
    const val DEFAULT_QUIET_START_MIN = 22 * 60
    const val DEFAULT_QUIET_END_MIN = 8 * 60

    fun isQuiet(nowMin: Int, startMin: Int, endMin: Int): Boolean {
        // Início igual ao fim é janela de duração zero, e não de vinte e quatro horas.
        if (startMin == endMin) return false
        return if (startMin < endMin) {
            nowMin in startMin until endMin
        } else {
            // A janela normal atravessa a meia-noite, e por isso o intervalo parte-se em
            // dois: do início até ao fim do dia, e do princípio do dia até ao fim.
            nowMin >= startMin || nowMin < endMin
        }
    }

    fun shouldRemindMeal(
        slot: MealSlot,
        loggedSlots: Set<MealSlot>,
        enabled: Boolean,
    ): Boolean = enabled && slot !in loggedSlots

    /**
     * Adia para o fim do silêncio em vez de cancelar: um lembrete de refeição perdido é um
     * dia por registar, e às oito da manhã ainda serve.
     */
    fun nextAllowedMinute(nowMin: Int, quietStartMin: Int, quietEndMin: Int, quietEnabled: Boolean): Int {
        if (!quietEnabled) return nowMin
        return if (isQuiet(nowMin, quietStartMin, quietEndMin)) quietEndMin else nowMin
    }
}
