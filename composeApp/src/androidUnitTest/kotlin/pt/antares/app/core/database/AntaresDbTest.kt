package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class AntaresDbTest {

    private lateinit var db: AntaresDb

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `base de dados abre e permite escrever e ler`() = runTest {
        val dao = db.dbInfoDao()
        assertNull(dao.get("schema_version"))

        dao.upsert(DbInfo(key = "schema_version", value = "1"))

        val stored = dao.get("schema_version")
        assertEquals("1", stored?.value)
    }

    @Test
    fun `upsert substitui valor existente`() = runTest {
        val dao = db.dbInfoDao()
        dao.upsert(DbInfo(key = "flag", value = "old"))
        dao.upsert(DbInfo(key = "flag", value = "new"))

        assertEquals("new", dao.get("flag")?.value)
    }
}
