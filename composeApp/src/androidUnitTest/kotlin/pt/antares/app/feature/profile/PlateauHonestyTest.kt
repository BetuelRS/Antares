package pt.antares.app.feature.profile

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.calc.AdaptiveTdee
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.MealSlot
import pt.antares.app.feature.profile.data.ProfileRepository
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PlateauHonestyTest {

    private lateinit var db: AntaresDb
    private lateinit var repository: ProfileRepository

    private val hoje = 20639L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        repository = ProfileRepository(
            db.userProfileDao(),
            db.weightLogDao(),
            db.dailyTargetOverrideDao(),
            db.foodLogDao(),
            db.goalHistoryDao(),
            Dispatchers.Default,
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun logar(dias: List<Long>) {
        for (dia in dias) {
            db.foodLogDao().upsert(
                FoodLogEntity(
                    id = "log-$dia",
                    epochDay = dia,
                    mealSlot = MealSlot.LUNCH,
                    foodId = null,
                    nameSnapshot = "almoço",
                    quantityGrams = 100.0,
                    kcalSnapshot = 200,
                    proteinSnapshot = 10.0,
                    carbsSnapshot = 20.0,
                    fatSnapshot = 5.0,
                    microsPer100Json = null,
                    updatedAt = 0L,
                ),
            )
        }
    }

    @Test
    fun `sem registo nenhum a media e zero`() = runTest {
        assertEquals(0, repository.loggedDaysPerWeek(weeks = 3, today = hoje))
    }

    @Test
    fun `tres semanas cheias dao sete dias por semana`() = runTest {
        logar((hoje - 20..hoje).toList())
        assertEquals(7, repository.loggedDaysPerWeek(weeks = 3, today = hoje))
    }

    @Test
    fun `registo a meio da tabela da a media certa`() = runTest {

        logar((0..8).map { hoje - it * 2L })
        assertEquals(3, repository.loggedDaysPerWeek(weeks = 3, today = hoje))
    }

    @Test
    fun `dias fora da janela nao contam`() = runTest {

        logar((hoje - 60..hoje - 30).toList())
        assertEquals(0, repository.loggedDaysPerWeek(weeks = 3, today = hoje))
    }

    @Test
    fun `um dia com varias refeicoes conta uma vez`() = runTest {

        val dia = hoje - 1
        db.foodLogDao().upsert(
            FoodLogEntity(
                id = "a", epochDay = dia, mealSlot = MealSlot.LUNCH, foodId = null,
                nameSnapshot = "almoço", quantityGrams = 100.0, kcalSnapshot = 200,
                proteinSnapshot = 10.0, carbsSnapshot = 20.0, fatSnapshot = 5.0,
                microsPer100Json = null, updatedAt = 0L,
            ),
        )
        db.foodLogDao().upsert(
            FoodLogEntity(
                id = "b", epochDay = dia, mealSlot = MealSlot.DINNER, foodId = null,
                nameSnapshot = "jantar", quantityGrams = 100.0, kcalSnapshot = 200,
                proteinSnapshot = 10.0, carbsSnapshot = 20.0, fatSnapshot = 5.0,
                microsPer100Json = null, updatedAt = 0L,
            ),
        )
        assertEquals(0, repository.loggedDaysPerWeek(weeks = 3, today = hoje))
        assertEquals(1, repository.loggedDaysPerWeek(weeks = 1, today = hoje))
    }

    @Test
    fun `quem registou tudo le adaptacao metabolica`() = runTest {
        logar((hoje - 20..hoje).toList())
        val dias = repository.loggedDaysPerWeek(weeks = 3, today = hoje)
        assertEquals(
            AdaptiveTdee.Assessment.METABOLIC_ADAPTATION,
            AdaptiveTdee.assessPlateau(consecutiveStallWeeks = 3, loggedDays = dias),
        )
    }

    @Test
    fun `quem registou metade dos dias le comida por contar`() = runTest {

        logar((0..9).map { hoje - it * 2L })
        val dias = repository.loggedDaysPerWeek(weeks = 3, today = hoje)
        assertEquals(
            AdaptiveTdee.Assessment.LIKELY_UNDER_LOGGING,
            AdaptiveTdee.assessPlateau(consecutiveStallWeeks = 3, loggedDays = dias),
        )
    }
}
