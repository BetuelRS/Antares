package pt.antares.app.core.network.off

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

class OffApi(private val client: HttpClient) {

    suspend fun product(barcode: String): OffProductResponse =
        client.get("$BASE/api/v2/product/$barcode.json") {
            header("User-Agent", USER_AGENT)
        }.body()

    suspend fun search(query: String, pageSize: Int = 40): OffSearchResponse =
        client.get("$BASE/api/v2/search") {
            header("User-Agent", USER_AGENT)
            parameter("search_terms", query)
            parameter("fields", FIELDS)
            parameter("page_size", pageSize)

            parameter("lc", "pt")
            parameter("json", 1)
        }.body()

    companion object {
        private const val BASE = "https://world.openfoodfacts.org"

        const val USER_AGENT = "Antares/0.1.0 (betuel801@gmail.com)"
        private const val FIELDS =
            "code,product_name,product_name_pt,brands,serving_quantity,nutriments"
    }
}
