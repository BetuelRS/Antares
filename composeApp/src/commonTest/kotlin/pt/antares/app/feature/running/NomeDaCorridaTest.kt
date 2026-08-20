package pt.antares.app.feature.running

import pt.antares.app.feature.running.ui.PeriodoDoDia
import pt.antares.app.feature.running.ui.periodoDoDia
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * O campo do nome de uma corrida abria vazio, e quase ninguém escrevia nada: o detalhe de
 * todas as corridas chamava-se «Resumo» e o GPX exportado saía sem nome. Passa a abrir com
 * «Corrida da manhã».
 *
 * A parte que se pode enganar é a hora. A madrugada dá a volta à meia-noite, e escrita como
 * `hora in 22..4` seria um intervalo vazio — todas as horas cairiam noutro ramo sem erro
 * nenhum a avisar.
 */
class NomeDaCorridaTest {

    @Test
    fun `as quatro fronteiras estao onde se diz`() {
        assertEquals(PeriodoDoDia.MANHA, periodoDoDia(5))
        assertEquals(PeriodoDoDia.MANHA, periodoDoDia(11))
        assertEquals(PeriodoDoDia.TARDE, periodoDoDia(12))
        assertEquals(PeriodoDoDia.TARDE, periodoDoDia(17))
        assertEquals(PeriodoDoDia.NOITE, periodoDoDia(18))
        assertEquals(PeriodoDoDia.NOITE, periodoDoDia(21))
    }

    @Test
    fun `a madrugada da a volta a meia-noite`() {
        for (hora in listOf(22, 23, 0, 1, 4)) {
            assertEquals(
                PeriodoDoDia.MADRUGADA,
                periodoDoDia(hora),
                "a hora $hora saiu da madrugada — o intervalo que dá a volta partiu-se",
            )
        }
    }

    @Test
    fun `as vinte e quatro horas do dia cabem todas`() {
        val cobertas = (0..23).map { periodoDoDia(it) }
        assertEquals(24, cobertas.size)
        assertEquals(
            setOf(
                PeriodoDoDia.MADRUGADA,
                PeriodoDoDia.MANHA,
                PeriodoDoDia.TARDE,
                PeriodoDoDia.NOITE,
            ),
            cobertas.toSet(),
            "há um período do dia que nenhuma hora alcança",
        )
    }
}
