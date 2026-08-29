package pt.antares.app.feature.fooddata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.ai.AiClient
import pt.antares.app.core.ai.AiRepository
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * As pesquisas que não deram nada eram uma lista de texto com uma contagem, e mais nada. Quem
 * a lia via o que faltava ao catálogo e não tinha como o acrescentar sem reescrever o nome à
 * mão — o passo em que se desiste.
 *
 * O que se testa aqui é o círculo fechar: o nome chega preenchido, e criar o alimento tira a
 * linha da lista. A chave da lista é o texto **normalizado**, por isso o nome que se guarda
 * tem de passar pela mesma regra que o gravou, ou a linha ficava lá para sempre.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FalhaCriaAlimentoTest : ViewModelHarness() {

    private fun repository() =
        FoodRepository(
            db.foodDao(),
            db.foodMarkDao(),
            db.foodNutrientDao(),
            db.searchMissDao(),
            db.foodLogDao(),
            dispatcher,
        )

    private fun viewModel(repo: FoodRepository) = FoodEditViewModel(
        repository = repo,
        ai = AiRepository(
            client = NenhumaIa,
            ensureAccount = {},
            saveFoodLog = {},
            latestWeightKg = { null },
            persistUsage = { _, _ -> },
            io = Dispatchers.Unconfined,
        ),
    )

    private suspend fun preencherEGuardar(vm: FoodEditViewModel) {
        vm.setKcal("120")
        vm.setProtein("20")
        vm.setCarbs("2")
        vm.setFat("4")
        vm.save()
        dispatcher.scheduler.advanceUntilIdle()
        vm.state.first { it.saved }
    }

    @Test
    fun `o nome da pesquisa falhada chega preenchido`() = runTest(dispatcher) {
        val vm = viewModel(repository())

        vm.start(foodId = null, barcode = null, nomeInicial = "queijo da ilha")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("queijo da ilha", vm.state.value.name)
    }

    @Test
    fun `criar o alimento tira a falha da lista`() = runTest(dispatcher) {
        val repo = repository()
        db.searchMissDao().record("queijo da ilha", epochDay = 20_000L)
        val vm = viewModel(repo)

        vm.start(foodId = null, barcode = null, nomeInicial = "queijo da ilha")
        dispatcher.scheduler.advanceUntilIdle()
        preencherEGuardar(vm)

        assertTrue(
            db.searchMissDao().top().isEmpty(),
            "deixar lá o que já foi criado tornava a lista um conjunto de tarefas repetidas",
        )
    }

    @Test
    fun `a acentuacao e as maiusculas nao impedem a falha de sair`() = runTest(dispatcher) {
        val repo = repository()
        // O que fica gravado é sempre o texto normalizado — é assim que a pesquisa o escreve.
        db.searchMissDao().record("bacalhau a bras", epochDay = 20_000L)
        val vm = viewModel(repo)

        vm.start(foodId = null, barcode = null, nomeInicial = "bacalhau a bras")
        dispatcher.scheduler.advanceUntilIdle()
        vm.setName("Bacalhau à Brás")
        preencherEGuardar(vm)

        assertTrue(
            db.searchMissDao().top().isEmpty(),
            "quem escreve o nome como deve ser continua a resolver a falha que o gerou",
        )
    }

    @Test
    fun `criar outra coisa qualquer deixa a falha onde estava`() = runTest(dispatcher) {
        val repo = repository()
        db.searchMissDao().record("queijo da ilha", epochDay = 20_000L)
        val vm = viewModel(repo)

        vm.setName("Frango grelhado")
        preencherEGuardar(vm)

        assertEquals(
            listOf("queijo da ilha"),
            db.searchMissDao().top().map { it.query },
            "o catálogo continua sem o queijo, e a lista tem de continuar a dizê-lo",
        )
    }

    private object NenhumaIa : AiClient {
        override suspend fun analyzeFoodText(text: String, lang: String, day: String) =
            error("não usado")

        override suspend fun analyzeFoodPhoto(imageBase64: String, mime: String, lang: String, day: String) =
            error("não usado")

        override suspend fun readLabel(imageBase64: String, mime: String, lang: String, day: String) =
            error("não usado")

        override suspend fun analyzeExercise(text: String, weightKg: Double, lang: String, day: String) =
            error("não usado")
    }
}
