package pt.antares.app.core.util

import pt.antares.app.core.model.MealSlot
import kotlin.test.Test
import kotlin.test.assertEquals

class MealSlotAtHourTest {

    @Test
    fun `de manha e pequeno-almoco`() {
        assertEquals(MealSlot.BREAKFAST, MealSlot.atHour(5))
        assertEquals(MealSlot.BREAKFAST, MealSlot.atHour(8))
        assertEquals(MealSlot.BREAKFAST, MealSlot.atHour(10))
    }

    @Test
    fun `o almoco vai ate as quinze`() {
        assertEquals(MealSlot.LUNCH, MealSlot.atHour(11))
        assertEquals(MealSlot.LUNCH, MealSlot.atHour(13))

        assertEquals(MealSlot.LUNCH, MealSlot.atHour(15))
    }

    @Test
    fun `o jantar so comeca as dezoito`() {

        assertEquals(MealSlot.SNACK, MealSlot.atHour(16))
        assertEquals(MealSlot.SNACK, MealSlot.atHour(17))
        assertEquals(MealSlot.DINNER, MealSlot.atHour(18))
        assertEquals(MealSlot.DINNER, MealSlot.atHour(21))
        assertEquals(MealSlot.DINNER, MealSlot.atHour(22))
    }

    @Test
    fun `a madrugada e a ceia caem em lanche`() {
        assertEquals(MealSlot.SNACK, MealSlot.atHour(23))
        assertEquals(MealSlot.SNACK, MealSlot.atHour(0))
        assertEquals(MealSlot.SNACK, MealSlot.atHour(4))
    }

    @Test
    fun `nenhuma hora do dia fica sem resposta`() {

        val cobertas = (0..23).map { MealSlot.atHour(it) }
        assertEquals(24, cobertas.size)
    }
}
