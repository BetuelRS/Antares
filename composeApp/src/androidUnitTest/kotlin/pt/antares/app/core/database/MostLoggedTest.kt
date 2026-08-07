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
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MostLoggedTest {

    private lateinit var db: AntaresDb

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun food(id: String, nome: String, lastUsed: Long = 0L) = FoodEntity(
        id = id, source = FoodSource.SEED, sourceRef = null, namePt = nome, nameEn = nome,
        brand = null, kcal = 100, proteinG = 1.0, carbsG = 1.0, sugarsG = null, fatG = 1.0,
        satFatG = null, fiberG = null, sodiumMg = null, microsJson = null,
        servingName = null, servingGrams = null, isFavorite = false, lastUsedAt = lastUsed,
        verified = true, updatedAt = 1L,
    )

    private fun log(id: String, day: Long, foodId: String?, deleted: Boolean = false) =
        FoodLogEntity(
            id = id, epochDay = day, mealSlot = MealSlot.BREAKFAST, foodId = foodId,
            nameSnapshot = "x", quantityGrams = 100.0, kcalSnapshot = 100,
            proteinSnapshot = 1.0, carbsSnapshot = 1.0, fatSnapshot = 1.0,
            microsPer100Json = null, origin = LogOrigin.MANUAL, updatedAt = 1L,
            deleted = deleted,
        )

    @Test
    fun `a aveia de todos os dias ganha ao gelado de ontem`() = runTest {
        val foods = db.foodDao()
        val logs = db.foodLogDao()

        foods.upsert(food("aveia", "Aveia", lastUsed = 1_000L))
        foods.upsert(food("gelado", "Gelado", lastUsed = 9_999L))
        repeat(10) { i -> logs.upsert(log("a$i", 100L + i, "aveia")) }
        logs.upsert(log("g1", 110, "gelado"))

        val top = db.foodDao().observeMostLogged(sinceEpochDay = 0, limit = 20).first()

        assertEquals(listOf("aveia", "gelado"), top.map { it.id })
    }

    @Test
    fun `o que ficou fora da janela nao conta`() = runTest {
        val foods = db.foodDao()
        val logs = db.foodLogDao()
        foods.upsert(food("antigo", "Comida de outrora"))
        foods.upsert(food("atual", "Comida de agora"))
        repeat(20) { i -> logs.upsert(log("v$i", 10L + i, "antigo")) }
        logs.upsert(log("n1", 200, "atual"))

        val top = db.foodDao().observeMostLogged(sinceEpochDay = 150, limit = 20).first()

        assertEquals(listOf("atual"), top.map { it.id })
    }

    @Test
    fun `registos apagados nao contam`() = runTest {
        val foods = db.foodDao()
        val logs = db.foodLogDao()
        foods.upsert(food("x", "X"))
        repeat(5) { i -> logs.upsert(log("d$i", 100L + i, "x", deleted = true)) }

        assertTrue(db.foodDao().observeMostLogged(sinceEpochDay = 0, limit = 20).first().isEmpty())
    }

    @Test
    fun `registos sem alimento nao rebentam a lista`() = runTest {

        val logs = db.foodLogDao()
        repeat(3) { i -> logs.upsert(log("ai$i", 100L + i, null)) }

        assertTrue(db.foodDao().observeMostLogged(sinceEpochDay = 0, limit = 20).first().isEmpty())
    }

    @Test
    fun `o limite e respeitado`() = runTest {
        val foods = db.foodDao()
        val logs = db.foodLogDao()
        repeat(25) { i ->
            foods.upsert(food("f$i", "Alimento $i"))
            logs.upsert(log("l$i", 100, "f$i"))
        }

        assertEquals(20, db.foodDao().observeMostLogged(sinceEpochDay = 0, limit = 20).first().size)
    }
}
