package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodFtsEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.model.MealSlot

data class DayTotals(
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)

data class DayKcal(val epochDay: Long, val kcal: Int)

data class FoodNameRow(
    val id: String,
    val namePt: String,
    val nameEn: String,
    val brand: String?,
)

@Dao
interface FoodDao {

    @Upsert
    suspend fun upsert(food: FoodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFtsAll(rows: List<FoodFtsEntity>)

    @Query("DELETE FROM foods_fts WHERE foodId = :foodId")
    suspend fun deleteFts(foodId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(row: FoodFtsEntity)

    /**
     * O alimento e o seu índice de pesquisa numa transação só. O FTS4 não tem chave
     * primária, por isso a atualização é apagar e reinserir — e ficar a meio deixava o
     * alimento fora da pesquisa ou duplicado nela.
     */
    @Transaction
    suspend fun upsertWithFts(food: FoodEntity, searchText: String) {
        upsert(food)
        deleteFts(food.id)
        insertFts(FoodFtsEntity(foodId = food.id, searchText = searchText))
    }

    @Query("SELECT id, namePt, nameEn, brand FROM foods WHERE deleted = 0")
    suspend fun nameRows(): List<FoodNameRow>

    @Query("SELECT id, namePt, nameEn, brand FROM foods WHERE id LIKE 'usda-%' AND deleted = 0")
    suspend fun usdaNameRows(): List<FoodNameRow>

    @Query("UPDATE foods SET isLiquid = 1 WHERE id IN (:ids)")
    suspend fun markLiquid(ids: List<String>)

    @Query("UPDATE foods SET isLiquid = 0 WHERE isLiquid = 1")
    suspend fun clearAllLiquid()

    @Transaction
    suspend fun setDisplayNameWithFts(id: String, name: String, searchText: String) {
        setDisplayName(id, name)
        deleteFts(id)
        insertFts(FoodFtsEntity(foodId = id, searchText = searchText))
    }

    @Query("UPDATE foods SET namePt = :name WHERE id = :id")
    suspend fun setDisplayName(id: String, name: String)

    @Query("UPDATE foods SET microsJson = :json WHERE id = :id")
    suspend fun setMicros(id: String, json: String)

    @Query("SELECT * FROM foods WHERE id = :id AND deleted = 0")
    suspend fun byId(id: String): FoodEntity?

    /**
     * Só o que é preciso para encher a `food_nutrient`. Trazer os alimentos inteiros para
     * memória para ler duas colunas é exatamente o que essa tabela existe para evitar.
     */
    @Query("SELECT id, microsJson FROM foods WHERE microsJson IS NOT NULL AND deleted = 0")
    suspend fun microsParaIndexar(): List<FoodMicrosRow>

    @Query("SELECT * FROM foods WHERE sourceRef = :barcode AND deleted = 0 LIMIT 1")
    suspend fun byBarcode(barcode: String): FoodEntity?

    /**
     * A pesquisa de alimentos. A ordem é a decisão toda deste ficheiro, e lê-se de cima
     * para baixo: o que a pessoa marcou vem primeiro, depois o que usou há pouco, e só
     * então o catálogo — dentro dele, os alimentos portugueses antes dos importados, e
     * os que trazem micronutrientes antes dos que não trazem. O nome mais curto ganha em
     * último recurso, porque é quase sempre o genérico e não a variante.
     */
    @Query(
        """
        SELECT f.* FROM foods f
        JOIN foods_fts s ON s.foodId = f.id
        WHERE foods_fts MATCH :ftsQuery AND f.deleted = 0
        ORDER BY
            f.isFavorite DESC,
            f.lastUsedAt DESC,
            (CASE
                WHEN f.id LIKE 'pt-%' OR f.id LIKE 'ptx%' OR f.source = 'CUSTOM' THEN 0
                WHEN f.id LIKE 'ciqual-%' THEN 1
                ELSE 2
             END) ASC,
            (CASE WHEN f.microsJson IS NULL THEN 1 ELSE 0 END) ASC,
            length(f.namePt) ASC,
            f.namePt ASC
        LIMIT :limit
        """,
    )
    suspend fun search(ftsQuery: String, limit: Int = 50): List<FoodEntity>

    @Query(
        """
        UPDATE foods SET namePt = nameEn
        WHERE id LIKE 'usda-%'
        """,
    )
    suspend fun cleanUsdaDisplayNames(): Int

    /**
     * Limpa alimentos importados que a versão nova do catálogo já não traz. As quatro
     * condições finais repetem-se em todas as limpezas deste ficheiro e são a mesma regra:
     * nunca apagar um alimento que a pessoa tocou — favorito, usado, com porção guardada,
     * ou presente em qualquer registo do diário, mesmo apagado.
     */
    @Query(
        """
        DELETE FROM foods
        WHERE id LIKE 'usda-%'
          AND updatedAt < :importedAt
          AND isFavorite = 0
          AND lastUsedAt = 0
          AND lastAmountG IS NULL
          AND id NOT IN (SELECT foodId FROM food_log WHERE foodId IS NOT NULL)
        """,
    )
    suspend fun pruneStaleUsda(importedAt: Long): Int

    // Quando a mesma comida existe nas duas listas portuguesas, fica a `ptx`, que é a
    // revista; o nome é o único critério possível porque os identificadores não coincidem.
    @Query(
        """
        DELETE FROM foods
        WHERE id LIKE 'pt-%'
          AND namePt IN (SELECT namePt FROM foods WHERE id LIKE 'ptx%')
          AND isFavorite = 0
          AND lastUsedAt = 0
          AND lastAmountG IS NULL
          AND id NOT IN (SELECT foodId FROM food_log WHERE foodId IS NOT NULL)
        """,
    )
    suspend fun pruneDuplicateCurated(): Int

    @Query("UPDATE foods SET verified = 1 WHERE (id LIKE 'ciqual-%' OR id LIKE 'usda-%') AND deleted = 0")
    suspend fun markAnalysedAsVerified(): Int

    @Query(
        """
        DELETE FROM foods
        WHERE id IN (:ids)
          AND isFavorite = 0
          AND lastUsedAt = 0
          AND lastAmountG IS NULL
          AND id NOT IN (SELECT foodId FROM food_log WHERE foodId IS NOT NULL)
        """,
    )
    suspend fun pruneByIds(ids: List<String>): Int

    @Query("SELECT id, namePt FROM foods WHERE deleted = 0 AND (id LIKE 'pt-%' OR id LIKE 'ptx%')")
    suspend fun curatedIdsAndNames(): List<FoodIdName>

    @Query("SELECT id, namePt FROM foods WHERE deleted = 0 AND id LIKE 'tca-%'")
    suspend fun tcaIdsAndNames(): List<FoodIdName>

    // O FTS não tem chave estrangeira: apagar um alimento deixa a linha de pesquisa para
    // trás, e essa linha continuaria a aparecer nos resultados sem alimento por trás.
    @Query("DELETE FROM foods_fts WHERE foodId NOT IN (SELECT id FROM foods)")
    suspend fun pruneOrphanFts(): Int

    @Query("DELETE FROM foods_fts WHERE foodId IN (:ids)")
    suspend fun deleteFtsIn(ids: List<String>)

    // Guarda a primeira linha de cada alimento e apaga as outras. O `docid` é a coluna
    // interna do FTS4, a única identidade que estas linhas têm.
    @Query(
        """
        DELETE FROM foods_fts
        WHERE docid NOT IN (SELECT MIN(docid) FROM foods_fts GROUP BY foodId)
        """,
    )
    suspend fun dedupeFts(): Int

    @Query("SELECT * FROM foods WHERE deleted = 0 AND lastUsedAt > 0 ORDER BY lastUsedAt DESC LIMIT 30")
    fun observeRecents(): Flow<List<FoodEntity>>

    @Query(
        """
        SELECT f.* FROM foods f
        JOIN (
            SELECT foodId, COUNT(*) AS n FROM food_log
            WHERE deleted = 0 AND foodId IS NOT NULL AND epochDay >= :sinceEpochDay
            GROUP BY foodId
        ) u ON u.foodId = f.id
        WHERE f.deleted = 0
        ORDER BY u.n DESC, f.lastUsedAt DESC
        LIMIT :limit
        """,
    )
    fun observeMostLogged(sinceEpochDay: Long, limit: Int = 20): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE deleted = 0 AND isFavorite = 1 ORDER BY namePt ASC")
    fun observeFavorites(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE deleted = 0 AND source = 'CUSTOM' ORDER BY namePt ASC")
    fun observeMyFoods(): Flow<List<FoodEntity>>

    @Query("UPDATE foods SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    // O COALESCE preserva a última quantidade quando a chamada não traz nenhuma: marcar o
    // uso não pode apagar a porção que a app tinha aprendido.
    @Query("UPDATE foods SET lastUsedAt = :now, lastAmountG = COALESCE(:amountG, lastAmountG) WHERE id = :id")
    suspend fun touchLastUsed(id: String, now: Long, amountG: Double? = null)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Query("SELECT * FROM foods WHERE deleted = 0 AND source = 'CUSTOM'")
    suspend fun exportRows(): List<FoodEntity>
}

@Dao
interface FoodLogDao {

    @Upsert
    suspend fun upsert(log: FoodLogEntity)

    // Apagar é marcar. A linha fica, e é por isso que todas as consultas daqui para baixo
    // filtram `deleted = 0`; esquecer o filtro devolve comida que a pessoa já tirou do dia.
    @Query("UPDATE food_log SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM food_log WHERE id = :id AND deleted = 0")
    suspend fun byId(id: String): FoodLogEntity?

    // Ordena por `updatedAt` e não por hora da refeição: a app não pergunta a que horas se
    // comeu, e a ordem de registo é a única cronologia que existe.
    @Query("SELECT * FROM food_log WHERE deleted = 0 AND epochDay = :epochDay ORDER BY updatedAt ASC")
    fun observeDay(epochDay: Long): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_log WHERE deleted = 0 AND epochDay = :epochDay")
    suspend fun dayLogs(epochDay: Long): List<FoodLogEntity>

    @Query(
        """
        SELECT quantityGrams FROM food_log
        WHERE deleted = 0 AND foodId = :foodId
        ORDER BY epochDay DESC, updatedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun recentAmounts(foodId: String, limit: Int = 12): List<Double>

    @Query(
        """
        SELECT epochDay, COALESCE(SUM(kcalSnapshot), 0) AS kcal
        FROM food_log
        WHERE deleted = 0 AND epochDay BETWEEN :from AND :to
        GROUP BY epochDay
        """,
    )
    fun observeDailyKcal(from: Long, to: Long): Flow<List<DayKcal>>

    @Query("SELECT * FROM food_log WHERE deleted = 0 AND epochDay BETWEEN :from AND :to")
    suspend fun logsInRange(from: Long, to: Long): List<FoodLogEntity>

    @Query("SELECT * FROM food_log WHERE deleted = 0 AND epochDay = :epochDay AND mealSlot = :slot")
    suspend fun mealLogs(epochDay: Long, slot: MealSlot): List<FoodLogEntity>

    // Suporta o "repetir refeição": `< :day` para o dia que se está a preencher nunca se
    // oferecer a si próprio.
    @Query(
        """
        SELECT MAX(epochDay) FROM food_log
        WHERE deleted = 0 AND mealSlot = :slot AND epochDay < :day
        """,
    )
    suspend fun lastDayWithMeal(slot: MealSlot, day: Long): Long?

    @Query(
        """
        SELECT DISTINCT epochDay FROM food_log
        WHERE deleted = 0 AND mealSlot = :slot AND epochDay < :day
        ORDER BY epochDay DESC
        LIMIT :limit
        """,
    )
    suspend fun recentDaysWithMeal(slot: MealSlot, day: Long, limit: Int = 10): List<Long>

    // Os COALESCE existem porque SUM devolve NULL num dia sem registos, e [DayTotals] não
    // tem campos anuláveis — sem eles a leitura de um dia em branco rebentava.
    @Query(
        """
        SELECT COALESCE(SUM(kcalSnapshot), 0) AS kcal,
               COALESCE(SUM(proteinSnapshot), 0) AS proteinG,
               COALESCE(SUM(carbsSnapshot), 0) AS carbsG,
               COALESCE(SUM(fatSnapshot), 0) AS fatG
        FROM food_log WHERE deleted = 0 AND epochDay = :epochDay
        """,
    )
    fun observeDayTotals(epochDay: Long): Flow<DayTotals>

    @Query("SELECT DISTINCT mealSlot FROM food_log WHERE deleted = 0 AND epochDay = :epochDay")
    suspend fun loggedSlots(epochDay: Long): List<MealSlot>

    @Query("SELECT DISTINCT epochDay FROM food_log WHERE deleted = 0 AND epochDay >= :fromEpochDay")
    suspend fun loggedDaysSince(fromEpochDay: Long): List<Long>

    @Query("SELECT DISTINCT epochDay FROM food_log WHERE deleted = 0 AND epochDay >= :fromEpochDay")
    fun observeLoggedDaysSince(fromEpochDay: Long): Flow<List<Long>>

    @Query(
        """
        SELECT COALESCE(SUM(kcalSnapshot), 0) AS kcal,
               COALESCE(SUM(proteinSnapshot), 0) AS proteinG,
               COALESCE(SUM(carbsSnapshot), 0) AS carbsG,
               COALESCE(SUM(fatSnapshot), 0) AS fatG
        FROM food_log WHERE deleted = 0 AND epochDay = :epochDay
        """,
    )
    suspend fun dayTotals(epochDay: Long): DayTotals

    @Query("SELECT * FROM food_log WHERE deleted = 0")
    suspend fun exportRows(): List<FoodLogEntity>
}

@Dao
interface WaterLogDao {

    @Upsert
    suspend fun upsert(entry: WaterLogEntity)

    @Query("SELECT * FROM water_log WHERE deleted = 0 AND epochDay = :epochDay")
    fun observeDay(epochDay: Long): Flow<WaterLogEntity?>

    @Query("SELECT * FROM water_log WHERE deleted = 0 AND epochDay = :epochDay")
    suspend fun byDay(epochDay: Long): WaterLogEntity?

    /**
     * A mesma consulta sem filtrar lápides, e é essa a razão de existir. O índice único
     * em `epochDay` não distingue apagados: quem vai escrever tem de encontrar a linha
     * morta e reaproveitá-la, senão a inserção falha contra uma linha que já não se vê.
     */
    @Query("SELECT * FROM water_log WHERE epochDay = :epochDay")
    suspend fun byDayForWrite(epochDay: Long): WaterLogEntity?

    @Query("SELECT * FROM water_log WHERE deleted = 0")
    suspend fun exportRows(): List<WaterLogEntity>
}

data class FoodIdName(val id: String, val namePt: String)

data class FoodMicrosRow(val id: String, val microsJson: String?)
