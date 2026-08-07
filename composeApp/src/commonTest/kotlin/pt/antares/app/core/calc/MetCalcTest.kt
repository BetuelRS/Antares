package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class MetCalcTest {

    @Test
    fun `kcal segue MET x peso x horas`() {

        assertEquals(385, MetCalc.kcal(met = 11.0, weightKg = 70.0, durationMin = 30))

        assertEquals(280, MetCalc.kcal(3.5, 80.0, 60))

        assertEquals(390, MetCalc.kcal(8.0, 65.0, 45))

        assertEquals(150, MetCalc.kcal(5.0, 90.0, 20))

        assertEquals(392, MetCalc.kcal(9.8, 60.0, 40))
    }

    @Test
    fun `entradas invalidas nao produzem kcal`() {
        assertEquals(0, MetCalc.kcal(met = 8.0, weightKg = 70.0, durationMin = 0))
        assertEquals(0, MetCalc.kcal(met = 0.0, weightKg = 70.0, durationMin = 30))
        assertEquals(0, MetCalc.kcal(met = 8.0, weightKg = 0.0, durationMin = 30))
    }

    @Test
    fun `o exercicio soma sempre ao orcamento`() {
        val b = DailyBudgetCalc.compute(target = 2000, consumed = 1800, exercise = 400)
        assertEquals(2400, b.budget)
        assertEquals(600, b.remaining)
    }

    @Test
    fun `sem exercicio o orcamento e a meta`() {

        val b = DailyBudgetCalc.compute(target = 2000, consumed = 1800, exercise = 0)
        assertEquals(2000, b.budget)
        assertEquals(200, b.remaining)
    }
}
