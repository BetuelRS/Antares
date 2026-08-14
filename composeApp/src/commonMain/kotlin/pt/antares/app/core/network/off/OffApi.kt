package pt.antares.app.core.network.off

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

/**
 * A Open Food Facts, a única fonte de alimentos que a app consulta em linha. Serve o que o
 * catálogo local não tem: produtos de marca, encontrados por código de barras.
 *
 * O `userAgent` vem de fora porque a Open Food Facts exige nele o nome e a versão de quem
 * chama, e a versão tem uma fonte única — ver o [coreModule]. Escrevê-la aqui punha um
 * número a envelhecer sozinho, longe do único sítio onde as versões vivem.
 */
class OffApi(
    private val client: HttpClient,
    private val userAgent: String,
) {

    suspend fun product(barcode: String): OffProductResponse =
        client.get("$BASE/api/v2/product/$barcode.json") {
            header("User-Agent", userAgent)
        }.body()

    suspend fun search(query: String, pageSize: Int = 40): OffSearchResponse =
        client.get("$BASE/api/v2/search") {
            header("User-Agent", userAgent)
            parameter("search_terms", query)
            parameter("fields", FIELDS)
            parameter("page_size", pageSize)

            // Idioma fixo em português, e não o do telemóvel: decide qual dos nomes do
            // produto vem primeiro, e o catálogo da app é português.
            parameter("lc", "pt")
            parameter("json", 1)
        }.body()

    companion object {
        private const val BASE = "https://world.openfoodfacts.org"

        // A Open Food Facts exige nome e contacto em todos os pedidos, sob pena de bloquear.
        // É por isso que existe um endereço de e-mail no `User-Agent` montado no [coreModule].
        const val CONTACT = "betuel801@gmail.com"
        // Pede-se só o que a app usa: a resposta completa de um produto traz centenas de
        // campos, e a pesquisa devolve dezenas de produtos de cada vez.
        private const val FIELDS =
            "code,product_name,product_name_pt,brands,serving_quantity,nutriments"
    }
}
