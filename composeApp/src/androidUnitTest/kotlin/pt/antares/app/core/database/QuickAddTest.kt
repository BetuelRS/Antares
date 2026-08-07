package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.diary.DiaryRepository
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class QuickAddTest {

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

    @Test
    fun `grava as calorias com o nome dado e sem alimento de origem`() = runTest {
        repo.logQuickCalories(kcal = 450, name = "Jantar fora", slot = MealSlot.DINNER, epochDay = 100)

        val logs = db.foodLogDao().mealLogs(100, MealSlot.DINNER)
        assertEquals(1, logs.size)
        assertEquals(450, logs[0].kcalSnapshot)
        assertEquals("Jantar fora", logs[0].nameSnapshot)
        assertNull(logs[0].foodId)
    }

    @Test
    fun `conta para as calorias do dia e nao para os macros`() = runTest {
        repo.logQuickCalories(kcal = 700, name = "x", slot = MealSlot.LUNCH, epochDay = 100)

        val totais = db.foodLogDao().observeDayTotals(100).first()
        assertEquals(700, totais.kcal)
        assertEquals(0.0, totais.proteinG, 1e-9)
        assertEquals(0.0, totais.carbsG, 1e-9)
        assertEquals(0.0, totais.fatG, 1e-9)
    }

    @Test
    fun `a quantidade nunca fica a zero`() = runTest {

        repo.logQuickCalories(kcal = 300, name = "x", slot = MealSlot.SNACK, epochDay = 100)
        val log = db.foodLogDao().mealLogs(100, MealSlot.SNACK).first()

        repo.updateQuantity(log.id, 200.0)

        assertEquals(600, db.foodLogDao().byId(log.id)?.kcalSnapshot)
    }
}
