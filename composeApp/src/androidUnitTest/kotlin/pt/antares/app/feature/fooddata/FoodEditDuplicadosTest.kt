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
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Criar um alimento não verificava se já existia outro com o mesmo nome, e o catálogo enchia-se
 * de cópias que só quem as criou reconhece.
 *
 * O aviso procura com o **índice da pesquisa**, e não por igualdade de texto: quem escreve
 * «arroz cozido» tem de ser avisado do «Arroz, cozido» que já lá está. E avisa sem bloquear —
 * há bacalhaus diferentes, e o botão de guardar continua com as mesmas condições de antes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FoodEditDuplicadosTest : ViewModelHarness() {

    private fun viewModel() = FoodEditViewModel(
        repository = FoodRepository(db.foodDao(), db.foodNutrientDao(), db.searchMissDao(), dispatcher),
        ai = AiRepository(
            client = NenhumaIa,
            ensureAccount = {},
            saveFoodLog = {},
            latestWeightKg = { null },
            persistUsage = { _, _ -> },
            io = Dispatchers.Unconfined,
        ),
    )

    private suspend fun guardado(id: String, nome: String, kcal: Int) {
        db.foodDao().upsertWithFts(
            FoodEntity(
                id = id,
                source = FoodSource.SEED,
                sourceRef = null,
                namePt = nome,
                nameEn = nome,
                brand = null,
                kcal = kcal,
                proteinG = 2.0,
                carbsG = 28.0,
                sugarsG = null,
                fatG = 0.3,
                satFatG = null,
                fiberG = null,
                sodiumMg = null,
                microsJson = null,
                servingName = null,
                servingGrams = null,
                updatedAt = 0L,
            ),
            TextNormalize.normalize(nome),
        )
    }

    private suspend fun escrever(vm: FoodEditViewModel, nome: String) {
        vm.setName(nome)
        dispatcher.scheduler.advanceUntilIdle()
        vm.state.first()
    }

    @Test
    fun `avisa do que ja existe com outra pontuacao`() = runTest(dispatcher) {
        guardado("ciqual-9645", "Arroz, cozido", kcal = 130)
        val vm = viewModel()

        escrever(vm, "arroz cozido")

        val duplicados = vm.state.value.duplicados
        assertEquals(listOf("ciqual-9645"), duplicados.map { it.id })
        assertEquals(130, duplicados.single().kcal, "as calorias são o que distingue dois arrozes")
    }

    @Test
    fun `nao avisa antes de haver nome que chegue`() = runTest(dispatcher) {
        guardado("ciqual-9645", "Arroz, cozido", kcal = 130)
        val vm = viewModel()

        escrever(vm, "ar")

        assertTrue(
            vm.state.value.duplicados.isEmpty(),
            "duas letras avisariam de meio catálogo, e o aviso deixava de se ler",
        )
    }

    @Test
    fun `nome que nao existe nao avisa de nada`() = runTest(dispatcher) {
        guardado("ciqual-9645", "Arroz, cozido", kcal = 130)
        val vm = viewModel()

        escrever(vm, "bacalhau da minha avó")

        assertTrue(vm.state.value.duplicados.isEmpty())
    }

    @Test
    fun `o aviso nunca impede de guardar`() = runTest(dispatcher) {
        guardado("ciqual-9645", "Arroz, cozido", kcal = 130)
        val vm = viewModel()

        escrever(vm, "arroz cozido")
        vm.setKcal("130")
        vm.setProtein("2.4")
        vm.setCarbs("28")
        vm.setFat("0.3")

        val s = vm.state.value
        assertTrue(s.duplicados.isNotEmpty(), "o aviso tem de estar de pé para o teste valer")
        assertTrue(s.valid, "avisar não é bloquear: há bacalhaus diferentes")
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
