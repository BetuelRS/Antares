package pt.antares.app.feature.running

import pt.antares.app.feature.running.domain.RunPrCalc
import pt.antares.app.feature.running.domain.Split
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RunPrCalcTest {

    private fun km(index: Int, sec: Int) =
        Split(index = index, distanceM = 1000.0, movingMs = sec * 1000L, paceSecPerKm = sec, kcal = 60)

    @Test
    fun `tempo de 1k e o melhor split de 1 km entre atividades`() {
        val a = listOf(km(1, 300), km(2, 320))
        val b = listOf(km(1, 290), km(2, 310), km(3, 305))
        assertEquals(290_000L, RunPrCalc.bestTimeMs(listOf(a, b), 1))
    }

    @Test
    fun `5k soma os primeiros 5 splits completos`() {
        val run = (1..6).map { km(it, 300) }
        assertEquals(5 * 300_000L, RunPrCalc.timeForKm(run, 5))
    }

    @Test
    fun `sem distancia suficiente o PR e null`() {
        val run = listOf(km(1, 300), km(2, 300))
        assertNull(RunPrCalc.timeForKm(run, 5))
        assertNull(RunPrCalc.bestTimeMs(listOf(run), 10))
    }

    @Test
    fun `split parcial final nao conta para PR`() {
        val run = listOf(km(1, 300), Split(2, 400.0, 120_000L, 300, 24))
        assertEquals(300_000L, RunPrCalc.timeForKm(run, 1))
        assertNull(RunPrCalc.timeForKm(run, 2))
    }
}
