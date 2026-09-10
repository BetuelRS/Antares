package pt.antares.app.feature.running

import pt.antares.app.feature.running.domain.RitmoMedio
import kotlin.test.Test
import kotlin.test.assertEquals

class RitmoMedioTest {

    @Test
    fun `sai dos totais e nao da media dos ritmos`() {
        // 1 km a 6:00 e 10 km a 5:00: 11 km em 3 360 s. A média dos dois ritmos daria 5:30,
        // que dá peso igual a uma corrida dez vezes mais curta.
        val segundos = 360L + 3000L
        assertEquals(305, RitmoMedio.secPorKm(11_000.0, segundos))
    }

    @Test
    fun `sem distancia devolve zero, que e o que o formatador le como tracos`() {
        assertEquals(0, RitmoMedio.secPorKm(0.0, 600))
        assertEquals(0, RitmoMedio.secPorKm(5.0, 600))
    }

    @Test
    fun `sem tempo, ou com tempo negativo, devolve zero`() {
        assertEquals(0, RitmoMedio.secPorKm(5000.0, 0))
        // Um relógio acertado para trás a meio da semana dava tempo negativo, e um ritmo
        // negativo não se escreve.
        assertEquals(0, RitmoMedio.secPorKm(5000.0, -10))
    }
}
