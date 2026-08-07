package pt.antares.app.feature.fooddata

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.FoodDao
import pt.antares.app.core.database.daos.SearchMissDao
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.util.FtsQuery
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.core.util.todayEpochDay

class FoodRepository(
    private val foodDao: FoodDao,
    private val searchMissDao: SearchMissDao,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()

    suspend fun recordSearchMiss(canonicalQuery: String, today: Long = todayEpochDay()) =
        withContext(io) { searchMissDao.record(canonicalQuery, today) }

    suspend fun topSearchMisses(limit: Int = 20) = withContext(io) { searchMissDao.top(limit) }

    suspend fun clearSearchMisses() = withContext(io) { searchMissDao.clear() }

    suspend fun search(rawQuery: String): List<FoodEntity> = withContext(io) {

        val ftsQuery = FtsQuery.build(rawQuery)
        if (ftsQuery.isBlank()) return@withContext emptyList()
        foodDao.search(ftsQuery)
    }

    fun observeRecents(): Flow<List<FoodEntity>> = foodDao.observeRecents()
    fun observeFavorites(): Flow<List<FoodEntity>> = foodDao.observeFavorites()
    fun observeMyFoods(): Flow<List<FoodEntity>> = foodDao.observeMyFoods()

    fun observeMostLogged(limit: Int = 20, janelaDias: Long = 90): Flow<List<FoodEntity>> =
        foodDao.observeMostLogged(sinceEpochDay = todayEpochDay() - janelaDias, limit = limit)

    suspend fun byId(id: String): FoodEntity? = withContext(io) { foodDao.byId(id) }

    suspend fun foodsWithNutrient(key: String): List<FoodEntity> =
        withContext(io) { foodDao.foodsWithNutrient(key) }

    suspend fun byBarcode(barcode: String): FoodEntity? = withContext(io) { foodDao.byBarcode(barcode) }

    fun isBarcodeCacheStale(food: FoodEntity, now: Long): Boolean =
        food.source == FoodSource.OFF && now - food.updatedAt > BARCODE_CACHE_TTL_MS

    companion object {

        const val BARCODE_CACHE_TTL_MS = 90L * 24 * 60 * 60 * 1000
    }

    suspend fun toggleFavorite(food: FoodEntity) = withContext(io) {
        foodDao.setFavorite(food.id, !food.isFavorite, now())
    }

    suspend fun touchLastUsed(foodId: String, amountG: Double? = null) = withContext(io) {
        foodDao.touchLastUsed(foodId, now(), amountG)
    }

    suspend fun cacheOnline(food: FoodEntity) = withContext(io) {
        foodDao.upsertWithFts(
            food,
            TextNormalize.normalize("${food.namePt} ${food.nameEn} ${food.brand.orEmpty()}"),
        )
    }

    suspend fun upsertCustom(
        existingId: String?,
        namePt: String,
        kcal: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        sugarsG: Double?,
        satFatG: Double?,
        fiberG: Double?,
        sodiumMg: Int?,
        servingName: String?,
        servingGrams: Double?,
        barcode: String? = null,
    ): FoodEntity = withContext(io) {
        val existing = existingId?.let { foodDao.byId(it) }
        val food = FoodEntity(
            id = existing?.id ?: Ids.newUuid(),
            source = FoodSource.CUSTOM,

            sourceRef = barcode ?: existing?.sourceRef,
            namePt = namePt,
            nameEn = namePt,
            brand = null,
            kcal = kcal,
            proteinG = proteinG,
            carbsG = carbsG,
            sugarsG = sugarsG,
            fatG = fatG,
            satFatG = satFatG,
            fiberG = fiberG,
            sodiumMg = sodiumMg,
            microsJson = existing?.microsJson,
            servingName = servingName,
            servingGrams = servingGrams,
            isFavorite = existing?.isFavorite ?: false,
            lastUsedAt = existing?.lastUsedAt ?: 0L,
            verified = true,
            updatedAt = now(),
            dirty = true,
        )
        foodDao.upsertWithFts(food, TextNormalize.normalize("${food.namePt} ${food.nameEn}"))
        food
    }
}
