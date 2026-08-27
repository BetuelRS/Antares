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
import pt.antares.app.core.database.entities.FoodMarkEntity
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

/**
 * O que a pessoa deixou sobre um alimento: o favorito, o uso e a porção.
 *
 * Tabela à parte desde a v27. Enquanto vivia dentro da linha do alimento, escrever o
 * catálogo por cima apagava-a — e não ia na cópia de segurança, porque o catálogo não se
 * exporta. Ver [pt.antares.app.core.database.entities.FoodMarkEntity].
 */
@Dao
interface FoodMarkDao {

    @Upsert
    suspend fun upsert(marca: FoodMarkEntity)

    @Query("SELECT * FROM food_marca WHERE foodId = :foodId")
    suspend fun byFoodId(foodId: String): FoodMarkEntity?

    /**
     * Marca ou desmarca como favorito sem tocar no resto. O `INSERT … ON CONFLICT` existe
     * porque a marca pode ainda não existir: um alimento que nunca foi usado nem marcado
     * não tem linha nenhuma aqui, e é isso que faz a tabela ter dezenas de linhas em vez
     * de oito mil.
     */
    @Query(
        """
        INSERT INTO food_marca (foodId, isFavorite, lastUsedAt, lastAmountG, updatedAt, deleted)
        VALUES (:foodId, :favorito, 0, NULL, :agora, 0)
        ON CONFLICT(foodId) DO UPDATE SET
            isFavorite = :favorito, updatedAt = :agora, deleted = 0
        """,
    )
    suspend fun marcarFavorito(foodId: String, favorito: Boolean, agora: Long)

    // O COALESCE preserva a última quantidade quando a chamada não traz nenhuma: marcar o
    // uso não pode apagar a porção que a app tinha aprendido.
    @Query(
        """
        INSERT INTO food_marca (foodId, isFavorite, lastUsedAt, lastAmountG, updatedAt, deleted)
        VALUES (:foodId, 0, :agora, :gramas, :agora, 0)
        ON CONFLICT(foodId) DO UPDATE SET
            lastUsedAt = :agora,
            lastAmountG = COALESCE(:gramas, lastAmountG),
            updatedAt = :agora,
            deleted = 0
        """,
    )
    suspend fun marcarUso(foodId: String, agora: Long, gramas: Double? = null)

    /**
     * Manda a marca seguir o alimento que foi fundido noutro.
     *
     * O `OR REPLACE` é preciso porque a pessoa pode ter marca nos dois — o favorito no
     * antigo e um uso recente no novo. Fica a do sucessor, que é a que aponta para o
     * alimento que continua a existir; a outra vai-se embora com ele.
     *
     * **Sem isto, fundir dois alimentos tirava um favorito a alguém sem aviso.**
     */
    @Query("UPDATE OR REPLACE food_marca SET foodId = :sucessor WHERE foodId = :antigo")
    suspend fun seguir(antigo: String, sucessor: String)

    @Query("SELECT * FROM food_marca WHERE deleted = 0")
    suspend fun exportRows(): List<FoodMarkEntity>

