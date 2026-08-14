package pt.antares.app.feature.fooddata

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.network.off.OffApi
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A Open Food Facts é preenchida por voluntários e a pesquisa dela é generosa: devolve fichas
 * por preencher, produtos sem nome e coisas sem relação com o que se pediu. Os filtros do
 * [OffRepository.searchOnline] são o que separa isso de uma lista utilizável, e é o que aqui
 * se afirma — o mapeamento campo a campo tem o `OffMapperTest`.
 *
 * A distinção entre `null` e lista vazia é a parte que mais custa se partir: o ecrã diz
 * "sem internet" a um e "não há nada" ao outro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OffSearchOnlineTest : ViewModelHarness() {

    private fun produto(
        code: String?,
        nome: String?,
        marca: String? = null,
        kcal: Double? = 100.0,
    ): String {
        val campos = buildList {
            code?.let { add(""""code":"$it"""") }
            nome?.let { add(""""product_name":"$it"""") }
            marca?.let { add(""""brands":"$it"""") }
            kcal?.let { add(""""nutriments":{"energy-kcal_100g":$it}""") }
        }
        return "{${campos.joinToString(",")}}"
    }

    private fun repositoryQueDevolve(vararg produtos: String): OffRepository {
        val corpo = """{"products":[${produtos.joinToString(",")}],"count":${produtos.size}}"""
        val client = HttpClient(
            MockEngine {
                respond(
                    corpo,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return OffRepository(OffApi(client, "teste"), db.foodDao(), dispatcher)
    }

    private fun repositorySemRede(): OffRepository {
        val client = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return OffRepository(OffApi(client, "teste"), db.foodDao(), dispatcher)
    }

    @Test
    fun `um produto normal passa e fica com o id da origem`() = runTest(dispatcher) {
        val out = repositoryQueDevolve(produto("123", "Iogurte natural")).searchOnline("iogurte")

        assertEquals(1, out?.size)
        val food = out!!.single()
        assertEquals("off_123", food.id)
        assertEquals("Iogurte natural", food.namePt)
    }

    @Test
    fun `sem codigo de barras nao ha produto`() = runTest(dispatcher) {
        val out = repositoryQueDevolve(produto(null, "Iogurte natural")).searchOnline("iogurte")
        assertEquals(emptyList(), out)
    }

    @Test
    fun `sem nome fica de fora, para nao listar codigos de barras`() = runTest(dispatcher) {
        // Ao ler um código concreto o nome em falta é aceitável; numa lista de resultados
        // uma linha com o código não diz nada a ninguém.
        val out = repositoryQueDevolve(produto("123", null)).searchOnline("iogurte")
        assertEquals(emptyList(), out)
    }

    @Test
    fun `uma ficha sem macro nenhum e um formulario por preencher`() = runTest(dispatcher) {
        val out = repositoryQueDevolve(produto("123", "Iogurte natural", kcal = null))
            .searchOnline("iogurte")
        assertEquals(emptyList(), out)
    }

    @Test
    fun `um resultado sem relacao com a procura nao entra`() = runTest(dispatcher) {
        val out = repositoryQueDevolve(
            produto("1", "Iogurte natural"),
            produto("2", "Detergente da louça"),
        ).searchOnline("iogurte")

        assertEquals(listOf("off_1"), out?.map { it.id })
    }

    @Test
    fun `a marca tambem conta como relacao`() = runTest(dispatcher) {
        val out = repositoryQueDevolve(produto("1", "Skyr", marca = "Mimosa"))
            .searchOnline("mimosa")

        assertEquals(listOf("off_1"), out?.map { it.id })
    }

    @Test
    fun `acentos e maiusculas nao escondem o resultado`() = runTest(dispatcher) {
        val out = repositoryQueDevolve(produto("1", "Pão de Trigo"))
            .searchOnline("pao")

        assertEquals(listOf("off_1"), out?.map { it.id })
    }

    @Test
    fun `o mesmo codigo repetido aparece uma vez so`() = runTest(dispatcher) {
        val out = repositoryQueDevolve(
            produto("1", "Iogurte natural"),
            produto("1", "Iogurte natural"),
        ).searchOnline("iogurte")

        assertEquals(1, out?.size)
    }

    @Test
    fun `sem rede devolve nulo, e nao uma lista vazia`() = runTest(dispatcher) {
        val out = repositorySemRede().searchOnline("iogurte")

        // É esta diferença que faz o ecrã convidar a tentar outra vez em vez de dizer que
        // o produto não existe.
        assertNull(out, "uma falha de rede passou por 'não há resultados'")
    }

    @Test
    fun `sem resultados devolve lista vazia, e nao nulo`() = runTest(dispatcher) {
        val out = repositoryQueDevolve().searchOnline("iogurte")

        assertTrue(out != null && out.isEmpty())
    }
}
