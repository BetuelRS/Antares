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
import pt.antares.app.core.database.daos.FoodNutrientDao
import pt.antares.app.core.database.entities.FoodNutrientEntity
import pt.antares.app.core.nutrition.NutrientDensity
import pt.antares.app.core.nutrition.microsDeJson

class FoodRepository(
    private val foodDao: FoodDao,
    private val nutrientDao: FoodNutrientDao,
    private val searchMissDao: SearchMissDao,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()

    suspend fun recordSearchMiss(canonicalQuery: String, today: Long = todayEpochDay()) =
        withContext(io) { searchMissDao.record(canonicalQuery, today) }

    suspend fun topSearchMisses(limit: Int = 20) = withContext(io) { searchMissDao.top(limit) }

    suspend fun clearSearchMisses() = withContext(io) { searchMissDao.clear() }

    /**
     * Pesquisa no catálogo local. Devolve vazio para uma pesquisa que só tem pontuação ou
     * palavras de uma letra: sem isto, a expressão FTS saía malformada e a consulta
     * rebentava em vez de não encontrar nada.
     */
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

    /**
     * Alimentos ricos num nutriente, já ordenados por densidade e já sem os que a regra
     * exclui. Antes disto vinham até 1500 alimentos inteiros para memória, a filtrar com
     * `LIKE` dentro do JSON; agora é uma junção por índice, e o limite passa a ser o
     * comprimento da lista que se mostra em vez de um teto para a memória aguentar.
     *
     * Os cortes vêm das constantes do [NutrientDensity], convertidos aqui em valores
     * absolutos porque dependem da referência diária de quem está a perguntar.
     */
    suspend fun foodsWithNutrient(key: String, drv: Double, limit: Int): List<FoodEntity> =
        withContext(io) {
            if (drv <= 0) return@withContext emptyList()
            nutrientDao.richIn(
                key = key,
                minPer100Kcal = drv * NutrientDensity.MIN_PER_KCAL_PCT / 100.0,
                maxPer100g = drv * NutrientDensity.MAX_PER_100G_PCT / 100.0,
                limit = limit,
            )
        }

    suspend fun byBarcode(barcode: String): FoodEntity? = withContext(io) { foodDao.byBarcode(barcode) }

    /**
     * Se vale a pena ir buscar o produto outra vez à Open Food Facts. Só se aplica ao que
     * veio de lá: um alimento do catálogo ou criado à mão nunca fica velho.
     */
    fun isBarcodeCacheStale(food: FoodEntity, now: Long): Boolean =
        food.source == FoodSource.OFF && now - food.updatedAt > BARCODE_CACHE_TTL_MS

    companion object {

        // Três meses. Os rótulos mudam devagar, e ler o código de barras tem de funcionar
        // sem rede — o que está em cache é preferível a não ter nada.
        const val BARCODE_CACHE_TTL_MS = 90L * 24 * 60 * 60 * 1000
    }

    suspend fun toggleFavorite(food: FoodEntity) = withContext(io) {
        foodDao.setFavorite(food.id, !food.isFavorite, now())
    }

    suspend fun touchLastUsed(foodId: String, amountG: Double? = null) = withContext(io) {
        foodDao.touchLastUsed(foodId, now(), amountG)
    }

    /**
     * Guarda um produto vindo da Open Food Facts. A marca entra no texto de pesquisa — é
     * por ela que se procuram produtos de embalagem, e não pelo nome genérico.
     */
    suspend fun cacheOnline(food: FoodEntity) = withContext(io) {
        guardarComIndice(
            food,
            TextNormalize.normalize("${food.namePt} ${food.nameEn} ${food.brand.orEmpty()}"),
        )
    }

    /**
     * Grava o alimento e reescreve as linhas dele na `food_nutrient`.
     *
     * Existe para o índice não descolar do JSON: um produto lido de um código de barras
     * traz micronutrientes, e sem isto ficava fora do «rico em» até à próxima sementeira
     * — ou seja, para sempre.
     */
    private suspend fun guardarComIndice(food: FoodEntity, searchText: String) {
        foodDao.upsertWithFts(food, searchText)
        nutrientDao.clearFood(food.id)
        val linhas = microsDeJson(food.microsJson)
            .filterValues { it > 0 }
            .map { (chave, valor) -> FoodNutrientEntity(food.id, chave, valor) }
        if (linhas.isNotEmpty()) nutrientDao.upsertAll(linhas)
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

            // Os campos que se mantêm do que já existia — código de barras, micronutrientes,
            // favorito, último uso — são os que o formulário não pergunta. Sem isto, editar
            // o nome de um alimento apagava-lhe a análise e tirava-o dos favoritos.
            sourceRef = barcode ?: existing?.sourceRef,
            // Um alimento próprio tem um nome só, repetido nos dois campos: o índice de
            // pesquisa lê ambos, e deixar o inglês vazio tirava-o de metade das pesquisas.
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
            // Verificado por definição: os números são da pessoa, e não há mais ninguém a
            // quem os confirmar.
            verified = true,
            updatedAt = now(),
            dirty = true,
        )
        guardarComIndice(food, TextNormalize.normalize("${food.namePt} ${food.nameEn}"))
        food
    }
}
