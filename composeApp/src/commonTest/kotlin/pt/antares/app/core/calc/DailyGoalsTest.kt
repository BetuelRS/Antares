package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class DailyGoalsTest {

    @Test
    fun `a agua e 35 ml por kg, arredondada aos 50`() {

        assertEquals(2800, DailyGoals.waterMl(80.0))

        assertEquals(2200, DailyGoals.waterMl(63.0))

        assertEquals(2500, DailyGoals.waterMl(71.0))
    }

    @Test
    fun `o resultado e sempre multiplo de 50`() {
        for (kg in 40..150) {
            val ml = DailyGoals.waterMl(kg.toDouble())
            assertEquals(0, ml % DailyGoals.WATER_ROUNDING_ML, "$kg kg deu $ml ml")
        }
    }

    @Test
    fun `peso invalido nao produz meta`() {
        assertEquals(0, DailyGoals.waterMl(0.0))
        assertEquals(0, DailyGoals.waterMl(-5.0))
    }

    @Test
    fun `mais peso nunca da menos agua`() {
        var anterior = 0
        for (kg in 40..150) {
            val ml = DailyGoals.waterMl(kg.toDouble())
            kotlin.test.assertTrue(ml >= anterior, "a $kg kg a meta desceu para $ml")
            anterior = ml
        }
    }

    @Test
    fun `a fibra e a mesma para toda a gente`() {
        assertEquals(25, DailyGoals.fibreG())
    }
}
