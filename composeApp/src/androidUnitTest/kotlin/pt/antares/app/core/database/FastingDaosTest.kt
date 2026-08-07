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
import pt.antares.app.core.database.entities.FastingProtocolEntity
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.model.FastingStatus
import pt.antares.app.feature.fasting.data.FastingProtocolSeeder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class FastingDaosTest {

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

    private fun protocol(id: String, hours: Int) =
        FastingProtocolEntity(id = id, name = "$hours h", fastingHours = hours, updatedAt = 1L)

    private fun session(id: String, started: Long, status: FastingStatus, ended: Long? = null) =
        FastingSessionEntity(
            id = id, protocolId = "fp_16_8", startedAt = started,
            targetEndAt = started + 16 * 3_600_000L, endedAt = ended,
            status = status, updatedAt = started,
        )

    @Test
    fun `observeActive devolve so a sessao ACTIVE mais recente`() = runTest {
        val dao = db.fastingSessionDao()
        dao.upsert(session("s1", 1_000L, FastingStatus.COMPLETED, ended = 2_000L))
        dao.upsert(session("s2", 5_000L, FastingStatus.ACTIVE))

        val active = dao.observeActive().first()
        assertNotNull(active)
        assertEquals("s2", active.id)
        assertEquals("s2", dao.activeSession()?.id)
    }

    @Test
    fun `sem sessao ativa observeActive e null`() = runTest {
        val dao = db.fastingSessionDao()
        dao.upsert(session("s1", 1_000L, FastingStatus.BROKEN, ended = 1_500L))

        assertNull(dao.observeActive().first())
        assertNull(dao.activeSession())
    }

    @Test
    fun `historico exclui ACTIVE e ordena por startedAt desc`() = runTest {
        val dao = db.fastingSessionDao()
        dao.upsert(session("a", 1_000L, FastingStatus.COMPLETED, ended = 2_000L))
        dao.upsert(session("b", 3_000L, FastingStatus.BROKEN, ended = 3_500L))
        dao.upsert(session("c", 9_000L, FastingStatus.ACTIVE))

        val history = dao.observeHistory().first()
        assertEquals(listOf("b", "a"), history.map { it.id })

        assertEquals(listOf("a", "b"), dao.finishedSessions().map { it.id })
    }

    @Test
    fun `seed protocolos e idempotente`() = runTest {
        val dao = db.fastingProtocolDao()
        val seeder = FastingProtocolSeeder(dao, Dispatchers.Default)

        seeder.seedIfNeeded()
        val first = dao.count()
        assertEquals(4, first)

        seeder.seedIfNeeded()
        assertEquals(4, dao.count())

        val ordered = dao.observeAll().first().map { it.fastingHours }
        assertEquals(listOf(16, 18, 20, 23), ordered)
    }

    @Test
    fun `soft delete some da observeAll`() = runTest {
        val dao = db.fastingProtocolDao()
        dao.upsert(protocol("p1", 14))
        dao.upsert(protocol("p2", 16))
        assertEquals(2, dao.observeAll().first().size)

        dao.upsert(protocol("p1", 14).copy(deleted = true))
        val visible = dao.observeAll().first()
        assertEquals(listOf("p2"), visible.map { it.id })
    }
}
