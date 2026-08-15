package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodNutrientEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.nutrition.NutrientDensity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime

/**
 * A `food_nutrient` é a mesma informação do `microsJson` virada ao contrário, para
 * responder à pergunta oposta: *que alimentos têm este nutriente?*
 *
 * Antes disto, essa pergunta era um `LIKE` dentro do JSON sobre a tabela toda, com um teto
 * de 1500 linhas — e o comentário no código admitia-o: «não há índice possível — e daí o
 * limite». Este teste mede as duas maneiras sobre o mesmo catálogo, e fixa o que a nova
 * tem de continuar a fazer bem.
 */
@RunWith(RobolectricTestRunner::class)
class FoodNutrientIndexTest {

    private val db: AntaresDb = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AntaresDb::class.java,
    ).setQueryCoroutineContext(Dispatchers.Default).build()

    @AfterTest
    fun tearDown() = db.close()

    private val json = Json

    /**
     * Um catálogo do tamanho do verdadeiro, com um alimento em cada dez a declarar ferro.
     * Os valores sobem com o índice para a ordem por densidade ser previsível.
     */
    private suspend fun catalogo(quantos: Int) {
        val alimentos = (0 until quantos).map { i ->
            val micros = if (i % 10 == 0) mapOf(FERRO to 1.0 + i / 1000.0) else null
            FoodEntity(
                id = "f$i",
                source = FoodSource.SEED,
                sourceRef = null,
                namePt = "Alimento $i",
                nameEn = "Food $i",
                brand = null,
                kcal = 100,
                proteinG = 1.0,
                carbsG = 1.0,
                fatG = 1.0,
                sugarsG = null,
                satFatG = null,
                fiberG = null,
                sodiumMg = null,
                microsJson = micros?.let { json.encodeToString(it) },
                servingName = null,
                servingGrams = null,
                updatedAt = 1_000,
            )
        }
        alimentos.chunked(500).forEach { db.foodDao().insertAll(it) }

        val linhas = alimentos.flatMap { alimento ->
            alimento.microsJson
                ?.let { json.decodeFromString<Map<String, Double>>(it) }
                ?.map { (k, v) -> FoodNutrientEntity(alimento.id, k, v) }
                ?: emptyList()
        }
        linhas.chunked(500).forEach { db.foodNutrientDao().upsertAll(it) }
    }

    @Test
    fun `a consulta indexada devolve os mais densos primeiro`() = runTest {
        catalogo(1_000)

        val ricos = db.foodNutrientDao().richIn(
            key = FERRO,
            minPer100Kcal = 0.0,
            maxPer100g = Double.MAX_VALUE,
            limit = NutrientDensity.LIST_LIMIT,
        )

        assertEquals(NutrientDensity.LIST_LIMIT, ricos.size)
        assertEquals(
            "f990",
            ricos.first().id,
            "a ordem não é por densidade — e é a densidade que põe espinafres e amêndoas " +
                "na mesma escala",
        )
    }

    @Test
    fun `os cortes vem de fora e sao respeitados`() = runTest {
        catalogo(100)

        // Dez alimentos declaram ferro, de 1,00 a 1,09 por 100 g, todos com 100 kcal — o
        // que dá densidades de 1,00 a 1,09 por 100 kcal. Só o último passa este corte.
        val comCorteAlto = db.foodNutrientDao().richIn(FERRO, minPer100Kcal = 1.085, maxPer100g = 1e9, limit = 50)
        assertEquals(1, comCorteAlto.size, "o corte por densidade não filtrou")

        val comTetoBaixo = db.foodNutrientDao().richIn(FERRO, minPer100Kcal = 0.0, maxPer100g = 1.05, limit = 50)
        assertTrue(
            comTetoBaixo.size < 10,
            "o teto por 100 g não filtrou — é ele que tira suplementos e especiarias",
        )
    }

    @Test
    fun `um nutriente que ninguem declara devolve vazio, sem varrer nada`() = runTest {
        catalogo(1_000)
        assertTrue(db.foodNutrientDao().richIn("vitB12_ug", 0.0, 1e9, 50).isEmpty())
    }

    @Test
    fun `um produto guardado de um codigo de barras entra logo no indice`() = runTest {
        val repo = pt.antares.app.feature.fooddata.FoodRepository(
            db.foodDao(),
            db.foodNutrientDao(),
            db.searchMissDao(),
            Dispatchers.Default,
        )

        repo.cacheOnline(
            FoodEntity(
                id = "off-1",
                source = FoodSource.OFF,
                sourceRef = "5601234567890",
                namePt = "Cereais",
                nameEn = "Cereal",
                brand = "Marca",
                kcal = 100,
                proteinG = 1.0,
                carbsG = 1.0,
                fatG = 1.0,
                sugarsG = null,
                satFatG = null,
                fiberG = null,
                sodiumMg = null,
                microsJson = json.encodeToString(mapOf(FERRO to 8.0)),
                servingName = null,
                servingGrams = null,
                updatedAt = 1_000,
            ),
        )

        assertEquals(
            listOf("off-1"),
            db.foodNutrientDao().richIn(FERRO, 0.0, 1e9, 10).map { it.id },
            "um alimento novo ficou fora do «rico em» até à próxima sementeira — ou seja, " +
                "para sempre",
        )
    }

    @Test
    fun `a consulta indexada e mais rapida do que varrer o JSON`() = runTest {
        catalogo(CATALOGO_REAL)

        // A primeira corrida de cada um paga a compilação e o aquecimento. Medi-la fazia o
        // resultado depender de qual dos dois corresse primeiro.
        varrerJson()
        db.foodNutrientDao().richIn(FERRO, 0.0, 1e9, NutrientDensity.LIST_LIMIT)

        val comLike = measureTime { repeat(REPETICOES) { varrerJson() } }
        val comIndice = measureTime {
            repeat(REPETICOES) {
                db.foodNutrientDao().richIn(FERRO, 0.0, 1e9, NutrientDensity.LIST_LIMIT)
            }
        }

        println("rico em: LIKE=$comLike | indice=$comIndice | $CATALOGO_REAL alimentos")
        // O número exato depende da máquina; o que não pode acontecer é a nova ser pior.
        assertTrue(
            comIndice < comLike,
            "a junção indexada ($comIndice) não bateu o varrimento do JSON ($comLike) — " +
                "se isto falhar, o índice em `key` desapareceu",
        )
    }

    /**
     * O caminho antigo, inteiro: o `LIKE` dentro do JSON, as linhas trazidas para memória, e
     * o JSON de cada uma descodificado. Medir só a consulta deixava de fora metade do custo
     * — que é, precisamente, a metade que esta tarefa tira.
     */
    private suspend fun varrerJson(): Int {
        val encontrados = db.foodDao().microsParaIndexar()
        return encontrados.count { linha ->
            linha.microsJson
                ?.let { runCatching { json.decodeFromString<Map<String, Double>>(it) }.getOrNull() }
                ?.containsKey(FERRO) == true
        }
    }

    private companion object {
        const val FERRO = "iron_mg"

        // A ordem de grandeza do catálogo que a app traz.
        const val CATALOGO_REAL = 6_000
        const val REPETICOES = 5
    }
}
