package pt.antares.app.feature.fooddata

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.FoodDao
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.fooddata.Barcode
import pt.antares.app.core.network.off.OffApi
import pt.antares.app.core.network.off.OffMapper
import pt.antares.app.core.util.FtsQuery
import pt.antares.app.core.util.TextNormalize

sealed interface OffFetch {
    data class Found(val food: FoodEntity) : OffFetch
    data object NotFound : OffFetch
    data object NetworkError : OffFetch
}

class OffRepository(
    private val api: OffApi,
    private val foodDao: FoodDao,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()

    suspend fun fetchAndCache(barcode: String): OffFetch = withContext(io) {

        var networkFailed = false
        for (code in Barcode.searchVariants(barcode)) {
            val response = runCatching { api.product(code) }.getOrNull()
            if (response == null) {
                networkFailed = true
                continue
            }
            val product = response.product
            if (response.status == 1 && product != null) {
                val food = OffMapper.toFood(product, Barcode.normalize(code) ?: code, now())
                cache(food)
                return@withContext OffFetch.Found(food)
            }
        }
        if (networkFailed) OffFetch.NetworkError else OffFetch.NotFound
    }

    suspend fun searchOnline(query: String): List<FoodEntity>? = withContext(io) {

        val response = runCatching { api.search(query) }.getOrNull() ?: return@withContext null
        val tokens = FtsQuery.tokens(query)

        response.products.asSequence()
            .mapNotNull { p ->
                val code = p.code?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

                val hasRealName = !p.productNamePt.isNullOrBlank() || !p.productName.isNullOrBlank()
                if (!hasRealName) return@mapNotNull null
                OffMapper.toFood(p, code, now())
            }

            .filter { it.kcal > 0 || it.proteinG > 0 || it.carbsG > 0 || it.fatG > 0 }

            .filter { food ->
                if (tokens.isEmpty()) return@filter true
                val name = TextNormalize.normalize("${food.namePt} ${food.brand.orEmpty()}")
                tokens.any { name.contains(it) }
            }
            .distinctBy { it.id }
            .toList()
    }

    private suspend fun cache(food: FoodEntity) {
        foodDao.upsertWithFts(
            food,
            TextNormalize.normalize("${food.namePt} ${food.nameEn} ${food.brand.orEmpty()}"),
        )
    }
}
