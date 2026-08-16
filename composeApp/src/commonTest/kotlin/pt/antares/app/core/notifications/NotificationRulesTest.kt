package pt.antares.app.core.notifications

import pt.antares.app.core.model.MealSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationRulesTest {

    private fun h(hour: Int, min: Int = 0) = hour * 60 + min

    @Test
    fun `quiet hours 22-08 silencia a noite e deixa passar o dia`() {
        val start = h(22)
        val end = h(8)

        assertTrue(NotificationRules.isQuiet(h(23), start, end))
        assertTrue(NotificationRules.isQuiet(h(0), start, end))
        assertTrue(NotificationRules.isQuiet(h(7, 59), start, end))

        assertFalse(NotificationRules.isQuiet(h(8), start, end))
        assertFalse(NotificationRules.isQuiet(h(13), start, end))
        assertFalse(NotificationRules.isQuiet(h(21, 59), start, end))
    }

    @Test
    fun `janela normal (nao atravessa a meia-noite) tambem funciona`() {

        assertTrue(NotificationRules.isQuiet(h(13, 30), h(13), h(14)))
        assertFalse(NotificationRules.isQuiet(h(12), h(13), h(14)))
        assertFalse(NotificationRules.isQuiet(h(14), h(13), h(14)))
    }

    @Test
    fun `janela vazia (inicio igual ao fim) nunca silencia`() {
        assertFalse(NotificationRules.isQuiet(h(3), h(9), h(9)))
    }

    @Test
    fun `notificacao no silencio e empurrada para o fim`() {

        assertEquals(h(8), NotificationRules.nextAllowedMinute(h(23), h(22), h(8), quietEnabled = true))
    }

    @Test
    fun `notificacao fora do silencio mantem a hora`() {
        assertEquals(h(13), NotificationRules.nextAllowedMinute(h(13), h(22), h(8), quietEnabled = true))
    }

    @Test
    fun `com quiet hours desligado nunca empurra`() {
        assertEquals(h(23), NotificationRules.nextAllowedMinute(h(23), h(22), h(8), quietEnabled = false))
    }

    @Test
    fun `lembra a refeicao que ainda nao foi registada`() {
        assertTrue(
            NotificationRules.shouldRemindMeal(MealSlot.LUNCH, loggedSlots = setOf(MealSlot.BREAKFAST), enabled = true),
        )
    }

    @Test
    fun `nao lembra o que ja foi registado`() {
        assertFalse(
            NotificationRules.shouldRemindMeal(MealSlot.LUNCH, loggedSlots = setOf(MealSlot.LUNCH), enabled = true),
        )
    }

    @Test
    fun `toggle desligado nunca lembra`() {
        assertFalse(
            NotificationRules.shouldRemindMeal(MealSlot.DINNER, loggedSlots = emptySet(), enabled = false),
        )
    }

    @Test
    fun `a pesagem so avisa no dia escolhido`() {
        // Segunda às nove, e é terça.
        assertFalse(
            NotificationRules.shouldRemindWeighIn(
                hojeIso = 2, agoraMin = h(9), diaEscolhidoIso = 1, horaEscolhidaMin = h(9),
                pesouHoje = false, jaAvisadoHoje = false,
            ),
        )
        assertTrue(
            NotificationRules.shouldRemindWeighIn(
                hojeIso = 1, agoraMin = h(9), diaEscolhidoIso = 1, horaEscolhidaMin = h(9),
                pesouHoje = false, jaAvisadoHoje = false,
            ),
        )
    }

    @Test
    fun `antes da hora nao avisa, depois dela sim`() {
        fun em(minuto: Int) = NotificationRules.shouldRemindWeighIn(
            hojeIso = 1, agoraMin = minuto, diaEscolhidoIso = 1, horaEscolhidaMin = h(9),
            pesouHoje = false, jaAvisadoHoje = false,
        )

        assertFalse(em(h(8, 59)))
        // Depois da hora continua a valer: o trabalho periódico chega perto da hora e não à
        // hora, e recusar por dez minutos era perder o aviso da semana.
        assertTrue(em(h(9)))
        assertTrue(em(h(14)))
    }

    @Test
    fun `quem ja se pesou hoje nao e chateado`() {
        assertFalse(
            NotificationRules.shouldRemindWeighIn(
                hojeIso = 1, agoraMin = h(10), diaEscolhidoIso = 1, horaEscolhidaMin = h(9),
                pesouHoje = true, jaAvisadoHoje = false,
            ),
        )
    }

    @Test
    fun `o aviso da pesagem sai uma vez por dia`() {
        assertFalse(
            NotificationRules.shouldRemindWeighIn(
                hojeIso = 1, agoraMin = h(15), diaEscolhidoIso = 1, horaEscolhidaMin = h(9),
                pesouHoje = false, jaAvisadoHoje = true,
            ),
            "o trabalho corre de três em três horas: sem isto seriam cinco avisos no dia",
        )
    }

    @Test
    fun `a agua avisa com o que falta`() {
        assertEquals(1500, NotificationRules.waterGapToNotify(consumedMl = 1000, goalMl = 2500))
    }

    @Test
    fun `com a meta cumprida nao ha nada a dizer`() {
        assertNull(
            NotificationRules.waterGapToNotify(consumedMl = 2500, goalMl = 2500),
            "avisar quem já bebeu o que devia é o caminho mais curto para desligar o lembrete",
        )
        assertNull(NotificationRules.waterGapToNotify(consumedMl = 3000, goalMl = 2500))
    }

    @Test
    fun `nao avisa por meio copo`() {
        assertNull(
            NotificationRules.waterGapToNotify(consumedMl = 2400, goalMl = 2500),
            "cem mililitros não justificam interromper ninguém",
        )
        assertEquals(250, NotificationRules.waterGapToNotify(consumedMl = 2250, goalMl = 2500))
    }

    @Test
    fun `sem meta nao ha aviso`() {
        assertNull(NotificationRules.waterGapToNotify(consumedMl = 0, goalMl = 0))
    }

    @Test
    fun `o intervalo escolhido e o que manda`() {
        val agora = 1_000_000_000L
        val umaHora = 3_600_000L

        assertFalse(
            NotificationRules.waterIntervalElapsed(agora, agora - 2 * umaHora, intervalHours = 3),
            "duas horas depois, com três escolhidas, ainda não é altura",
        )
        assertTrue(NotificationRules.waterIntervalElapsed(agora, agora - 3 * umaHora, intervalHours = 3))
    }

    @Test
    fun `o primeiro aviso do dia nao espera por nada`() {
        assertTrue(NotificationRules.waterIntervalElapsed(1_000L, lastNotifiedMs = 0L, intervalHours = 6))
    }

    @Test
    fun `um relogio acertado para tras nao cala a app para sempre`() {
        // O último aviso ficou no futuro. Sem isto, ninguém voltava a ser avisado até o
        // tempo o alcançar — que pode ser meses.
        assertTrue(
            NotificationRules.waterIntervalElapsed(1_000L, lastNotifiedMs = 9_000_000L, intervalHours = 2),
        )
    }
}
