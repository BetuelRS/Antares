package pt.antares.app.feature.today

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.DayTicker
import pt.antares.app.testing.CountingHealthGateway
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TodayViewModelTest : ViewModelHarness() {

    private val hoje get() = DayTicker.today.value

    private fun refeicao(kcal: Int, dia: Long = hoje, id: String = "l$kcal-$dia") = FoodLogEntity(
        id = id,
        epochDay = dia,
        mealSlot = MealSlot.LUNCH,
        foodId = null,
        nameSnapshot = "arroz",
        quantityGrams = 100.0,
        kcalSnapshot = kcal,
        proteinSnapshot = 3.0,
        carbsSnapshot = 28.0,
        fatSnapshot = 1.0,
        microsPer100Json = null,
        updatedAt = Clock.System.now().toEpochMilliseconds(),
    )

    @Test
    fun `construir o Hoje nao toca no health connect`() = runTest(dispatcher) {
        val gateway = CountingHealthGateway()
        todayViewModel(gateway)
        advanceUntilIdle()

        assertEquals(
            0,
            gateway.totalCalls,
            "construir o TodayViewModel foi ao Health Connect ${gateway.totalCalls} vezes",
        )
    }

    @Test
    fun `o ecra a pedir e que dispara a troca com o health connect`() = runTest(dispatcher) {
        val gateway = CountingHealthGateway()
        val vm = todayViewModel(gateway)
        advanceUntilIdle()

        vm.syncHealthConnect()
        advanceUntilIdle()

        assertTrue(
            gateway.totalCalls > 0,
            "syncHealthConnect() não chegou a falar com o Health Connect",
        )
    }

    @Test
    fun `o consumido do dia soma o que se registou`() = runTest(dispatcher) {
        val vm = todayViewModel()
        db.foodLogDao().upsert(refeicao(kcal = 500))
        db.foodLogDao().upsert(refeicao(kcal = 300, id = "segundo"))
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading }
        assertEquals(800, estado.consumed.kcal)
    }

    @Test
    fun `o que se comeu ontem nao conta para hoje`() = runTest(dispatcher) {
        val vm = todayViewModel()
        db.foodLogDao().upsert(refeicao(kcal = 900, dia = hoje - 1))
        advanceUntilIdle()

        assertEquals(0, vm.state.first { !it.loading }.consumed.kcal)
    }

    @Test
    fun `a meta de agua segue o peso mais recente`() = runTest(dispatcher) {
        val vm = todayViewModel()
        db.weightLogDao().upsert(
            WeightLogEntity(
                id = "w1",
                epochDay = hoje,
                weightKg = 80.0,
                note = null,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
        advanceUntilIdle()

        assertEquals(2800, vm.state.first { !it.loading }.waterGoalMl)
    }

    @Test
    fun `a agua bebida aparece no estado`() = runTest(dispatcher) {
        val vm = todayViewModel()
        db.waterLogDao().upsert(
            WaterLogEntity(
                id = "a1",
                epochDay = hoje,
                ml = 750,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
        advanceUntilIdle()

        assertEquals(750, vm.state.first { !it.loading }.waterMl)
    }

    @Test
    fun `a sequencia conta o dia em que se registou`() = runTest(dispatcher) {
        val vm = todayViewModel()
        db.foodLogDao().upsert(refeicao(kcal = 400))
        advanceUntilIdle()

        val streak = vm.loggingStreak.first { it.loggedToday }
        assertEquals(1, streak.current)
    }

    @Test
    fun `sem registos a sequencia esta a zero`() = runTest(dispatcher) {
        val vm = todayViewModel()
        advanceUntilIdle()

        val streak = vm.loggingStreak.value
        assertEquals(0, streak.current)
        assertTrue(!streak.loggedToday)
    }

    @Test
    fun `sem perfil nao ha orcamento semanal`() = runTest(dispatcher) {
        val vm = todayViewModel()
        advanceUntilIdle()

        assertEquals(null, vm.weeklyBudget.value)
    }
}
