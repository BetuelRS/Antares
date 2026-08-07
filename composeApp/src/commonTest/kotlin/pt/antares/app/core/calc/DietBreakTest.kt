package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DietBreakTest {

    private fun sugestao(semanasParadas: Int, diasRegistados: Int) =
        DietBreak.suggest(
            currentTdee = 2500.0,
            consecutiveStallWeeks = semanasParadas,
            loggedDays = diasRegistados,
        )

    @Test
    fun `a manutencao proposta e o gasto tal como esta`() {
        assertEquals(2500, sugestao(semanasParadas = 6, diasRegistados = 7).maintenanceKcal)
    }

    @Test
    fun `a pausa proposta e de duas semanas`() {
        assertEquals(DietBreak.DEFAULT_WEEKS, sugestao(6, 7).weeks)
        assertEquals(2, DietBreak.DEFAULT_WEEKS)
    }

    @Test
    fun `peso parado com registo fiel merece a proposta`() {
        val s = sugestao(semanasParadas = 6, diasRegistados = 7)
        assertEquals(AdaptiveTdee.Assessment.METABOLIC_ADAPTATION, s.assessment)
        assertTrue(s.isWorthSuggesting)
    }

    @Test
    fun `peso parado com registo irregular nao merece a proposta`() {
        val s = sugestao(semanasParadas = 6, diasRegistados = 2)
        assertEquals(AdaptiveTdee.Assessment.LIKELY_UNDER_LOGGING, s.assessment)
        assertFalse(s.isWorthSuggesting)
    }

    @Test
    fun `uma semana parada ainda nao e um plateau`() {
        val s = sugestao(semanasParadas = 1, diasRegistados = 7)
        assertEquals(AdaptiveTdee.Assessment.UNCLEAR, s.assessment)
        assertFalse(s.isWorthSuggesting)
    }

    @Test
    fun `sem plateau nao ha proposta`() {
        assertFalse(sugestao(semanasParadas = 0, diasRegistados = 7).isWorthSuggesting)
    }
}
