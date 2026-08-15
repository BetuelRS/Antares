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
import pt.antares.app.core.database.entities.RoutineScheduleEntity
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class RoutineScheduleDaoTest {

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

    private fun contaApagados(tabela: String): Int =
        db.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM " + tabela + " WHERE deleted = 1")
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    @Test
    fun `upsert substitui a rotina do dia e observeAll ordena por dia`() = runTest {
        val dao = db.routineScheduleDao()
        dao.upsert(RoutineScheduleEntity(dayOfWeek = 3, routineId = "r1", updatedAt = 1))
        dao.upsert(RoutineScheduleEntity(dayOfWeek = 1, routineId = "r2", updatedAt = 1))

        dao.upsert(RoutineScheduleEntity(dayOfWeek = 3, routineId = "r3", updatedAt = 2))

        val all = dao.observeAll().first()
        assertEquals(listOf(1, 3), all.map { it.dayOfWeek })
        assertEquals("r3", all.first { it.dayOfWeek == 3 }.routineId)
    }

    @Test
    fun `clearDay remove so o dia`() = runTest {
        val dao = db.routineScheduleDao()
        dao.upsert(RoutineScheduleEntity(dayOfWeek = 2, routineId = "r1", updatedAt = 1))
        dao.upsert(RoutineScheduleEntity(dayOfWeek = 5, routineId = "r1", updatedAt = 1))
        dao.clearDay(2, now = 99)
        assertNull(dao.observeRoutineForDay(2).first())
        assertEquals("r1", dao.observeRoutineForDay(5).first())
    }

    @Test
    fun `clearDay deixa tombstone sujo para o sync, nao apaga a linha`() = runTest {
        val dao = db.routineScheduleDao()
        dao.upsert(RoutineScheduleEntity(dayOfWeek = 2, routineId = "r1", updatedAt = 1))
        dao.clearDay(2, now = 99)

        assertEquals(1, contaApagados("routine_schedule"))
        assertEquals(0, dao.exportRows().size, "uma lápide não é dado a exportar")
    }

    @Test
    fun `clearByRoutine limpa todos os dias dessa rotina`() = runTest {
        val dao = db.routineScheduleDao()
        dao.upsert(RoutineScheduleEntity(dayOfWeek = 1, routineId = "gone", updatedAt = 1))
        dao.upsert(RoutineScheduleEntity(dayOfWeek = 4, routineId = "gone", updatedAt = 1))
        dao.upsert(RoutineScheduleEntity(dayOfWeek = 6, routineId = "keep", updatedAt = 1))
        dao.clearByRoutine("gone", now = 99)
        val all = dao.observeAll().first()
        assertEquals(listOf(6), all.map { it.dayOfWeek })
    }
}
