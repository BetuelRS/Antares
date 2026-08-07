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
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.database.entities.TrackPointEntity
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunStatus
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class RunDaoTest {

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

    private fun run(id: String, started: Long, status: RunStatus = RunStatus.DONE) = RunEntity(
        id = id, type = ActivityType.RUN, startedAt = started, endedAt = started + 1_000_000,
        distanceM = 5000.0, movingS = 1500, elapsedS = 1600, avgPaceSecPerKm = 300,
        kcal = 350, elevGainM = 42.0, polyline = "abc", splitsJson = "[]",
        name = "Corrida", note = "", status = status, updatedAt = started,
    )

    @Test
    fun `historico ordena por data desc e exclui descartadas e apagadas`() = runTest {
        val dao = db.runDao()
        dao.upsert(run("a", 1_000L))
        dao.upsert(run("b", 5_000L))
        dao.upsert(run("c", 9_000L, status = RunStatus.DISCARDED))
        val history = dao.observeHistory().first()
        assertEquals(listOf("b", "a"), history.map { it.id })

        dao.softDelete("b", now = 10_000L)
        assertEquals(listOf("a"), dao.observeHistory().first().map { it.id })
        assertEquals(true, dao.byId("b")?.deleted)
    }

    @Test
    fun `poda apaga os track points do run`() = runTest {
        val tpDao = db.trackPointDao()
        db.runDao().upsert(run("r1", 1_000L))
        tpDao.insertAll(
            (0 until 10).map { i ->
                TrackPointEntity(runId = "r1", tMs = i.toLong(), lat = 38.72 + i * 1e-4, lon = -9.13, altM = null, accM = 5.0, speedMps = null)
            },
        )
        assertEquals(10, tpDao.countForRun("r1"))
        tpDao.deleteForRun("r1")
        assertEquals(0, tpDao.countForRun("r1"))
    }

    @Test
    fun `run inexistente devolve null`() = runTest {
        assertNull(db.runDao().byId("nada"))
    }
}
