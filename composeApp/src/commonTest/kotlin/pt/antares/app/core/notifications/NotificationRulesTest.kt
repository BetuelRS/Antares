package pt.antares.app.core.notifications

import pt.antares.app.core.model.MealSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
