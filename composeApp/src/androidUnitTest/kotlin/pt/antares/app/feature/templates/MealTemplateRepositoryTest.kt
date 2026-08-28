package pt.antares.app.feature.templates

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class MealTemplateRepositoryTest {

    private lateinit var db: AntaresDb
    private lateinit var repo: MealTemplateRepository

    private var idSeq = 0

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        repo = MealTemplateRepository(
            foodLogDao = db.foodLogDao(),
            templateDao = db.mealTemplateDao(),
            itemDao = db.mealTemplateItemDao(),
            io = Dispatchers.Unconfined,
            now = { 1_000L },
            newId = { "id${++idSeq}" },
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun logFood(
        id: String,
        day: Long,
        slot: MealSlot,
        name: String,
        kcal: Int,
        protein: Double,
    ) {
        db.foodLogDao().upsert(
            FoodLogEntity(
                id = id,
                epochDay = day,
                mealSlot = slot,
                foodId = "food-$id",
                nameSnapshot = name,
                quantityGrams = 100.0,
                kcalSnapshot = kcal,
                proteinSnapshot = protein,
                carbsSnapshot = 0.0,
                fatSnapshot = 0.0,
                microsPer100Json = null,
                origin = LogOrigin.MANUAL,
                updatedAt = 1L,
            ),
        )
    }

    @Test
    fun `guardar snapshota a refeicao e aplicar soma os itens ao slot destino`() = runTest {

        logFood("a", 100, MealSlot.LUNCH, "Arroz", 200, 4.0)
        logFood("b", 100, MealSlot.LUNCH, "Frango", 330, 60.0)

        val templateId = repo.saveMealAsTemplate("Almoço típico", MealSlot.LUNCH, 100)!!
        assertEquals(2, repo.items(templateId).size)

        val applied = repo.applyTemplate(templateId, MealSlot.DINNER, 200)
        assertEquals(2, applied.size)

        val dinner = db.foodLogDao().mealLogs(200, MealSlot.DINNER)
        assertEquals(2, dinner.size)
        assertEquals(530, dinner.sumOf { it.kcalSnapshot })
        assertEquals(64.0, dinner.sumOf { it.proteinSnapshot })

        assertEquals(setOf(MealSlot.DINNER), dinner.map { it.mealSlot }.toSet())
        assertEquals(setOf(200L), dinner.map { it.epochDay }.toSet())
    }

    @Test
    fun `template e snapshot — apagar os registos de origem nao mexe no template`() = runTest {
        logFood("a", 100, MealSlot.BREAKFAST, "Aveia", 350, 12.0)
        val templateId = repo.saveMealAsTemplate("Pequeno-almoço", MealSlot.BREAKFAST, 100)!!

        db.foodLogDao().softDelete("a", 2_000L)

        val items = repo.items(templateId)
        assertEquals(1, items.size)
        assertEquals(350, items.first().kcalSnapshot)
        assertEquals(12.0, items.first().proteinSnapshot)

        val applied = repo.applyTemplate(templateId, MealSlot.BREAKFAST, 300)
        assertEquals(1, applied.size)
        assertEquals(350, db.foodLogDao().mealLogs(300, MealSlot.BREAKFAST).first().kcalSnapshot)
    }

    @Test
    fun `guardar refeicao vazia devolve null`() = runTest {
        assertNull(repo.saveMealAsTemplate("Vazio", MealSlot.SNACK, 100))
    }
}
