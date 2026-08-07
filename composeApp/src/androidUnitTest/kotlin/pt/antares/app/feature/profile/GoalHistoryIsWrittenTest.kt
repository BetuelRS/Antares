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
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import pt.antares.app.feature.profile.data.ProfileRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class GoalHistoryIsWrittenTest {

    private lateinit var db: AntaresDb
    private lateinit var repository: ProfileRepository

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

    private fun perfil(goalWeightKg: Double?) = UserProfileEntity(
        sex = Sex.MALE,
        birthEpochDay = 10_000L,
        heightCm = 178,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.LOSE,
        goalRateKcal = -550,
        macroStrategy = MacroStrategy.BALANCED,
        customProteinG = null,
        customCarbsG = null,
        customFatG = null,
        goalWeightKg = goalWeightKg,
        updatedAt = 0L,
    )

    @Test
    fun `gravar um peso-alvo deixa historia`() = runTest {
        repository.saveProfile(perfil(78.0))
        val historia = db.goalHistoryDao().all()
        assertEquals(1, historia.size, "o objetivo não foi registado")
        assertEquals(78.0, historia.first().targetKg)
    }

    @Test
    fun `gravar o mesmo alvo duas vezes nao duplica`() = runTest {

        repository.saveProfile(perfil(78.0))
        repository.saveProfile(perfil(78.0))
        assertEquals(1, db.goalHistoryDao().all().size)
    }

    @Test
    fun `mudar de alvo acrescenta, nao substitui`() = runTest {
        repository.saveProfile(perfil(78.0))
        repository.saveProfile(perfil(75.0))
        val historia = db.goalHistoryDao().all()
        assertEquals(2, historia.size)
        assertEquals(listOf(78.0, 75.0), historia.map { it.targetKg }.sortedDescending())
    }

    @Test
    fun `uma decima e uma mudanca`() = runTest {

        repository.saveProfile(perfil(78.0))
        repository.saveProfile(perfil(78.1))
        assertEquals(2, db.goalHistoryDao().all().size)
    }

    @Test
    fun `sem peso-alvo nao se inventa objetivo nenhum`() = runTest {
        repository.saveProfile(perfil(null))
        assertNull(db.goalHistoryDao().latest())
    }

    @Test
    fun `o objetivo guarda o peso de partida quando ja ha pesagens`() = runTest {
        repository.upsertWeight(epochDay = 20_000L, weightKg = 90.0, note = null)
        repository.saveProfile(perfil(78.0))
        val objetivo = db.goalHistoryDao().latest()
        assertNotNull(objetivo)
        assertEquals(90.0, objetivo.startWeightKg)
    }

    @Test
    fun `sem pesagens o ponto de partida fica por saber, e nao a zero`() = runTest {
        repository.saveProfile(perfil(78.0))
        assertNull(db.goalHistoryDao().latest()?.startWeightKg)
    }
}
