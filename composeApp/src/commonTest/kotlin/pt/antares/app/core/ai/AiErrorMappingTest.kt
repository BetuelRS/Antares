package pt.antares.app.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pt.antares.app.core.util.AppError
import kotlin.test.Test
import kotlin.test.assertEquals

class AiErrorMappingTest {

    private suspend fun errorFor(status: HttpStatusCode): AppError {
        val client = HttpClient(MockEngine { respond("erro", status, headersOf()) }) {
            expectSuccess = true
            install(HttpCallValidator)
        }
        return try {
            client.get("https://exemplo/fn")
            error("devia ter lançado")
        } catch (e: Exception) {
            e.toAiError()
        }
    }

    @Test
    fun `429 do servidor e quota, nao sem internet`() = runTest {
        assertEquals(AppError.QuotaExceeded, errorFor(HttpStatusCode.TooManyRequests))
    }

    @Test
    fun `402 e trial esgotado`() = runTest {
        assertEquals(AppError.QuotaExceeded, errorFor(HttpStatusCode.PaymentRequired))
    }

    @Test
    fun `401 e sem sessao`() = runTest {
        assertEquals(AppError.Unauthorized, errorFor(HttpStatusCode.Unauthorized))
    }

    @Test
    fun `404 da funcao nao publicada e erro conhecido, nao falta de rede`() = runTest {
        assertEquals(AppError.Unknown("ai 404"), errorFor(HttpStatusCode.NotFound))
    }

    @Test
    fun `502 do modelo em baixo`() = runTest {
        assertEquals(AppError.Unknown("ai 502"), errorFor(HttpStatusCode.BadGateway))
    }

    @Test
    fun `exceção sem resposta HTTP e mesmo falta de rede`() {

        assertEquals(AppError.Network, RuntimeException("connection refused").toAiError())
    }
}
