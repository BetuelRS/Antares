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
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.feature.profile.data.BodyMeasurementRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class MeasurementHistoryIsWrittenTest {

    private lateinit var db: AntaresDb
    private lateinit var repository: BodyMeasurementRepository

    private val hoje = 20_663L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        repository = BodyMeasurementRepository(db.bodyMeasurementDao(), db.userProfileDao(), Dispatchers.Default)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `gravar uma medida deixa historia`() = runTest {
        repository.record(epochDay = hoje, waistCm = 84.0)
        val guardada = repository.latest()
        assertNotNull(guardada)
        assertEquals(84.0, guardada.waistCm)
    }

    @Test
    fun `medir so o braco nao apaga a cintura de hoje`() = runTest {
        repository.record(epochDay = hoje, waistCm = 84.0)
        repository.record(epochDay = hoje, armCm = 36.0)
        val guardada = repository.latest()!!
        assertEquals(84.0, guardada.waistCm, "a cintura desapareceu ao gravar o braço")
        assertEquals(36.0, guardada.armCm)
    }

    @Test
    fun `duas medicoes no mesmo dia sao uma linha so`() = runTest {
        repository.record(epochDay = hoje, waistCm = 84.0)
        repository.record(epochDay = hoje, waistCm = 83.5)
        assertEquals(1, db.bodyMeasurementDao().all().size)
        assertEquals(83.5, repository.latest()!!.waistCm)
    }

    @Test
    fun `dias diferentes sao linhas diferentes`() = runTest {
        repository.record(epochDay = hoje - 30, waistCm = 88.0)
        repository.record(epochDay = hoje, waistCm = 84.0)
        assertEquals(2, db.bodyMeasurementDao().all().size)

        assertEquals(84.0, repository.latest()!!.waistCm)
    }

    @Test
    fun `as tres medidas novas sobrevivem a ida a base de dados`() = runTest {
        repository.record(epochDay = hoje, armCm = 36.0, thighCm = 58.0, chestCm = 100.0)
        val guardada = repository.latest()!!
        assertEquals(36.0, guardada.armCm)
        assertEquals(58.0, guardada.thighCm)
        assertEquals(100.0, guardada.chestCm)
    }

    @Test
    fun `uma gravacao sem medida nenhuma nao cria linha`() = runTest {

        repository.record(epochDay = hoje)
        assertNull(repository.latest())
    }

    @Test
    fun `a origem da percentagem viaja com ela`() = runTest {

        repository.record(epochDay = hoje, bodyFatPct = 18.0, bodyFatSource = BodyFatSource.NAVY)
        assertEquals(BodyFatSource.NAVY, repository.latest()!!.bodyFatSource)
    }
}
