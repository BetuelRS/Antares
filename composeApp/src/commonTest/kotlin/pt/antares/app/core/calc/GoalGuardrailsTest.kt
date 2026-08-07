package pt.antares.app.core.calc

import pt.antares.app.core.model.GoalType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoalGuardrailsTest {

    @Test
    fun `um alvo de 35 kg para 178 cm e sinalizado`() {

        assertTrue(BodyComposition.isGoalWeightBelowHealthy(35.0, 178))
    }

    @Test
    fun `um alvo dentro da faixa nao e sinalizado`() {

        assertFalse(BodyComposition.isGoalWeightBelowHealthy(70.0, 178))
        assertFalse(BodyComposition.isGoalWeightBelowHealthy(59.0, 178))
    }

    @Test
    fun `o limite de cima nao e problema deste aviso`() {

        assertFalse(BodyComposition.isGoalWeightBelowHealthy(95.0, 178))
    }

    @Test
    fun `sem altura valida nao se opina`() {
        assertFalse(BodyComposition.isGoalWeightBelowHealthy(35.0, 0))
    }

    @Test
    fun `a fronteira e a mesma que o cartao do IMC mostra`() {

        val range = BodyComposition.healthyWeightRange(178)!!
        assertFalse(BodyComposition.isGoalWeightBelowHealthy(range.start, 178))
        assertTrue(BodyComposition.isGoalWeightBelowHealthy(range.start - 0.1, 178))
    }

    @Test
    fun `atingido dentro da tolerancia`() {
        assertTrue(NutritionCalc.hasReachedGoalWeight(74.0, 74.1))
        assertTrue(NutritionCalc.hasReachedGoalWeight(74.0, 73.9))
    }

    @Test
    fun `passar do alvo tambem conta como atingido`() {

        assertTrue(NutritionCalc.hasReachedGoalWeight(74.0, 73.8) || 74.0 - 73.8 > 0.3)
        assertEquals(0.3, NutritionCalc.GOAL_REACHED_TOLERANCE_KG)
    }

    @Test
    fun `ainda longe nao conta`() {
        assertFalse(NutritionCalc.hasReachedGoalWeight(74.0, 80.0))
    }

    @Test
    fun `abaixo dos 18 a zona segura de perda aperta`() {
        val adulto = NutritionCalc.safeWeeklyLossKg(60.0, ageYears = 30).endInclusive
        val menor = NutritionCalc.safeWeeklyLossKg(60.0, ageYears = 16).endInclusive
        assertEquals(adulto / 2, menor, 0.001)
    }

    @Test
    fun `aos 18 ja e a zona de adulto`() {
        assertEquals(
            NutritionCalc.safeWeeklyLossKg(60.0, ageYears = 30).endInclusive,
            NutritionCalc.safeWeeklyLossKg(60.0, ageYears = 18).endInclusive,
            0.001,
        )
    }

    @Test
    fun `um ritmo aceite num adulto avisa num menor`() {

        val rate = NutritionCalc.kcalPerDayFromWeeklyKg(-0.45)
        assertFalse(NutritionCalc.isRateAboveSafeZone(GoalType.LOSE, rate, 60.0, ageYears = 30))
        assertTrue(NutritionCalc.isRateAboveSafeZone(GoalType.LOSE, rate, 60.0, ageYears = 16))
    }

    @Test
    fun `ganhar peso nao aperta para menores`() {

        val rate = NutritionCalc.kcalPerDayFromWeeklyKg(0.25)
        assertFalse(NutritionCalc.isRateAboveSafeZone(GoalType.GAIN, rate, 60.0, ageYears = 30))
        assertFalse(NutritionCalc.isRateAboveSafeZone(GoalType.GAIN, rate, 60.0, ageYears = 16))
    }
}
