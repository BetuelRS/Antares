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
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ExerciseDaosTest {

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

    private fun log(id: String, day: Long, kcal: Int, deleted: Boolean = false) = ExerciseLogEntity(
        id = id, epochDay = day, origin = ExerciseOrigin.MANUAL, label = "Corrida",
        metId = "run_moderate", met = 11.0, durationMin = 30, kcal = kcal, refId = null,
        updatedAt = 1L, deleted = deleted,
    )

    @Test
    fun `soma diaria agrega o dia e ignora outro dia e apagados`() = runTest {
        val dao = db.exerciseLogDao()
        dao.upsert(log("1", 100, 385))
        dao.upsert(log("2", 100, 150))
        dao.upsert(log("3", 100, 200, deleted = true))
        dao.upsert(log("4", 101, 999))

        assertEquals(535, dao.observeDayKcal(100).first())
        assertEquals(2, dao.observeDay(100).first().size)
    }

    @Test
    fun `dia sem exercicio soma zero`() = runTest {
        assertEquals(0, db.exerciseLogDao().observeDayKcal(42).first())
    }

    @Test
    fun `soft delete esconde mas mantem tombstone`() = runTest {
        val dao = db.exerciseLogDao()
        dao.upsert(log("1", 100, 385))
        dao.softDelete("1", now = 5)
        assertEquals(0, dao.observeDayKcal(100).first())
    }
}
