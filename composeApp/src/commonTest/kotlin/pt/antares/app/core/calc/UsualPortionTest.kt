package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsualPortionTest {

    @Test
    fun `sem registos que cheguem nao inventa nada`() {

        assertNull(UsualPortion.of(emptyList()))
        assertNull(UsualPortion.of(listOf(50.0)))
        assertNull(UsualPortion.of(listOf(50.0, 50.0)))
    }

    @Test
    fun `um engano isolado nao vira habito`() {

        val amounts = listOf(300.0, 30.0, 30.0, 30.0, 30.0, 30.0)
        assertEquals(30.0, UsualPortion.of(amounts))
    }

    @Test
    fun `com duas doses habituais ganha a mais frequente, nao a media`() {

        val amounts = listOf(200.0, 30.0, 30.0, 200.0, 30.0, 30.0, 200.0)
        assertEquals(30.0, UsualPortion.of(amounts))
    }

    @Test
    fun `quantidades quase iguais contam como a mesma dose`() {

        val amounts = listOf(48.0, 50.0, 52.0, 200.0)
        assertEquals(50.0, UsualPortion.of(amounts))
    }

    @Test
    fun `o valor devolvido nunca e o centro do degrau`() {

        assertEquals(51.0, UsualPortion.of(listOf(51.0, 51.0, 51.0)))
    }

    @Test
    fun `as fronteiras dos degraus sao fixas, e isso ve-se`() {

        assertEquals(46.5, UsualPortion.of(listOf(46.0, 47.0, 48.0)))
    }

    @Test
    fun `empate resolve-se pelo habito mais recente`() {

        val amounts = listOf(60.0, 60.0, 60.0, 200.0, 200.0, 200.0)
        assertEquals(60.0, UsualPortion.of(amounts))
    }

    @Test
    fun `quantidades invalidas sao ignoradas sem rebentar`() {
        assertNull(UsualPortion.of(listOf(0.0, -5.0, 0.0)))
        assertEquals(40.0, UsualPortion.of(listOf(40.0, 0.0, 40.0, -1.0, 40.0)))
    }
}
