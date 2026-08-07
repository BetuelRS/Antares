package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.diary.DiaryRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RepeatMealTest {

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

    private fun contaApagados(tabela: String): Int =
        db.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM " + tabela + " WHERE deleted = 1")
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    private fun log(
        id: String,
        day: Long,
        slot: MealSlot,
        name: String,
        kcal: Int,
        deleted: Boolean = false,
    ) = FoodLogEntity(
        id = id, epochDay = day, mealSlot = slot, foodId = null, nameSnapshot = name,
        quantityGrams = 100.0, kcalSnapshot = kcal, proteinSnapshot = 1.0,
        carbsSnapshot = 1.0, fatSnapshot = 1.0, microsPer100Json = null,
        origin = LogOrigin.MANUAL, updatedAt = 1L, deleted = deleted,
    )

    @Test
    fun `salta os dias falhados e vai buscar a ultima vez`() = runTest {
        val dao = db.foodLogDao()

        dao.upsert(log("a", 100, MealSlot.LUNCH, "Arroz", 300))
        dao.upsert(log("b", 100, MealSlot.LUNCH, "Frango", 250))

        val sugestao = repo.lastMealBefore(MealSlot.LUNCH, day = 105)

        assertEquals(100L, sugestao?.fromEpochDay)
        assertEquals(listOf("Arroz", "Frango"), sugestao?.names)
        assertEquals(550, sugestao?.kcal)
    }

    @Test
    fun `a ultima vez e mesmo a ultima, nao a primeira`() = runTest {
        val dao = db.foodLogDao()
        dao.upsert(log("velho", 100, MealSlot.DINNER, "Sopa", 120))
        dao.upsert(log("novo", 104, MealSlot.DINNER, "Peixe", 400))

        assertEquals(104L, repo.lastMealBefore(MealSlot.DINNER, day = 105)?.fromEpochDay)
    }

    @Test
    fun `o proprio dia nunca conta`() = runTest {

        db.foodLogDao().upsert(log("hoje", 105, MealSlot.BREAKFAST, "Pão", 200))

        assertNull(repo.lastMealBefore(MealSlot.BREAKFAST, day = 105))
    }

    @Test
    fun `um registo apagado nao ressuscita como sugestao`() = runTest {
        db.foodLogDao().upsert(log("morto", 100, MealSlot.SNACK, "Bolo", 400, deleted = true))

        assertNull(repo.lastMealBefore(MealSlot.SNACK, day = 105))
    }

    @Test
    fun `refeicoes nao se confundem umas com as outras`() = runTest {
        db.foodLogDao().upsert(log("a", 100, MealSlot.LUNCH, "Arroz", 300))

        assertNull(repo.lastMealBefore(MealSlot.DINNER, day = 105))
    }

    @Test
    fun `repetir copia o que a sugestao prometia, com ids novos`() = runTest {
        val dao = db.foodLogDao()
        dao.upsert(log("a", 100, MealSlot.LUNCH, "Arroz", 300))
        dao.upsert(log("b", 100, MealSlot.LUNCH, "Frango", 250))

        assertTrue(repo.repeatLastMeal(MealSlot.LUNCH, toDay = 105))

        val copiados = dao.mealLogs(105, MealSlot.LUNCH)
        assertEquals(2, copiados.size)
        assertEquals(550, copiados.sumOf { it.kcalSnapshot })

        assertTrue(copiados.none { it.id == "a" || it.id == "b" })
        assertEquals(2, dao.mealLogs(100, MealSlot.LUNCH).size)
    }

    @Test
    fun `repetir sem nada para repetir devolve falso e nao escreve`() = runTest {

        assertFalse(repo.repeatLastMeal(MealSlot.LUNCH, toDay = 105))
        assertEquals(0, db.foodLogDao().mealLogs(105, MealSlot.LUNCH).size)
    }

    @Test
    fun `mover a refeicao leva todos os itens e nao toca nos outros`() = runTest {
        val dao = db.foodLogDao()
        dao.upsert(log("a", 100, MealSlot.BREAKFAST, "Pão", 200))
        dao.upsert(log("b", 100, MealSlot.BREAKFAST, "Café", 5))
        dao.upsert(log("c", 100, MealSlot.LUNCH, "Arroz", 300))

        assertEquals(2, repo.moveMeal(100, MealSlot.BREAKFAST, MealSlot.SNACK))

        assertEquals(0, dao.mealLogs(100, MealSlot.BREAKFAST).size)
        assertEquals(2, dao.mealLogs(100, MealSlot.SNACK).size)

        assertEquals(1, dao.mealLogs(100, MealSlot.LUNCH).size)
    }

    @Test
    fun `mover para a propria refeicao nao faz nada`() = runTest {
        db.foodLogDao().upsert(log("a", 100, MealSlot.LUNCH, "Arroz", 300))
        assertEquals(0, repo.moveMeal(100, MealSlot.LUNCH, MealSlot.LUNCH))
        assertEquals(1, db.foodLogDao().mealLogs(100, MealSlot.LUNCH).size)
    }

    @Test
    fun `apagar a refeicao deixa tombstones, nao buracos`() = runTest {
        val dao = db.foodLogDao()
        dao.upsert(log("a", 100, MealSlot.DINNER, "Peixe", 400))
        dao.upsert(log("b", 100, MealSlot.DINNER, "Batata", 150))

        assertEquals(2, repo.clearMeal(100, MealSlot.DINNER))

        assertEquals(0, dao.mealLogs(100, MealSlot.DINNER).size)

        assertEquals(2, contaApagados("food_log"))
    }

    @Test
    fun `os candidatos vem do mais recente para o mais antigo, com o que traziam`() = runTest {
        val dao = db.foodLogDao()
        dao.upsert(log("a", 100, MealSlot.LUNCH, "Sopa", 120))
        dao.upsert(log("b", 102, MealSlot.LUNCH, "Arroz", 300))
        dao.upsert(log("c", 102, MealSlot.LUNCH, "Frango", 250))

        val candidatos = repo.recentMeals(MealSlot.LUNCH, beforeDay = 105)

        assertEquals(listOf(102L, 100L), candidatos.map { it.fromEpochDay })

        assertEquals(550, candidatos.first().kcal)
        assertEquals(listOf("Arroz", "Frango"), candidatos.first().names)
    }

    @Test
    fun `o dia visivel e o futuro nunca entram nos candidatos`() = runTest {
        val dao = db.foodLogDao()
        dao.upsert(log("hoje", 105, MealSlot.LUNCH, "Hoje", 100))
        dao.upsert(log("amanha", 106, MealSlot.LUNCH, "Amanhã", 100))

        assertEquals(emptyList(), repo.recentMeals(MealSlot.LUNCH, beforeDay = 105))
    }

    @Test
    fun `o limite corta os dias mais antigos, nao os mais recentes`() = runTest {
        val dao = db.foodLogDao()
        repeat(15) { i -> dao.upsert(log("d$i", 80L + i, MealSlot.DINNER, "Jantar $i", 100)) }

        val candidatos = repo.recentMeals(MealSlot.DINNER, beforeDay = 105, limit = 10)

        assertEquals(10, candidatos.size)
        assertEquals(94L, candidatos.first().fromEpochDay)
        assertEquals(85L, candidatos.last().fromEpochDay)
    }
}
