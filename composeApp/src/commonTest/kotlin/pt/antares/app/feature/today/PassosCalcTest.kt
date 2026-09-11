package pt.antares.app.feature.today

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A meta de passos e a distância estimada. O que se guarda é a honestidade dos dois números:
 * a fração passa de 1 quando se passa da meta, e sem altura não há distância — em vez de uma
 * passada inventada.
 */
class PassosCalcTest {

    @Test
    fun `a fracao passa de um quando se passa da meta`() {
        assertEquals(0.5f, PassosCalc.fracao(4_000))
        assertEquals(1.5f, PassosCalc.fracao(12_000))
    }

    @Test
    fun `a distancia sai da passada pela altura`() {
        // 1,80 m dá uma passada de 0,747 m; 10 000 passos são 7 470 m.
        assertEquals(7_470.0, PassosCalc.distanciaM(10_000, alturaCm = 180.0))
    }

    @Test
    fun `sem altura nao ha distancia`() {
        assertNull(PassosCalc.distanciaM(10_000, alturaCm = null))
        assertNull(PassosCalc.distanciaM(10_000, alturaCm = 0.0))
    }
}
