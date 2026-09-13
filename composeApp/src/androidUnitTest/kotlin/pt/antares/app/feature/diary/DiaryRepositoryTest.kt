package pt.antares.app.feature.diary

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.MealSlot
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * As três coisas novas do `DiaryRepository`: copiar o dia inteiro com desfazer, os
 * candidatos a copiar, e a pesquisa pelo nome no histórico inteiro.
 */
@RunWith(RobolectricTestRunner::class)
class DiaryRepositoryTest {

    private lateinit var db: AntaresDb
    private lateinit var repo: DiaryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        repo = DiaryRepository(db.foodLogDao(), db.waterLogDao(), Dispatchers.Default)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun registar(id: String, dia: Long, nome: String = "arroz", kcal: Int = 200) =
        db.foodLogDao().upsert(
            FoodLogEntity(
                id = id,
                epochDay = dia,
                mealSlot = MealSlot.LUNCH,
                foodId = null,
                nameSnapshot = nome,
                quantityGrams = 100.0,
                kcalSnapshot = kcal,
                proteinSnapshot = 5.0,
                carbsSnapshot = 20.0,
                fatSnapshot = 2.0,
                microsPer100Json = null,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )

    // ---- copiar o dia inteiro ----------------------------------------------------------

    @Test
    fun `copiar o dia devolve exatamente os ids que criou`() = runTest {
        registar("a", dia = 100, kcal = 300)
        registar("b", dia = 100, kcal = 400)
        registar("ja-la-estava", dia = 200, kcal = 100)

        val criados = repo.copyDay(100, 200)

        assertEquals(2, criados.size)
        assertEquals(800, db.foodLogDao().dayTotals(200).kcal)
        // Os ids são novos: nenhum dos criados é o id de origem.
        assertTrue("a" !in criados && "b" !in criados)
    }

    @Test
    fun `desfazer a copia apaga so os ids devolvidos`() = runTest {
        registar("a", dia = 100, kcal = 300)
        registar("ja-la-estava", dia = 200, kcal = 100)

        val criados = repo.copyDay(100, 200)
        repo.desfazerCopiaDoDia(criados)

        val sobra = db.foodLogDao().dayLogs(200)
        assertEquals(listOf("ja-la-estava"), sobra.map { it.id })
    }

    @Test
    fun `os candidatos a copiar sao os dias antes do dia aberto, mais recente primeiro`() = runTest {
        registar("a", dia = 100, kcal = 300)
        registar("b", dia = 150, kcal = 200)
        registar("hoje-mesmo", dia = 200, kcal = 900)

        val candidatos = repo.recentDaysWithLogs(beforeDay = 200)

        assertEquals(listOf(150L, 100L), candidatos.map { it.epochDay })
        assertEquals(listOf(200, 300), candidatos.map { it.kcal })
    }

    // ---- pesquisar no historico ---------------------------------------------------------

    @Test
    fun `pesquisa em branco nao vai a base`() = runTest {
        registar("a", dia = 100, nome = "Arroz")

        assertEquals(emptyList(), repo.searchLogs(""))
        assertEquals(emptyList(), repo.searchLogs("   "))
    }

    @Test
    fun `pesquisa encontra pelo nome em qualquer dia, nao so no aberto`() = runTest {
        registar("bacalhau", dia = 100, nome = "Bacalhau à Gomes de Sá")
        registar("arroz", dia = 200, nome = "Arroz branco")

        val achados = repo.searchLogs("bacalhau")

        assertEquals(listOf("bacalhau"), achados.map { it.id })
    }

    /**
     * A pesquisa de alimentos da app ignora acentos e maiúsculas, e quem a usa todos os dias
     * espera o mesmo aqui. O `LIKE` do SQLite só dobra maiúsculas ASCII e não tira acentos:
     * «gomes de sa» não encontrava o exemplo que a própria área 02 dá.
     */
    @Test
    fun `pesquisa ignora acentos e maiusculas, como a pesquisa de alimentos`() = runTest {
        registar("bacalhau", dia = 100, nome = "Bacalhau à Gomes de Sá")
        registar("pao", dia = 110, nome = "Pão de mistura")

        assertEquals(listOf("bacalhau"), repo.searchLogs("gomes de sa").map { it.id })
        assertEquals(listOf("pao"), repo.searchLogs("PAO").map { it.id })
    }

    @Test
    fun `o que a pessoa escreve nao e um curinga`() = runTest {
        registar("arroz", dia = 100, nome = "Arroz branco")

        assertEquals(emptyList(), repo.searchLogs("%"))
        assertEquals(emptyList(), repo.searchLogs("_"))
    }

    @Test
    fun `a pesquisa devolve os mais recentes primeiro, ate ao limite`() = runTest {
        registar("velho", dia = 100, nome = "Arroz")
        registar("meio", dia = 150, nome = "Arroz")
        registar("novo", dia = 200, nome = "Arroz")

        assertEquals(listOf("novo", "meio"), repo.searchLogs("arroz", limit = 2).map { it.id })
    }
}
