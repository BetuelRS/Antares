package pt.antares.app.core.calc

import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlateauTest {

    private fun week(
        stallWeeks: Int = 0,
        goalRateKcal: Int = -500,
        loggedDays: Int = 7,
    ) = AdaptiveTdee.WeekInput(
        avgIntakeKcal = 2000.0,
        loggedDays = loggedDays,
        weightTrendDeltaKg = 0.0,
        weighIns = 4,
        currentTdee = 2500.0,
        goalRateKcal = goalRateKcal,
        sex = Sex.MALE,
        consecutiveStallWeeks = stallWeeks,
    )

    @Test
    fun `duas semanas paradas ainda deixam o motor trabalhar`() {

        val r = AdaptiveTdee.evaluate(week(stallWeeks = 2))
        assertTrue(r is AdaptiveTdee.Result.Propose)
    }

    @Test
    fun `a terceira semana parada trava o corte`() {
        val r = AdaptiveTdee.evaluate(week(stallWeeks = 3))
        assertTrue(r is AdaptiveTdee.Result.Skip)
        assertEquals(AdaptiveTdee.Veto.LIKELY_METABOLIC_ADAPTATION, r.reason)
    }

    @Test
    fun `quem quer ganhar peso nao e travado`() {

        val r = AdaptiveTdee.evaluate(week(stallWeeks = 5, goalRateKcal = 300))
        assertTrue(r is AdaptiveTdee.Result.Propose)
    }

    @Test
    fun `em manutencao tambem nao ha travao`() {
        val r = AdaptiveTdee.evaluate(week(stallWeeks = 5, goalRateKcal = 0))
        assertTrue(r is AdaptiveTdee.Result.Propose)
    }

    @Test
    fun `registo fiel e semanas a fio parece adaptacao`() {
        assertEquals(
            AdaptiveTdee.Assessment.METABOLIC_ADAPTATION,
            AdaptiveTdee.assessPlateau(consecutiveStallWeeks = 4, loggedDays = 7),
        )
    }

    @Test
    fun `com dias em falta o mais provavel e faltar comida ao registo`() {

        assertEquals(
            AdaptiveTdee.Assessment.LIKELY_UNDER_LOGGING,
            AdaptiveTdee.assessPlateau(consecutiveStallWeeks = 4, loggedDays = 4),
        )
    }

    @Test
    fun `poucas semanas nao dao para opinar`() {
        assertEquals(
            AdaptiveTdee.Assessment.UNCLEAR,
            AdaptiveTdee.assessPlateau(consecutiveStallWeeks = 1, loggedDays = 7),
        )
    }

    @Test
    fun `a pausa propoe comer a manutencao`() {
        val s = DietBreak.suggest(currentTdee = 2500.0, consecutiveStallWeeks = 4, loggedDays = 7)
        assertEquals(2500, s.maintenanceKcal)
        assertEquals(DietBreak.DEFAULT_WEEKS, s.weeks)
        assertTrue(s.isWorthSuggesting)
    }

    @Test
    fun `nao se propoe pausa a quem esta a subregistar`() {
        val s = DietBreak.suggest(currentTdee = 2500.0, consecutiveStallWeeks = 4, loggedDays = 3)
        assertFalse(s.isWorthSuggesting)
        assertEquals(AdaptiveTdee.Assessment.LIKELY_UNDER_LOGGING, s.assessment)
    }

    @Test
    fun `sem plateau nao se propoe nada`() {
        assertFalse(
            DietBreak.suggest(currentTdee = 2500.0, consecutiveStallWeeks = 0, loggedDays = 7)
                .isWorthSuggesting,
        )
    }
}
