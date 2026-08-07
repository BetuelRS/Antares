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
import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.database.entities.CoachReportEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class TombstoneCollisionTest {

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

    private val dia = 20_000L

    @Test
    fun `pesar-se num dia com registo apagado guarda o peso novo`() = runTest {
        val dao = db.weightLogDao()
        dao.upsert(WeightLogEntity(id = "antigo", epochDay = dia, weightKg = 80.0, note = null, updatedAt = 1L))
        dao.softDelete("antigo", now = 2L)

        val visto = dao.byDayForWrite(dia)
        dao.upsert(
            WeightLogEntity(
                id = visto?.id ?: "novo",
                epochDay = dia,
                weightKg = 75.0,
                note = null,
                updatedAt = 3L,
            ),
        )

        val guardado = dao.byDay(dia)
        assertNotNull(guardado, "a pesagem desapareceu sem erro nenhum")
        assertEquals(75.0, guardado.weightKg, 0.001)
    }

    @Test
    fun `medir-se num dia com registo apagado guarda a medida nova`() = runTest {
        val dao = db.bodyMeasurementDao()
        dao.upsert(BodyMeasurementEntity(id = "antigo", epochDay = dia, waistCm = 90.0, updatedAt = 1L))
        dao.softDelete("antigo", now = 2L)

        val visto = dao.byDayForWrite(dia)
        dao.upsert(
            BodyMeasurementEntity(
                id = visto?.id ?: "novo",
                epochDay = dia,
                waistCm = 85.0,
                updatedAt = 3L,
            ),
        )

        val guardado = dao.byDay(dia)
        assertNotNull(guardado, "a medição desapareceu sem erro nenhum")
        assertEquals(85.0, guardado.waistCm)
    }

    @Test
    fun `regerar a semana de um relatorio apagado guarda o relatorio novo`() = runTest {
        val dao = db.coachReportDao()

        dao.upsert(relatorio("antigo", focus = "proteína", updatedAt = 1L, deleted = true))

        val visto = dao.byWeekForWrite(dia)
        dao.upsert(relatorio(visto?.id ?: "novo", focus = "hidratos", updatedAt = 3L))

        val guardado = dao.byWeekForWrite(dia)
        assertNotNull(guardado, "o relatório desapareceu sem erro nenhum")
        assertEquals("hidratos", guardado.focus)
        assertEquals(false, guardado.deleted, "o relatório novo nasceu apagado")
    }

    private fun relatorio(
        id: String,
        focus: String,
        updatedAt: Long,
        deleted: Boolean = false,
    ) = CoachReportEntity(
        id = id,
        weekStartEpochDay = dia,
        winsJson = "[]",
        observationsJson = "[]",
        adjustmentsJson = "[]",
        focus = focus,
        aggregateJson = "{}",
        createdAt = updatedAt,
        updatedAt = updatedAt,
        deleted = deleted,
    )

    @Test
    fun `byDay e observeDay da agua respondem o mesmo sobre um apagado`() = runTest {
        val dao = db.waterLogDao()
        dao.upsert(WaterLogEntity(id = "a", epochDay = dia, ml = 1000, updatedAt = 1L, deleted = true))

        assertNull(dao.observeDay(dia).first())
        assertNull(dao.byDay(dia), "o byDay da água devolveu um registo apagado")
    }

    @Test
    fun `beber agua num dia com registo apagado nao soma ao valor antigo`() = runTest {
        val dao = db.waterLogDao()
        dao.upsert(WaterLogEntity(id = "a", epochDay = dia, ml = 1000, updatedAt = 1L, deleted = true))

        val visto = dao.byDayForWrite(dia)
        val base = if (visto == null || visto.deleted) 0 else visto.ml
        dao.upsert(
            WaterLogEntity(
                id = visto?.id ?: "novo",
                epochDay = dia,
                ml = base + 250,
                updatedAt = 2L,
            ),
        )

        val guardado = dao.observeDay(dia).first()
        assertNotNull(guardado, "a água desapareceu")
        assertEquals(250, guardado.ml, "somou ao litro que estava apagado")
    }
}
