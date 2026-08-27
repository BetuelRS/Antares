package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodNutrientEntity

@Dao
interface FoodNutrientDao {

    @Upsert
    suspend fun upsertAll(rows: List<FoodNutrientEntity>)

    @Query("DELETE FROM food_nutrient WHERE foodId = :foodId")
    suspend fun clearFood(foodId: String)

    @Query("DELETE FROM food_nutrient")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM food_nutrient")
    suspend fun count(): Int

    /**
     * Alimentos ricos num nutriente, já ordenados por **densidade** — quanto do nutriente
     * por caloria — que é a ordem que o ecrã mostra.
     *
     * Os dois cortes vêm de fora em vez de estarem escritos aqui: dependem da referência
     * diária da pessoa, que muda com o sexo e com a fase da vida. Os valores saem das
     * constantes do `NutrientDensity`, que continua a ser o sítio onde a regra vive.
     *
     * - [minPer100Kcal] tira o que era preciso comer o dia inteiro para fazer diferença.
     * - [maxPer100g] tira suplementos, fortificados e especiarias: ganhariam sempre, e
     *   ninguém come 100 g de canela.
     */
    @Query(
        """
        SELECT f.* FROM foods f
        JOIN food_nutrient n ON n.foodId = f.id
        WHERE n.key = :key
          AND f.deleted = 0
          AND f.kcal > 0
          AND n.value > 0
          AND (n.value * 100.0 / f.kcal) >= :minPer100Kcal
          AND n.value <= :maxPer100g
        ORDER BY (n.value * 100.0 / f.kcal) DESC, f.namePt ASC
        LIMIT :limit
        """,
    )
    suspend fun richIn(
        key: String,
        minPer100Kcal: Double,
        maxPer100g: Double,
        limit: Int,
    ): List<FoodEntity>

    /**
     * Os índices que a `food_nutrient` tem, como o SQLite os declara.
     *
     * Existe para um teste-guarda, e é a única maneira honesta de o escrever nesta máquina:
     * a alternativa era comparar relógios, e uma comparação de relógio num servidor
     * partilhado mede a carga tanto quanto mede o código — esse teste já ficou vermelho duas
     * vezes por isso, a segunda no CI.
     *
     * Ler o `sqlite_master` não é intrometer-se em nada: é a mesma tabela que o Room lê para
     * conferir o esquema ao abrir.
     */
    @Query(
        "SELECT sql FROM sqlite_master WHERE type = 'index' AND tbl_name = 'food_nutrient' " +
            "AND sql IS NOT NULL",
    )
    suspend fun indicesDaTabela(): List<String>
}
