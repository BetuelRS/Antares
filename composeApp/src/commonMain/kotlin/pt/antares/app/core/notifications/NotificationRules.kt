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
     * Quantos mililitros faltam para a meta de água, ou `null` quando não há nada a dizer.
     *
     * Devolve `null` com a meta cumprida — avisar quem já bebeu o que devia é o caminho mais
     * curto para o lembrete ser desligado. E devolve `null` quando falta muito pouco: um
     * aviso por meio copo trata a pessoa como se ela não soubesse beber água.
     */
    fun waterGapToNotify(consumedMl: Int, goalMl: Int): Int? {
        if (goalMl <= 0) return null
        val falta = goalMl - consumedMl
        return falta.takeIf { it >= MIN_WATER_GAP_ML }
    }

    // Um copo. Abaixo disto o aviso custa mais do que vale.
    const val MIN_WATER_GAP_ML = 250

    /**
     * Se já passou o intervalo escolhido desde o último aviso de água.
     *
     * O trabalho periódico corre ao ritmo mais curto que a app oferece e é isto que o
     * filtra — o período de um trabalho periódico fixa-se ao ser posto na fila, e sem este
     * filtro mudar o intervalo nas definições não mudava nada.
     */
    fun waterIntervalElapsed(nowMs: Long, lastNotifiedMs: Long, intervalHours: Int): Boolean {
        if (lastNotifiedMs <= 0L) return true
        // Relógio acertado para trás: o último aviso fica no futuro, e sem isto ninguém
        // voltava a ser avisado até o tempo o alcançar.
        if (nowMs < lastNotifiedMs) return true
        return nowMs - lastNotifiedMs >= intervalHours * MS_PER_HOUR
    }

    private const val MS_PER_HOUR = 3_600_000L

    /**
     * Adia para o fim do silêncio em vez de cancelar: um lembrete de refeição perdido é um
     * dia por registar, e às oito da manhã ainda serve.
     */
    fun nextAllowedMinute(nowMin: Int, quietStartMin: Int, quietEndMin: Int, quietEnabled: Boolean): Int {
        if (!quietEnabled) return nowMin
        return if (isQuiet(nowMin, quietStartMin, quietEndMin)) quietEndMin else nowMin
    }
}
