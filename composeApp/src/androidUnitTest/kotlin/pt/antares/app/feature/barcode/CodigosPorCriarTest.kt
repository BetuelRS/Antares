package pt.antares.app.feature.barcode

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.network.off.OffApi
import pt.antares.app.feature.fooddata.FoodRepository
import pt.antares.app.feature.fooddata.OffRepository
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals

/**
 * Na leitura contínua, um produto que não existe em lado nenhum contava para um número. Liam-se
 * cinco, dois falhavam, e no fim ficava-se a saber que dois falharam — sem saber quais, e sem
 * poder criar nenhum.
 *
 * Guarda-se a lista dos códigos. E guarda-se cada um **uma vez**: com a câmara apontada ao mesmo
 * produto o código volta a ser lido muitas vezes por segundo, e a lista é de produtos por criar,
 * não de leituras.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CodigosPorCriarTest : ViewModelHarness() {

    /** A Open Food Facts responde `status: 0` para um código que não conhece. */
    private fun offSemProdutos(): OffRepository {
        val client = HttpClient(
            MockEngine {
                respond(
                    """{"status":0,"product":null}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return OffRepository(OffApi(client, "teste"), db.foodDao(), dispatcher)
    }

    private fun viewModel(): BarcodeResolveViewModel {
        val vm = BarcodeResolveViewModel(
            foodRepository = FoodRepository(
                db.foodDao(),
                db.foodMarkDao(),
                db.foodNutrientDao(),
                db.searchMissDao(),
                db.foodLogDao(),
                dispatcher,
            ),
            offRepository = offSemProdutos(),
            diaryRepository = diaryRepository(),
        )
        vm.configure(MealSlot.LUNCH, epochDay = 20_000L)
        vm.toggleContinuous()
        return vm
    }

    /**
     * O `advanceUntilIdle` sozinho não chega: o motor de HTTP de teste responde numa linha de
     * execução verdadeira, fora do tempo virtual. O `resolve` põe o estado em `Resolving`
     * antes de lançar, por isso esperar pelo `Idle` é esperar pela leitura acabar mesmo.
     */
    private suspend fun ler(vm: BarcodeResolveViewModel, codigo: String) {
        vm.resolve(codigo)
        dispatcher.scheduler.advanceUntilIdle()
        vm.result.first { it is BarcodeResult.Idle }
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `guarda o codigo e nao so a contagem`() = runTest(dispatcher) {
        val vm = viewModel()

        ler(vm, "5601234567890")

        assertEquals(listOf("5601234567890"), vm.notFound.value)
    }

    @Test
    fun `o mesmo produto lido muitas vezes conta uma`() = runTest(dispatcher) {
        val vm = viewModel()

        repeat(4) { ler(vm, "5601234567890") }

        assertEquals(
            listOf("5601234567890"),
            vm.notFound.value,
            "a câmara relê o mesmo código dezenas de vezes; a lista é de produtos por criar",
        )
    }

    @Test
    fun `codigos diferentes ficam todos, pela ordem em que falharam`() = runTest(dispatcher) {
        val vm = viewModel()

        ler(vm, "5601234567890")
        ler(vm, "5609876543210")

        assertEquals(listOf("5601234567890", "5609876543210"), vm.notFound.value)
    }

    @Test
    fun `criar o alimento tira o codigo da lista`() = runTest(dispatcher) {
        val vm = viewModel()
        ler(vm, "5601234567890")
        ler(vm, "5609876543210")

        vm.forgetNotFound("5601234567890")

        assertEquals(
            listOf("5609876543210"),
            vm.notFound.value,
            "a leitura continua a correr, e o produto já criado não pode voltar a aparecer " +
                "como estando por resolver",
        )
    }
}
