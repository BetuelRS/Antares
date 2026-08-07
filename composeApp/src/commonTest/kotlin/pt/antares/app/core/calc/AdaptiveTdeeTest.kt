package pt.antares.app.core.calc

import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveTdeeTest {

    private fun week(
        avgIntakeKcal: Double = 2000.0,
        loggedDays: Int = 7,
        weightTrendDeltaKg: Double = -0.5,
        weighIns: Int = 4,
        currentTdee: Double = 2500.0,
        goalRateKcal: Int = -500,
        sex: Sex = Sex.MALE,
        bmr: Double? = null,
    ) = AdaptiveTdee.WeekInput(
        avgIntakeKcal = avgIntakeKcal,
        loggedDays = loggedDays,
        weightTrendDeltaKg = weightTrendDeltaKg,
        weighIns = weighIns,
        currentTdee = currentTdee,
        goalRateKcal = goalRateKcal,
        sex = sex,
        bmr = bmr,
    )

    private fun propose(input: AdaptiveTdee.WeekInput): AdaptiveTdee.Proposal {
        val r = AdaptiveTdee.evaluate(input)
        assertTrue(r is AdaptiveTdee.Result.Propose, "esperava proposta, veio $r")
        return r.proposal
    }

    private fun skip(input: AdaptiveTdee.WeekInput): AdaptiveTdee.Veto {
        val r = AdaptiveTdee.evaluate(input)
        assertTrue(r is AdaptiveTdee.Result.Skip, "esperava veto, veio $r")
        return r.reason
    }

    @Test
    fun `menos de 5 dias registados nao propoe nada`() {
        assertEquals(AdaptiveTdee.Veto.FEW_LOGGED_DAYS, skip(week(loggedDays = 4)))

        assertTrue(AdaptiveTdee.evaluate(week(loggedDays = 5)) is AdaptiveTdee.Result.Propose)
    }

    @Test
    fun `com uma pesagem so nao ha delta para medir`() {
        assertEquals(AdaptiveTdee.Veto.FEW_WEIGH_INS, skip(week(weighIns = 1)))
    }

    @Test
    fun `variacao de peso impossivel numa semana e agua, nao gordura`() {

        assertEquals(
            AdaptiveTdee.Veto.IMPLAUSIBLE_WEIGHT_CHANGE,
            skip(week(weightTrendDeltaKg = -2.0)),
        )
        assertEquals(
            AdaptiveTdee.Veto.IMPLAUSIBLE_WEIGHT_CHANGE,
            skip(week(weightTrendDeltaKg = 1.6)),
        )

        assertTrue(
            AdaptiveTdee.evaluate(week(weightTrendDeltaKg = -1.5)) is AdaptiveTdee.Result.Propose,
        )
    }

    @Test
    fun `quem perde peso mais depressa do que o previsto gasta mais do que pensavamos`() {

        val p = propose(week(avgIntakeKcal = 2000.0, weightTrendDeltaKg = -0.5, currentTdee = 2500.0))
        assertEquals(2550, p.observedTdee)

        assertEquals(2015, p.newTargetKcal)
        assertEquals(2000, p.previousTargetKcal)
        assertEquals(15, p.deltaKcal)
    }

    @Test
    fun `quem nao perde peso apesar do defice esta a gastar menos - a meta desce`() {

        val p = propose(week(avgIntakeKcal = 2000.0, weightTrendDeltaKg = 0.0, currentTdee = 2500.0))
        assertEquals(2000, p.observedTdee)

        assertEquals(1850, p.newTargetKcal)
        assertTrue(p.deltaKcal < 0)
    }

    @Test
    fun `a meta nunca se mexe mais de 200 kcal numa semana`() {

        val p = propose(week(avgIntakeKcal = 3000.0, weightTrendDeltaKg = -1.4, currentTdee = 2500.0))
        assertTrue(p.clamped, "devia ter sido travado")
        assertEquals(200, p.deltaKcal)
    }

    @Test
    fun `o piso de seguranca vence sempre a adaptacao`() {

        val p = propose(
            week(
                avgIntakeKcal = 1200.0,
                weightTrendDeltaKg = 0.0,
                currentTdee = 1600.0,
                goalRateKcal = -500,
                sex = Sex.FEMALE,
            ),
        )
        assertTrue(p.flooredToSafety)
        assertEquals(NutritionCalc.FLOOR_FEMALE, p.newTargetKcal)
        assertTrue(p.newTargetKcal >= NutritionCalc.FLOOR_FEMALE)
    }

    @Test
    fun `o piso relativo ao basal trava onde o fixo deixava passar`() {

        val p = propose(
            week(
                avgIntakeKcal = 1600.0,
                weightTrendDeltaKg = 0.0,
                currentTdee = 1700.0,
                goalRateKcal = -500,
                bmr = 2180.0,
            ),
        )
        assertTrue(p.flooredToSafety)
        assertEquals(1744, p.newTargetKcal)
    }

    @Test
    fun `sem basar conhecido vale o piso fixo, como antes`() {

        val p = propose(
            week(
                avgIntakeKcal = 1200.0,
                weightTrendDeltaKg = 0.0,
                currentTdee = 1600.0,
                goalRateKcal = -500,
                bmr = null,
            ),
        )
        assertEquals(NutritionCalc.FLOOR_MALE, p.newTargetKcal)
    }

    @Test
    fun `semanas seguidas a cortar nao levam ninguem abaixo do basal`() {

        val bmr = 2180.0
        var tdee = 3000.0
        var target = 0
        repeat(10) {
            val p = propose(
                week(
                    avgIntakeKcal = 1500.0,
                    weightTrendDeltaKg = 0.0,
                    currentTdee = tdee,
                    goalRateKcal = -500,
                    bmr = bmr,
                ),
            )
            target = p.newTargetKcal
            tdee = p.newTdee.toDouble()
        }
        assertTrue(
            target >= (bmr * NutritionCalc.BMR_FLOOR_FRACTION).toInt(),
            "dez semanas de corte levaram a meta a $target, abaixo de 80% do basal",
        )
    }

    @Test
    fun `quem quer ganhar peso e nao ganha recebe mais comida`() {

        val p = propose(
            week(
                avgIntakeKcal = 3000.0,
                weightTrendDeltaKg = 0.0,
                currentTdee = 2700.0,
                goalRateKcal = 300,
            ),
        )

        assertEquals(3090, p.newTargetKcal)
        assertTrue(p.deltaKcal > 0)
    }

    @Test
    fun `semana em que os dados confirmam a estimativa nao muda quase nada`() {

        val p = propose(
            week(avgIntakeKcal = 2000.0, weightTrendDeltaKg = -0.4545, currentTdee = 2500.0),
        )
        assertTrue(p.deltaKcal in -5..5, "devia estar quase parado, veio ${p.deltaKcal}")
    }
}
