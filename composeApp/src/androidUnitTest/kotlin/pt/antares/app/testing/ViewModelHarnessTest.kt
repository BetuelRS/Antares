package pt.antares.app.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.diary.DiaryRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ViewModelHarnessTest : ViewModelHarness() {

    private fun registo(dia: Long, kcal: Int, nome: String = "arroz") = FoodLogEntity(
        id = "log-$nome-$kcal",
        epochDay = dia,
        mealSlot = MealSlot.LUNCH,
        foodId = null,
        nameSnapshot = nome,
        quantityGrams = 100.0,
        kcalSnapshot = kcal,
        proteinSnapshot = 2.0,
        carbsSnapshot = 28.0,
        fatSnapshot = 0.5,
        microsPer100Json = null,
        updatedAt = Clock.System.now().toEpochMilliseconds(),
    )

    @Test
    fun `a base de dados arranca vazia em cada teste`() = runTest(dispatcher) {
        val totais = db.foodLogDao().observeDayTotals(0).first()
        assertEquals(0, totais.kcal, "a BD devia começar vazia")
    }

    @Test
    fun `o que se escreve na BD chega ao repositorio`() = runTest(dispatcher) {
        val repo = DiaryRepository(db.foodLogDao(), db.waterLogDao(), dispatcher)
        db.foodLogDao().upsert(registo(dia = 10, kcal = 130))
        advanceUntilIdle()

        val totais = repo.observeDayTotals(10).first()
        assertEquals(130, totais.kcal, "o registo escrito não chegou ao repositório")
    }

    @Test
    fun `um dia sem registos nao herda os do dia ao lado`() = runTest(dispatcher) {
        val repo = DiaryRepository(db.foodLogDao(), db.waterLogDao(), dispatcher)
        db.foodLogDao().upsert(registo(dia = 10, kcal = 130))
        advanceUntilIdle()

        assertEquals(0, repo.observeDayTotals(11).first().kcal)
    }

    @Test
    fun `as preferencias comecam no valor por omissao e guardam`() = runTest(dispatcher) {
        assertEquals(0, prefs.lastCelebratedStreak.first())
        prefs.setLastCelebratedStreak(7)
        assertEquals(7, prefs.lastCelebratedStreak.first())
    }

    @Test
    fun `o duplo do health connect comeca a zero`() {
        val gateway = CountingHealthGateway()
        assertTrue(gateway.totalCalls == 0, "o contador devia começar a zero")
    }
}
