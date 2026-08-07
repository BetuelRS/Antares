package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimeAxisTest {

    private val d0 = 20_000L

    @Test
    fun `sem dias nao ha eixo`() {
        assertNull(TimeAxis.of(emptyList()))
    }

    @Test
    fun `as pontas sao o primeiro e o ultimo dia, venham por que ordem vierem`() {
        val eixo = TimeAxis.of(listOf(d0 + 5, d0, d0 + 30, d0 + 2))!!
        assertEquals(d0, eixo.firstDay)
        assertEquals(d0 + 30, eixo.lastDay)
    }

    @Test
    fun `as pontas encostam as bordas`() {
        val eixo = TimeAxis(d0, d0 + 10)
        assertEquals(0.0, eixo.fraction(d0))
        assertEquals(1.0, eixo.fraction(d0 + 10))
    }

    @Test
    fun `um dia so fica ao meio, nao encostado a uma borda`() {
        val eixo = TimeAxis(d0, d0)
        assertEquals(0.5, eixo.fraction(d0))
        assertEquals(0L, eixo.spanDays)
    }

    @Test
    fun `tres meses parado ocupam tres meses do grafico`() {

        val dias = listOf(d0, d0 + 1, d0 + 2, d0 + 3, d0 + 4, d0 + 94)
        val eixo = TimeAxis.of(dias)!!

        assertTrue(eixo.fraction(d0 + 4) < 0.05, "as diárias esticaram-se pelo gráfico")

        val ausencia = eixo.fraction(d0 + 94) - eixo.fraction(d0 + 4)
        assertTrue(ausencia > 0.94, "a ausência de 90 dias encolheu para $ausencia")
    }

    @Test
    fun `a fracao e monotona - um dia mais tarde nunca fica mais a esquerda`() {
        val eixo = TimeAxis(d0, d0 + 365)
        var anterior = -1.0
        for (dia in d0..(d0 + 365)) {
            val f = eixo.fraction(dia)
            assertTrue(f >= anterior, "dia $dia recuou: $f depois de $anterior")
            anterior = f
        }
    }

    @Test
    fun `um dia fora do eixo encosta em vez de sair do grafico`() {
        val eixo = TimeAxis(d0, d0 + 10)
        assertEquals(0.0, eixo.fraction(d0 - 100))
        assertEquals(1.0, eixo.fraction(d0 + 100))
    }

    @Test
    fun `o meio do eixo e mesmo o meio`() {
        val eixo = TimeAxis(d0, d0 + 100)
        assertTrue(abs(eixo.fraction(d0 + 50) - 0.5) < 1e-9)
    }

    @Test
    fun `tres rotulos - inicio, meio e fim`() {
        val eixo = TimeAxis(d0, d0 + 100)
        assertEquals(listOf(d0, d0 + 50, d0 + 100), eixo.tickDays(3))
    }

    @Test
    fun `os rotulos ficam sempre dentro do eixo`() {
        for (span in 0L..200L) {
            val eixo = TimeAxis(d0, d0 + span)
            for (t in eixo.tickDays(3)) {
                assertTrue(t in d0..(d0 + span), "rótulo $t fora do eixo de $span dias")
            }
        }
    }

    @Test
    fun `um eixo de um dia nao repete a mesma data tres vezes`() {
        assertEquals(listOf(d0), TimeAxis(d0, d0).tickDays(3))
    }

    @Test
    fun `um eixo de dois dias nao inventa um dia a meio`() {

        assertEquals(listOf(d0, d0 + 1), TimeAxis(d0, d0 + 1).tickDays(3))
    }

    @Test
    fun `zero rotulos e uma resposta valida`() {
        assertEquals(emptyList(), TimeAxis(d0, d0 + 10).tickDays(0))
    }

    @Test
    fun `os rotulos vem por ordem cronologica`() {
        val ticks = TimeAxis(d0, d0 + 999).tickDays(5)
        assertEquals(ticks.sorted(), ticks)
    }
}
