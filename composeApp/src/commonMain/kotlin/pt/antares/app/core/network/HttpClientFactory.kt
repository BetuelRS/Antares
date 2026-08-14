package pt.antares.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Três limites em escada: ligar, receber cada pedaço, e o pedido inteiro. O último é o
// mais generoso porque a análise de uma fotografia demora mesmo dezenas de segundos.
private const val CONNECT_TIMEOUT_MS = 10_000L

private const val SOCKET_TIMEOUT_MS = 15_000L

private const val REQUEST_TIMEOUT_MS = 30_000L

fun createAntaresHttpClient(): HttpClient = HttpClient {
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
    }
    install(ContentNegotiation) {
        // Tolerante de propósito. A Open Food Facts é preenchida por voluntários: campos
        // inesperados, números em texto e nulos onde se espera valor são o normal, e uma
        // leitura estrita perdia o produto inteiro por causa de um campo.
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            },
        )
    }
    // `INFO` regista o método, o endereço e o estado, mas não os corpos — que levariam a
    // fotografia da refeição e o que a pessoa escreveu para o registo do sistema.
    install(Logging) {
        level = LogLevel.INFO
    }
}
