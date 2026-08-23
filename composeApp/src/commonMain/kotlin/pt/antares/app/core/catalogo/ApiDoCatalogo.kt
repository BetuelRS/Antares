package pt.antares.app.core.catalogo

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Onde o catálogo mora quando não está no telemóvel: uma release do GitHub.
 *
 * Sem infraestrutura nova e ao lado da versão que o produziu — quem quiser ver de onde veio
 * um número abre a release e encontra lá o ficheiro exacto que a app tem.
 *
 * **Só é chamada quando alguém carrega no botão.** Não há trabalho em fundo nem
 * agendamento, e é por isso que esta classe não tem estado nenhum.
 */
class ApiDoCatalogo(
    private val client: HttpClient,
    private val userAgent: String,
    private val urlDoManifesto: String = MANIFESTO,
) {

    // Lido à mão em vez de pelo `ContentNegotiation`: um ficheiro anexado a uma release é
    // servido como binário, e a negociação de conteúdo recusava-o por causa do cabeçalho.
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun manifesto(): ManifestoDoCatalogo =
        json.decodeFromString(pedir(urlDoManifesto).bodyAsText())

    suspend fun descarregar(url: String): ByteArray = pedir(url).bodyAsBytes()

    /**
     * O estado confere-se aqui e não mais à frente. Sem isto, uma página de erro do GitHub
     * descia como se fosse o catálogo e só era recusada pelo resumo — o que dizia à pessoa
     * «o ficheiro veio estragado» quando o que aconteceu foi não ter vindo ficheiro nenhum.
     */
    private suspend fun pedir(url: String): HttpResponse {
        val resposta = client.get(url) { header("User-Agent", userAgent) }
        if (!resposta.status.isSuccess()) error("$url respondeu ${resposta.status}")
        return resposta
    }

    companion object {
        /**
         * O `latest` é do GitHub e não da app: aponta sempre para a release mais recente,
         * e por isso o endereço nunca precisa de ser mudado aqui. Quem decide se o que está
         * lá serve é o [ActualizadorDoCatalogo], que compara versões.
         */
        const val MANIFESTO =
            "https://github.com/BetuelRS/Antares/releases/latest/download/manifesto.json"
    }
}
