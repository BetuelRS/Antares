package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetLimitsTest {

    @Test
    fun `o peso que rebentou a app fica de fora`() {
        assertFalse(SetLimits.isWeightValid(6010.0), "6010 kg voltou a passar")
    }

    @Test
    fun `um levantador a serio nunca esbarra no teto`() {

        assertTrue(SetLimits.isWeightValid(300.0))
        assertTrue(SetLimits.isWeightValid(500.0), "o recorde do mundo de peso morto anda aqui")
    }

    @Test
    fun `peso nulo, negativo ou nao finito nao e uma serie`() {
        assertFalse(SetLimits.isWeightValid(0.0))
        assertFalse(SetLimits.isWeightValid(-20.0))
        assertFalse(SetLimits.isWeightValid(Double.NaN))
        assertFalse(SetLimits.isWeightValid(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `as barras vazias e os halteres leves passam`() {

        assertTrue(SetLimits.isWeightValid(0.5))
        assertTrue(SetLimits.isWeightValid(20.0))
    }

    @Test
    fun `series longas de peso do corpo continuam a caber`() {

        assertTrue(SetLimits.isRepsValid(1))
        assertTrue(SetLimits.isRepsValid(100))
        assertTrue(SetLimits.isRepsValid(SetLimits.MAX_REPS))
    }

    @Test
    fun `zero repeticoes nao e uma serie, e o campo de tres digitos nao chega ao absurdo`() {
        assertFalse(SetLimits.isRepsValid(0))
        assertFalse(SetLimits.isRepsValid(-3))
        assertFalse(SetLimits.isRepsValid(999), "o campo aceita 3 dígitos; o modelo não tem de aceitar")
    }

    @Test
    fun `o RPE e opcional, mas quando vem tem de estar na escala`() {

        assertTrue(SetLimits.isRpeValid(null))
        assertTrue(SetLimits.isRpeValid(1.0))
        assertTrue(SetLimits.isRpeValid(7.5))
        assertTrue(SetLimits.isRpeValid(10.0))

        assertFalse(SetLimits.isRpeValid(0.5))
        assertFalse(SetLimits.isRpeValid(11.0))
        assertFalse(SetLimits.isRpeValid(99.0))
        assertFalse(SetLimits.isRpeValid(Double.NaN))
    }

    @Test
    fun `a serie so passa quando as tres partes passam`() {
        assertTrue(SetLimits.isSetValid(60.0, 10, null))
        assertTrue(SetLimits.isSetValid(60.0, 10, 8.0))

        assertFalse(SetLimits.isSetValid(null, 10, null), "sem peso escrito")
        assertFalse(SetLimits.isSetValid(60.0, null, null), "sem repetições escritas")
        assertFalse(SetLimits.isSetValid(6010.0, 10, null), "peso fora do intervalo")
        assertFalse(SetLimits.isSetValid(60.0, 0, null), "repetições fora do intervalo")
        assertFalse(SetLimits.isSetValid(60.0, 10, 42.0), "RPE fora da escala")
    }

    @Test
    fun `o recorde absurdo deixa de poder nascer`() {

        val serieRecusada = SetLimits.isSetValid(6010.0, 10, null)
        assertFalse(serieRecusada)

        val absurdo = OneRepMax.epley(6010.0, 10)
        assertTrue(
            absurdo != null && abs(absurdo - 8013.33) < 0.01,
            "o Epley mudou e este teste deixou de descrever o defeito: deu $absurdo",
        )
    }

    @Test
    fun `varrimento - nenhum peso aceite produz um 1RM fora da escala humana`() {
        var vistos = 0
        for (kg in 1..SetLimits.MAX_WEIGHT_KG.toInt()) {
            for (reps in 1..12) {
                if (!SetLimits.isSetValid(kg.toDouble(), reps, null)) continue
                val um = OneRepMax.epley(kg.toDouble(), reps)
                assertTrue(um != null && um.isFinite(), "$kg kg × $reps deu $um")

                assertTrue(um!! <= 700.0, "$kg kg × $reps estimou $um kg de 1RM")
                vistos++
            }
        }
        assertTrue(vistos > 5000, "o varrimento encolheu: $vistos")
    }
}
