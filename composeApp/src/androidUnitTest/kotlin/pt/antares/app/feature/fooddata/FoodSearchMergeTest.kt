package pt.antares.app.feature.fooddata

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
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.network.off.OffApi
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.feature.recipe.RecipeRepository
import pt.antares.app.feature.templates.MealTemplateRepository
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O ecrã de pesquisa faz duas pesquisas ao mesmo tempo sobre o mesmo texto: uma na base, barata,
 * e outra na Open Food Facts, que custa um pedido a um serviço de terceiros. Têm esperas e
 * mínimos diferentes de propósito, e o resultado das duas aparece em listas separadas.
 *
 * O que se testa aqui é o encontro das duas: um produto lido por código de barras fica guardado
 * na base, e a partir daí a mesma procura encontra-o dos dois lados. Sem o corte, aparece a
 * dobrar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FoodSearchMergeTest : ViewModelHarness() {

    /** Conta os pedidos, para se poder afirmar que a rede não foi tocada. */
    private var pedidos = 0

    private fun offRepository(vararg codigos: Pair<String, String>): OffRepository {
        val produtos = codigos.joinToString(",") { (code, nome) ->
            """{"code":"$code","product_name":"$nome","nutriments":{"energy-kcal_100g":100.0}}"""
        }
        val corpo = """{"products":[$produtos],"count":${codigos.size}}"""
        val client = HttpClient(
            MockEngine {
                pedidos++
                respond(corpo, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        // Ligada: o interruptor da 2.2.0 tem o seu próprio teste, e aqui o que se mede é o
        // encontro das duas listas.
        return OffRepository(OffApi(client, "teste"), db.foodDao(), dispatcher) { true }
    }

    private fun viewModel(off: OffRepository) = FoodSearchViewModel(
        repository = FoodRepository(db.foodDao(), db.foodNutrientDao(), db.searchMissDao(), dispatcher),
        offRepository = off,
        recipeRepository = RecipeRepository(
            db.recipeDao(),
            db.recipeIngredientDao(),
            db.foodDao(),
            diaryRepository(),
            dispatcher,
        ),
        templateRepository = MealTemplateRepository(
            db.foodLogDao(),
            db.mealTemplateDao(),
            db.mealTemplateItemDao(),
            dispatcher,
        ),
        diaryRepository = diaryRepository(),
        preferences = prefs,
    )

    /**
     * Escreve na caixa de procura e espera pelas duas pesquisas. O `advanceUntilIdle` sozinho não
     * chega: o motor de HTTP de teste responde numa linha de execução verdadeira, fora do tempo
     * virtual, e sem esperar pelo fim afirmava-se sobre listas ainda por preencher.
     */
    private suspend fun procurar(vm: FoodSearchViewModel, texto: String) {
        // O aviso da Open Food Facts trava a primeira procura em linha até haver resposta.
        // Aqui dá-se por visto: o que se mede neste ficheiro é o encontro das duas listas, e
        // o aviso tem os seus testes noutro sítio.
        prefs.marcarAvisoDaOffVisto()
        vm.setQuery(texto)
        dispatcher.scheduler.advanceUntilIdle()
        vm.state.first { !it.searching && !it.searchingOnline }
        dispatcher.scheduler.advanceUntilIdle()
    }

    /** Guarda o alimento e o seu índice de pesquisa: sem o segundo, o primeiro não se encontra. */
    private suspend fun guardado(id: String, nome: String) {
        db.foodDao().upsertWithFts(
            FoodEntity(
                id = id,
                source = if (id.startsWith("off_")) FoodSource.OFF else FoodSource.CUSTOM,
                sourceRef = id.removePrefix("off_").takeIf { id.startsWith("off_") },
                namePt = nome,
                nameEn = nome,
                brand = null,
                kcal = 100,
                proteinG = 5.0,
                carbsG = 10.0,
                sugarsG = null,
                fatG = 2.0,
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

    @Test
    fun `o produto que ja esta guardado nao volta a aparecer na lista de fora`() =
        runTest(dispatcher) {
            // Mesmo código dos dois lados: foi lido por código de barras noutro dia e ficou
            // na base.
            guardado("off_123", "Iogurte natural")
            val vm = viewModel(offRepository("123" to "Iogurte natural"))

            procurar(vm, "iogurte")

            val estado = vm.state.value
            assertEquals(listOf("off_123"), estado.results.map { it.id })
            assertTrue(
                estado.onlineResults.isEmpty(),
                "o mesmo produto apareceu duas vezes: ${estado.onlineResults.map { it.id }}",
            )
        }

    @Test
    fun `o que a base nao tem continua a vir de fora`() = runTest(dispatcher) {
        guardado("off_123", "Iogurte natural")
        val vm = viewModel(offRepository("123" to "Iogurte natural", "456" to "Iogurte grego"))

        procurar(vm, "iogurte")

        assertEquals(listOf("off_456"), vm.state.value.onlineResults.map { it.id })
    }

    @Test
    fun `duas letras procuram na base, mas nao gastam um pedido de rede`() = runTest(dispatcher) {
        guardado("pao-1", "Pão de centeio")
        val vm = viewModel(offRepository("1" to "Pão de forma"))

        procurar(vm, "pa")

        assertEquals(listOf("pao-1"), vm.state.value.results.map { it.id })
        assertEquals(0, pedidos, "duas letras chegaram à Open Food Facts")
        assertTrue(vm.state.value.onlineResults.isEmpty())
    }

    @Test
    fun `uma letra so nao procura em lado nenhum`() = runTest(dispatcher) {
        guardado("pao-1", "Pão de centeio")
        val vm = viewModel(offRepository("1" to "Pão de forma"))

        procurar(vm, "p")

        assertTrue(vm.state.value.results.isEmpty())
        assertEquals(0, pedidos)
    }

    @Test
    fun `as tres letras a rede entra`() = runTest(dispatcher) {
        val vm = viewModel(offRepository("1" to "Iogurte grego"))

        procurar(vm, "iog")

        assertEquals(1, pedidos)
        assertEquals(listOf("off_1"), vm.state.value.onlineResults.map { it.id })
    }

    @Test
    fun `apagar o que se escreveu limpa as duas listas`() = runTest(dispatcher) {
        guardado("off_123", "Iogurte natural")
        val vm = viewModel(offRepository("456" to "Iogurte grego"))

        procurar(vm, "iogurte")
        assertTrue(vm.state.value.results.isNotEmpty())
        assertTrue(vm.state.value.onlineResults.isNotEmpty())

        procurar(vm, "")

        assertTrue(vm.state.value.results.isEmpty(), "os resultados locais ficaram no ecrã")
        assertTrue(vm.state.value.onlineResults.isEmpty(), "os resultados de fora ficaram no ecrã")
    }

    @Test
    fun `uma procura sem resultados nenhuns fica registada como falha`() = runTest(dispatcher) {
        val vm = viewModel(offRepository())

        procurar(vm, "cachupa")

        val falhas = db.searchMissDao().top(10)
        assertEquals(listOf("cachupa"), falhas.map { it.query })
    }

    @Test
    fun `uma procura com resultados nao e falha nenhuma`() = runTest(dispatcher) {
        guardado("pao-1", "Pão de centeio")
        val vm = viewModel(offRepository())

        procurar(vm, "pao")

        assertTrue(db.searchMissDao().top(10).isEmpty(), "registou falha com resultados no ecrã")
    }
}
