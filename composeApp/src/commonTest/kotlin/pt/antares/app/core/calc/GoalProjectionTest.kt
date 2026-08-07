package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoalProjectionTest {

    private val today = 20639L

    @Test
    fun `com ritmo medido da semanas e data`() {

        val p = GoalProjection.project(84.0, 80.0, measuredRateKgWeek = -0.5, todayEpochDay = today)
        assertEquals(8, p.weeks)
        assertEquals(today + 56L, p.etaEpochDay)
        assertEquals(4.0, p.remainingKg, 0.001)
    }

    @Test
    fun `sem ritmo medido nao ha data`() {

        val p = GoalProjection.project(84.0, 80.0, measuredRateKgWeek = null, todayEpochDay = today)
        assertNull(p.weeks)
        assertNull(p.etaEpochDay)
        assertFalse(p.reached)
        assertFalse(p.movingAway)
    }

    @Test
    fun `ritmo quase parado nao gera data`() {

        val p = GoalProjection.project(84.0, 80.0, measuredRateKgWeek = -0.01, todayEpochDay = today)
        assertNull(p.weeks)
    }

    @Test
    fun `andar ao contrario do objetivo e dito, nao escondido`() {

        val p = GoalProjection.project(84.0, 80.0, measuredRateKgWeek = 0.3, todayEpochDay = today)
        assertTrue(p.movingAway)
        assertNull(p.weeks)
    }

    @Test
    fun `ganhar peso tambem projeta`() {

        val p = GoalProjection.project(60.0, 65.0, measuredRateKgWeek = 0.25, todayEpochDay = today)
        assertEquals(20, p.weeks)
        assertFalse(p.movingAway)
    }

    @Test
    fun `quem quer ganhar e esta a perder tambem e avisado`() {
        val p = GoalProjection.project(60.0, 65.0, measuredRateKgWeek = -0.25, todayEpochDay = today)
        assertTrue(p.movingAway)
    }

    @Test
    fun `objetivo atingido dentro da tolerancia`() {
        val p = GoalProjection.project(80.1, 80.0, measuredRateKgWeek = -0.5, todayEpochDay = today)
        assertTrue(p.reached)
        assertNull(p.weeks)
    }

    @Test
    fun `data longinqua demais nao se mostra`() {

        val p = GoalProjection.project(110.0, 80.0, measuredRateKgWeek = -0.1, todayEpochDay = today)
        assertNull(p.etaEpochDay)
        assertFalse(p.movingAway)
    }

    @Test
    fun `semanas arredondam para cima`() {

        val p = GoalProjection.project(84.1, 80.0, measuredRateKgWeek = -1.0, todayEpochDay = today)
        assertEquals(5, p.weeks)
    }
}
