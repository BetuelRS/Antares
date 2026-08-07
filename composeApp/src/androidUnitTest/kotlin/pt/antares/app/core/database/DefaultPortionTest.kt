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
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.diary.DiaryRepository
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DefaultPortionTest {

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

    private fun food(
        id: String,
        lastAmountG: Double? = null,
        servingGrams: Double? = null,
    ) = FoodEntity(
        id = id, source = FoodSource.SEED, sourceRef = null, namePt = id, nameEn = id,
        brand = null, kcal = 100, proteinG = 1.0, carbsG = 1.0, sugarsG = null, fatG = 1.0,
        satFatG = null, fiberG = null, sodiumMg = null, microsJson = null,
        servingName = null, servingGrams = servingGrams, isFavorite = false, lastUsedAt = 0L,
        lastAmountG = lastAmountG, verified = true, updatedAt = 1L,
    )

    private suspend fun logar(foodId: String, gramas: Double, dia: Long) {
        db.foodLogDao().upsert(
            FoodLogEntity(
                id = "log-$foodId-$dia", epochDay = dia, mealSlot = MealSlot.LUNCH,
                foodId = foodId, nameSnapshot = foodId, quantityGrams = gramas,
                kcalSnapshot = 100, proteinSnapshot = 1.0, carbsSnapshot = 1.0,
                fatSnapshot = 1.0, microsPer100Json = null, origin = LogOrigin.MANUAL,
                updatedAt = 1L,
            ),
        )
    }

    @Test
    fun `sem nada sabido cai nas cem gramas do catalogo`() = runTest {
        assertEquals(100.0, repo.defaultPortionFor(food("x")))
    }

    @Test
    fun `a porcao do alimento ganha ao valor de recurso`() = runTest {
        assertEquals(30.0, repo.defaultPortionFor(food("x", servingGrams = 30.0)))
    }

    @Test
    fun `a ultima usada ganha a porcao do alimento`() = runTest {
        assertEquals(45.0, repo.defaultPortionFor(food("x", lastAmountG = 45.0, servingGrams = 30.0)))
    }

    @Test
    fun `o habito ganha a ultima usada`() = runTest {

        logar("x", 60.0, 100)
        logar("x", 60.0, 101)
        logar("x", 60.0, 102)
        logar("x", 300.0, 103)

        assertEquals(60.0, repo.defaultPortionFor(food("x", lastAmountG = 300.0, servingGrams = 30.0)))
    }
}