    @Query("SELECT COUNT(*) FROM food_marca")
    suspend fun count(): Int
}

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
        LEFT JOIN food_marca m ON m.foodId = f.id
        WHERE foods_fts MATCH :ftsQuery AND f.deleted = 0
        ORDER BY
            COALESCE(m.isFavorite, 0) DESC,
            COALESCE(m.lastUsedAt, 0) DESC,
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

    /**
     * Apaga o que ficou da versão anterior do catálogo: o que não foi reescrito agora tem
     * o `updatedAt` para trás do instante desta instalação.
     *
     * As condições finais são a regra que atravessa o ficheiro todo — **nunca apagar um
     * alimento que a pessoa tocou.** Tocar é ter deixado uma marca — favorito, uso ou
     * porção guardada — ou aparecer num registo do diário, numa receita ou numa
     * refeição-tipo, mesmo apagados.
     *
     * Alimentos criados pela pessoa, vindos da Open Food Facts ou estimados por AI não são
     * catálogo e nunca entram aqui.
     */
    @Query(
        """
        DELETE FROM foods
        WHERE source = 'SEED'
          AND updatedAt < :instaladoEm
          AND id NOT IN (SELECT foodId FROM food_marca)
          AND id NOT IN (SELECT foodId FROM food_log WHERE foodId IS NOT NULL)
          AND id NOT IN (SELECT foodId FROM recipe_ingredient)
          AND id NOT IN (SELECT foodId FROM meal_template_item WHERE foodId IS NOT NULL)
        """,
    )
    suspend fun podarCatalogoAnterior(instaladoEm: Long): Int

    // O FTS não tem chave estrangeira: apagar um alimento deixa a linha de pesquisa para
    // trás, e essa linha continuaria a aparecer nos resultados sem alimento por trás.
    @Query("DELETE FROM foods_fts WHERE foodId NOT IN (SELECT id FROM foods)")
    suspend fun pruneOrphanFts(): Int

    @Query("DELETE FROM foods_fts WHERE foodId IN (:ids)")
    suspend fun deleteFtsIn(ids: List<String>)

    /**
     * Manda os ingredientes de receita seguirem o alimento fundido.
     *
     * Uma receita é a coisa que mais tempo custa a escrever nesta app, e um ingrediente que
     * desaparece dela não se recupera: a receita fica com menos comida do que tem, e a conta
     * do dia passa a estar errada por omissão — que é o erro mais difícil de notar.
     */
    @Query("UPDATE recipe_ingredient SET foodId = :sucessor WHERE foodId = :antigo")
    suspend fun seguirEmReceitas(antigo: String, sucessor: String)

    /** O mesmo para as refeições guardadas. */
    @Query("UPDATE meal_template_item SET foodId = :sucessor WHERE foodId = :antigo")
    suspend fun seguirEmRefeicoes(antigo: String, sucessor: String)

    // A junção é para dentro (`JOIN`, não `LEFT JOIN`): recente é quem tem marca de uso, e
    // um alimento sem marca nenhuma não pode aparecer aqui.
    @Query(
        """
        SELECT f.* FROM foods f
        JOIN food_marca m ON m.foodId = f.id
        WHERE f.deleted = 0 AND m.deleted = 0 AND m.lastUsedAt > 0
        ORDER BY m.lastUsedAt DESC
        LIMIT 30
        """,
    )
    fun observeRecents(): Flow<List<FoodEntity>>

    @Query(
        """
        SELECT f.* FROM foods f
        JOIN (
            SELECT foodId, COUNT(*) AS n FROM food_log
            WHERE deleted = 0 AND foodId IS NOT NULL AND epochDay >= :sinceEpochDay
            GROUP BY foodId
        ) u ON u.foodId = f.id
        LEFT JOIN food_marca m ON m.foodId = f.id
        WHERE f.deleted = 0
        ORDER BY u.n DESC, COALESCE(m.lastUsedAt, 0) DESC
        LIMIT :limit
        """,
    )
    fun observeMostLogged(sinceEpochDay: Long, limit: Int = 20): Flow<List<FoodEntity>>

    @Query(
        """
        SELECT f.* FROM foods f
        JOIN food_marca m ON m.foodId = f.id
        WHERE f.deleted = 0 AND m.deleted = 0 AND m.isFavorite = 1
        ORDER BY f.namePt ASC
        """,
    )
    fun observeFavorites(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE deleted = 0 AND source = 'CUSTOM' ORDER BY namePt ASC")
    fun observeMyFoods(): Flow<List<FoodEntity>>

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

    @Query("UPDATE food_log SET deleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("SELECT * FROM food_log WHERE id = :id AND deleted = 0")
    suspend fun byId(id: String): FoodLogEntity?

    /**
     * Os caminhos de fotografia que ainda são referidos por alguma linha — **incluindo as
     * apagadas**.
     *
     * O `deleted = 0` está de fora de propósito, e é a única consulta deste ficheiro onde
     * isso é certo: apagar um registo é desfazível, e um ficheiro apagado não volta. Uma
     * lápide ainda vale como razão para a imagem ficar.
     */
    @Query("SELECT DISTINCT photoPath FROM food_log WHERE photoPath IS NOT NULL")
    suspend fun caminhosDeFoto(): List<String>

    /**
     * Esquece as fotografias mais antigas do que um dia. Não toca no `updatedAt`: é por ele
     * que o diário ordena as refeições, e uma varredura que o mexesse baralhava a ordem do
     * dia sem ninguém ter tocado em nada.
     */
    @Query("UPDATE food_log SET photoPath = NULL WHERE photoPath IS NOT NULL AND epochDay < :antesDe")
    suspend fun esquecerFotosAntesDe(antesDe: Long): Int

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
