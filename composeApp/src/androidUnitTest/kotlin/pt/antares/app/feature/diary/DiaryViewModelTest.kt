package pt.antares.app.feature.diary

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
import pt.antares.app.core.util.DayTicker
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DiaryViewModelTest : ViewModelHarness() {

    private val hoje get() = DayTicker.today.value

    private fun registo(
        kcal: Int,
        slot: MealSlot = MealSlot.LUNCH,
        dia: Long = hoje,
        id: String = "$slot-$kcal-$dia",
    ) = FoodLogEntity(
        id = id,
        epochDay = dia,
        mealSlot = slot,
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
    fun `abre no dia de hoje`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading }
        assertEquals(hoje, estado.epochDay)
        assertTrue(estado.isToday)
    }

    @Test
    fun `cada registo aparece na refeicao em que foi posto`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        db.foodLogDao().upsert(registo(kcal = 300, slot = MealSlot.BREAKFAST))
        db.foodLogDao().upsert(registo(kcal = 700, slot = MealSlot.LUNCH))
        advanceUntilIdle()

        val porRefeicao = vm.state.first { it.logsBySlot.isNotEmpty() }.logsBySlot
        assertEquals(1, porRefeicao[MealSlot.BREAKFAST]?.size)
        assertEquals(1, porRefeicao[MealSlot.LUNCH]?.size)
        assertEquals(300, porRefeicao[MealSlot.BREAKFAST]?.first()?.kcalSnapshot)
        assertEquals(700, porRefeicao[MealSlot.LUNCH]?.first()?.kcalSnapshot)
    }

    @Test
    fun `os totais somam as refeicoes todas do dia`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        db.foodLogDao().upsert(registo(kcal = 300, slot = MealSlot.BREAKFAST))
        db.foodLogDao().upsert(registo(kcal = 700, slot = MealSlot.LUNCH))
        db.foodLogDao().upsert(registo(kcal = 250, slot = MealSlot.SNACK))
        advanceUntilIdle()

        assertEquals(1250, vm.state.first { it.totals.kcal > 0 }.totals.kcal)
    }

    @Test
    fun `andar para tras mostra o dia de ontem`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        db.foodLogDao().upsert(registo(kcal = 400, dia = hoje))
        db.foodLogDao().upsert(registo(kcal = 900, dia = hoje - 1))
        advanceUntilIdle()

        vm.previousDay()
        advanceUntilIdle()

        val estado = vm.state.first { it.epochDay == hoje - 1 }
        assertEquals(900, estado.totals.kcal)
        assertTrue(!estado.isToday, "ontem não devia dizer que é hoje")
    }

    @Test
    fun `voltar a hoje repoe o dia`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        advanceUntilIdle()

        vm.previousDay()
        advanceUntilIdle()
        vm.goToDay(hoje)
        advanceUntilIdle()

        assertTrue(vm.state.first { it.epochDay == hoje }.isToday)
    }

    @Test
    fun `apagar um registo tira-o do dia`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        db.foodLogDao().upsert(registo(kcal = 500, id = "apagavel"))
        advanceUntilIdle()
        assertEquals(500, vm.state.first { it.totals.kcal > 0 }.totals.kcal)

        vm.deleteLog("apagavel")
        advanceUntilIdle()

        assertEquals(0, vm.state.first { it.totals.kcal == 0 }.totals.kcal)
    }

    @Test
    fun `a agua acumula ao longo do dia`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        advanceUntilIdle()

        vm.addWater(250)
        advanceUntilIdle()
        vm.addWater(500)
        advanceUntilIdle()

        assertEquals(750, vm.state.first { it.waterMl == 750 }.waterMl)
    }

    @Test
    fun `a agua de ontem nao aparece em hoje`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        advanceUntilIdle()
        vm.addWater(400)
        advanceUntilIdle()

        vm.previousDay()
        advanceUntilIdle()

        assertEquals(0, vm.state.first { it.epochDay == hoje - 1 }.waterMl)
    }

    @Test
    fun `mover um registo muda-o de refeicao`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        db.foodLogDao().upsert(registo(kcal = 600, slot = MealSlot.LUNCH, id = "movivel"))
        advanceUntilIdle()

        vm.moveLog("movivel", MealSlot.DINNER)
        advanceUntilIdle()

        val porRefeicao = vm.state.first { it.logsBySlot[MealSlot.DINNER]?.isNotEmpty() == true }
        assertEquals(null, porRefeicao.logsBySlot[MealSlot.LUNCH]?.firstOrNull())
        assertEquals(600, porRefeicao.logsBySlot[MealSlot.DINNER]?.first()?.kcalSnapshot)
    }

    // ---- a tira de semana ------------------------------------------------------------

    @Test
    fun `a tira de semana so ve os dias registados dentro dela`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        val inicioDaSemana = pt.antares.app.core.util.weekStartEpochDay(hoje)
        db.foodLogDao().upsert(registo(kcal = 300, dia = inicioDaSemana))
        db.foodLogDao().upsert(registo(kcal = 400, dia = inicioDaSemana - 1, id = "fora-da-semana"))
        advanceUntilIdle()

        val dias = vm.diasDaSemana.first { it.isNotEmpty() }
        assertEquals(listOf(inicioDaSemana), dias)
    }

    // ---- pesquisar no historico -------------------------------------------------------

    @Test
    fun `pesquisar encontra pelo nome em qualquer dia`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        db.foodLogDao().upsert(
            registo(kcal = 500, dia = hoje - 40, id = "bacalhau").copy(nameSnapshot = "Bacalhau à Gomes de Sá"),
        )
        advanceUntilIdle()

        vm.pesquisar("bacalhau")
        advanceUntilIdle()

        val achado = vm.resultadosDaPesquisa.first { it.isNotEmpty() }
        assertEquals("bacalhau", achado.single().id)
    }

    // ---- copiar o dia inteiro ----------------------------------------------------------

    @Test
    fun `copiar o dia inteiro acrescenta aos registos que ja la estao`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        db.foodLogDao().upsert(registo(kcal = 300, dia = hoje - 2, id = "a"))
        db.foodLogDao().upsert(registo(kcal = 400, dia = hoje - 2, id = "b"))
        db.foodLogDao().upsert(registo(kcal = 100, dia = hoje, id = "ja-la-estava"))
        advanceUntilIdle()

        var criados: List<String>? = null
        vm.copyDayFrom(hoje - 2) { criados = it }
        advanceUntilIdle()

        assertEquals(2, criados?.size)
        assertEquals(800, vm.state.first { it.totals.kcal == 800 }.totals.kcal)
    }

    @Test
    fun `desfazer a copia do dia apaga so o que ela criou`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        db.foodLogDao().upsert(registo(kcal = 300, dia = hoje - 2, id = "a"))
        db.foodLogDao().upsert(registo(kcal = 100, dia = hoje, id = "ja-la-estava"))
        advanceUntilIdle()

        var criados: List<String>? = null
        vm.copyDayFrom(hoje - 2) { criados = it }
        advanceUntilIdle()

        vm.desfazerCopiaDoDia(requireNotNull(criados))
        advanceUntilIdle()

        val estado = vm.state.first { it.totals.kcal == 100 }
        assertEquals(listOf("ja-la-estava"), estado.logsBySlot.values.flatten().map { it.id })
    }

    @Test
    fun `os candidatos a copiar vem do mais recente e sem o dia de hoje`() = runTest(dispatcher) {
        val vm = diaryViewModel()
        db.foodLogDao().upsert(registo(kcal = 300, dia = hoje - 3, id = "a"))
        db.foodLogDao().upsert(registo(kcal = 200, dia = hoje - 1, id = "b"))
        db.foodLogDao().upsert(registo(kcal = 900, dia = hoje, id = "hoje-mesmo"))
        advanceUntilIdle()

        vm.loadCopyDayCandidates()
        advanceUntilIdle()

        val candidatos = vm.copyDayCandidates.first { it != null }
        assertEquals(listOf(hoje - 1, hoje - 3), candidatos?.map { it.epochDay })
    }
}
