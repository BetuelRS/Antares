package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class MetCalcTest {

    @Test
    fun `kcal segue MET menos um x peso x horas`() {
        // Mudança intencional: o valor é o que se gasta **a mais** do que sentado, porque
        // o repouso já está dentro da meta diária.
        assertEquals(350, MetCalc.kcal(met = 11.0, weightKg = 70.0, durationMin = 30))

        assertEquals(200, MetCalc.kcal(3.5, 80.0, 60))

        assertEquals(341, MetCalc.kcal(8.0, 65.0, 45))

        assertEquals(120, MetCalc.kcal(5.0, 90.0, 20))

        assertEquals(352, MetCalc.kcal(9.8, 60.0, 40))
    }

    @Test
    fun `uma hora de cinco MET a oitenta quilos perde as oitenta kcal do repouso`() {
        // O caso do achado, escrito por extenso: era 400, passa a 320.
        assertEquals(320, MetCalc.kcal(met = 5.0, weightKg = 80.0, durationMin = 60))
    }

    @Test
    fun `entradas invalidas nao produzem kcal`() {
        assertEquals(0, MetCalc.kcal(met = 8.0, weightKg = 70.0, durationMin = 0))
        assertEquals(0, MetCalc.kcal(met = 0.0, weightKg = 70.0, durationMin = 30))
        assertEquals(0, MetCalc.kcal(met = 8.0, weightKg = 0.0, durationMin = 30))
    }

    @Test
    fun `estar sentado nao rende nada`() {
        assertEquals(
            0,
            MetCalc.kcal(met = 1.0, weightKg = 70.0, durationMin = 60),
            "um MET é o repouso, e o repouso já está na meta",
        )
        assertEquals(0, MetCalc.kcal(met = 0.8, weightKg = 70.0, durationMin = 60))
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
