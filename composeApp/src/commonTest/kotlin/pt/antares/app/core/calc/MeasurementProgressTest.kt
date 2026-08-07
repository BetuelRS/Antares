package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.BodyMeasurementEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MeasurementProgressTest {

    private fun medicao(dia: Long, cintura: Double? = null, gordura: Double? = null) =
        BodyMeasurementEntity(
            id = "m$dia",
            epochDay = dia,
            waistCm = cintura,
            bodyFatPct = gordura,
            updatedAt = 0L,
        )

    @Test
    fun `uma so medicao nao da comparacao`() {
        assertNull(MeasurementProgressCalc.compute(listOf(medicao(10, cintura = 90.0))))
    }

    @Test
    fun `sem medicoes nenhumas tambem nao`() {
        assertNull(MeasurementProgressCalc.compute(emptyList()))
    }

    @Test
    fun `a cintura a descer aparece como delta negativo`() {
        val p = MeasurementProgressCalc.compute(
            listOf(medicao(10, cintura = 92.0), medicao(100, cintura = 87.0)),
        )!!
        assertEquals(92.0, p.waistFrom)
        assertEquals(87.0, p.waistTo)
        assertEquals(-5.0, p.waistDelta)
        assertEquals(90L, p.spanDays)
    }

    @Test
    fun `meio centimetro nao e progresso`() {
        val p = MeasurementProgressCalc.compute(
            listOf(medicao(10, cintura = 90.0), medicao(100, cintura = 89.5)),
        )!!
        assertFalse(p.isMeaningful)
    }

    @Test
    fun `um centimetro ja e progresso`() {
        val p = MeasurementProgressCalc.compute(
            listOf(medicao(10, cintura = 90.0), medicao(100, cintura = 89.0)),
        )!!
        assertTrue(p.isMeaningful)
    }

    @Test
    fun `a cintura a subir tambem conta como mudanca`() {
        val p = MeasurementProgressCalc.compute(
            listOf(medicao(10, cintura = 88.0), medicao(100, cintura = 91.0)),
        )!!
        assertEquals(3.0, p.waistDelta)
        assertTrue(p.isMeaningful)
    }

    @Test
    fun `cada metrica usa os seus proprios extremos`() {
        val p = MeasurementProgressCalc.compute(
            listOf(
                medicao(10, gordura = 24.0),
                medicao(50, cintura = 92.0),
                medicao(100, cintura = 88.0, gordura = 20.0),
            ),
        )!!
        assertEquals(24.0, p.fatFrom)
        assertEquals(20.0, p.fatTo)
        assertEquals(92.0, p.waistFrom)
        assertEquals(88.0, p.waistTo)
    }

    @Test
    fun `uma metrica medida uma unica vez nao se compara`() {
        val p = MeasurementProgressCalc.compute(
            listOf(medicao(10, cintura = 92.0), medicao(100, cintura = 88.0, gordura = 20.0)),
        )!!
        assertNull(p.fatFrom)
        assertNull(p.fatTo)
        assertNull(p.fatDelta)
    }

    @Test
    fun `sem mudanca em nenhuma metrica nao ha nada a mostrar`() {
        val p = MeasurementProgressCalc.compute(
            listOf(medicao(10, cintura = 90.0), medicao(100, cintura = 90.0)),
        )!!
        assertEquals(0.0, p.waistDelta)
        assertFalse(p.isMeaningful)
    }
}
