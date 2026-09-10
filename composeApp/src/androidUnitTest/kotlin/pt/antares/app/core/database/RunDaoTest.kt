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
    fun `a semana soma so as corridas dentro dela, e so as feitas`() = runTest {
        val dao = db.runDao()
        dao.upsert(run("antes", 999L))
        dao.upsert(run("dentro1", 1_000L))
        dao.upsert(run("dentro2", 1_500L).copy(distanceM = 3000.0, movingS = 900))
        dao.upsert(run("descartada", 1_600L, status = RunStatus.DISCARDED))
        dao.upsert(run("apagada", 1_700L))
        dao.softDelete("apagada", now = 1_800L)
        // O limite de cima é exclusivo: a corrida que começa à meia-noite de segunda é da
        // semana seguinte, e contá-la nas duas somava-a duas vezes.
        dao.upsert(run("depois", 2_000L))

        val semana = dao.observeSemana(deMs = 1_000L, ateMs = 2_000L).first()

        assertEquals(2, semana.corridas)
        assertEquals(8000.0, semana.metros)
        assertEquals(2400L, semana.movimentoS)
    }

    @Test
    fun `as ultimas vem da mais recente e param no limite`() = runTest {
        val dao = db.runDao()
        dao.upsert(run("a", 1_000L))
        dao.upsert(run("b", 5_000L))
        dao.upsert(run("c", 9_000L))
        dao.upsert(run("d", 9_500L, status = RunStatus.DISCARDED))

        assertEquals(listOf("c", "b"), dao.observeUltimas(quantas = 2).first().map { it.id })
        assertEquals(listOf("c", "b", "a"), dao.observeCorridas().first().map { it.id })
    }

    @Test
    fun `os parciais dos recordes excluem descartadas e apagadas`() = runTest {
        val dao = db.runDao()
        dao.upsert(run("a", 1_000L).copy(splitsJson = "[1]"))
        dao.upsert(run("b", 2_000L, status = RunStatus.DISCARDED).copy(splitsJson = "[2]"))
        dao.upsert(run("c", 3_000L).copy(splitsJson = "[3]"))
        dao.softDelete("c", now = 4_000L)

        assertEquals(listOf("[1]"), dao.observeSplitsJson().first())
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
