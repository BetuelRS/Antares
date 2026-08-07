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
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class ProfileDaosTest {

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

    private fun profile() = UserProfileEntity(
        sex = Sex.MALE,
        birthEpochDay = 10000L,
        heightCm = 178,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.MAINTAIN,
        goalRateKcal = 0,
        macroStrategy = MacroStrategy.BALANCED,
        customProteinG = null,
        customCarbsG = null,
        customFatG = null,
        updatedAt = 1L,
    )

    @Test
    fun `perfil upsert e observe linha unica`() = runTest {
        val dao = db.userProfileDao()
        assertNull(dao.get())

        dao.upsert(profile())
        assertEquals(178, dao.get()?.heightCm)

        dao.upsert(profile().copy(heightCm = 180, updatedAt = 2L))
        assertEquals(180, dao.get()?.heightCm)
    }

    @Test
    fun `weight latest devolve o mais recente e softDelete esconde`() = runTest {
        val dao = db.weightLogDao()
        dao.upsert(WeightLogEntity("a", epochDay = 100, weightKg = 81.0, note = null, updatedAt = 1))
        dao.upsert(WeightLogEntity("b", epochDay = 105, weightKg = 80.2, note = null, updatedAt = 2))

        assertEquals(80.2, dao.latest()?.weightKg)

        dao.softDelete("b", now = 3)
        assertEquals(81.0, dao.latest()?.weightKg)

        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun `weight range ordena ascendente`() = runTest {
        val dao = db.weightLogDao()
        dao.upsert(WeightLogEntity(id = "a", epochDay = 103, weightKg = 80.5, note = null, updatedAt = 1))
        dao.upsert(WeightLogEntity(id = "b", epochDay = 101, weightKg = 81.0, note = null, updatedAt = 1))
        dao.upsert(WeightLogEntity(id = "c", epochDay = 102, weightKg = 80.8, note = null, updatedAt = 1))

        val range = dao.range(101, 103)
        assertEquals(listOf(101L, 102L, 103L), range.map { it.epochDay })
    }
}
