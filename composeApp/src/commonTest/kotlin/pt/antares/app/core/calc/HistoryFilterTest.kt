package pt.antares.app.core.calc

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Os históricos de treino e de corrida não tinham forma nenhuma de filtrar: com duzentos
 * treinos gravados, chegar ao de fevereiro era percorrer duzentos cartões.
 *
 * O filtro corre em UTC neste teste para o resultado não depender do fuso da máquina — na
 * app corre no fuso do telemóvel, que é onde as datas dos treinos foram escritas.
 */
class HistoryFilterTest {

    private val utc = TimeZone.UTC

    private fun instante(ano: Int, mes: Int, dia: Int): Long =
        LocalDateTime(LocalDate(ano, mes, dia), kotlinx.datetime.LocalTime(12, 0))
            .toInstant(utc)
            .toEpochMilliseconds()

    @Test
    fun `o mes de um instante e o ano mais o mes`() {
        assertEquals(Mes(2026, 2), HistoryFilter.mesDe(instante(2026, 2, 14), utc))
    }

    @Test
    fun `os meses saem do mais recente para o mais antigo`() {
        val instantes = listOf(
            instante(2025, 12, 3),
            instante(2026, 2, 14),
            instante(2026, 2, 2),
            instante(2026, 1, 30),
        )

        assertEquals(
            listOf(Mes(2026, 2), Mes(2026, 1), Mes(2025, 12)),
            HistoryFilter.mesesDe(instantes, utc),
            "a lista lê-se do mais recente para trás, e o filtro segue a mesma ordem",
        )
    }

    @Test
    fun `fevereiro de anos diferentes sao meses diferentes`() {
        // É esta a razão de o ano ir junto: sem ele, o filtro juntava o fevereiro deste ano
        // com o de há três, que é a comparação que um histórico serve para não fazer.
        val instantes = listOf(instante(2026, 2, 1), instante(2023, 2, 1))

        assertEquals(2, HistoryFilter.mesesDe(instantes, utc).size)
    }

    @Test
    fun `filtrar por um mes deixa so esse mes`() {
        val itens = listOf(
            instante(2026, 2, 14),
            instante(2026, 1, 30),
            instante(2026, 2, 2),
        )

        val fevereiro = HistoryFilter.porMes(itens, Mes(2026, 2), utc) { it }
        assertEquals(2, fevereiro.size)
    }

    @Test
    fun `sem mes escolhido a lista fica inteira`() {
        val itens = listOf(instante(2026, 2, 14), instante(2026, 1, 30))

        assertEquals(itens, HistoryFilter.porMes(itens, mes = null, zone = utc) { it })
    }

    @Test
    fun `uma lista vazia nao tem meses`() {
        assertEquals(emptyList(), HistoryFilter.mesesDe(emptyList(), utc))
    }
}
