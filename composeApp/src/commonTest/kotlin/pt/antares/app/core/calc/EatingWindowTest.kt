package pt.antares.app.core.calc

import pt.antares.app.core.util.MINUTES_PER_HOUR
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EatingWindowTest {

    private fun h(hora: Int, minuto: Int = 0) = hora * MINUTES_PER_HOUR + minuto

    @Test
    fun `a janela vai da primeira a ultima refeicao do dia`() {
        val j = assertNotNull(EatingWindow.doDia(listOf(h(13), h(8, 20), h(21, 5))))

        assertEquals(h(8, 20), j.primeiraMin)
        assertEquals(h(21, 5), j.ultimaMin)
        assertEquals(12 * MINUTES_PER_HOUR + 45, j.duracaoMin)
        assertEquals(11 * MINUTES_PER_HOUR + 15, j.jejumMin, "a janela e o jejum têm de dar um dia")
    }

    @Test
    fun `uma refeicao so nao e uma janela`() {
        assertNull(
            EatingWindow.doDia(listOf(h(13))),
            "com uma refeição a janela daria zero, e zero dizia que se comeu tudo num instante",
        )
        assertNull(EatingWindow.doDia(emptyList()))
    }

    @Test
    fun `registos sem hora nao entram na conta, mas ficam contados`() {
        val j = assertNotNull(EatingWindow.doDia(listOf(h(8), null, h(20), null, null)))

        assertEquals(h(8), j.primeiraMin)
        assertEquals(h(20), j.ultimaMin)
        assertEquals(
            3,
            j.semHora,
            "sem esta contagem, uma janela feita sobre dois de cinco registos passa por " +
                "ser a janela do dia inteiro",
        )
    }

    @Test
    fun `um dia inteiro sem horas nao da janela nenhuma`() {
        assertNull(EatingWindow.doDia(listOf(null, null, null)))
    }

    @Test
    fun `a janela tipica precisa de uma semana de dias`() {
        val seisDias = List(6) { listOf(h(8), h(20)) }
        assertNull(
            EatingWindow.tipica(seisDias),
            "seis dias não são um hábito, e chamar-lhes isso é o erro que a app não comete",
        )

        assertNotNull(EatingWindow.tipica(List(7) { listOf(h(8), h(20)) }))
    }

    @Test
    fun `um jantar tardio isolado nao muda a janela tipica`() {
        // Nove dias iguais e um jantar de aniversário às 23:45. Pela média a última
        // refeição passava das 20h para lá das 20h20; pela mediana, fica onde está.
        val dias = List(9) { listOf(h(8), h(20)) } + listOf(listOf(h(8), h(23, 45)))

        val tipica = assertNotNull(EatingWindow.tipica(dias))
        assertEquals(h(20), tipica.ultimaMin, "a mediana deixou-se levar por um dia só")
        assertEquals(h(8), tipica.primeiraMin)
    }

    @Test
    fun `dias sem janela propria nao contam para a tipica`() {
        val setePlenos = List(7) { listOf(h(9), h(19)) }
        val tresVazios = List(3) { listOf<Int?>(null) }

        val tipica = assertNotNull(EatingWindow.tipica(setePlenos + tresVazios))
        assertEquals(h(9), tipica.primeiraMin)
        assertEquals(h(19), tipica.ultimaMin)
    }
}
