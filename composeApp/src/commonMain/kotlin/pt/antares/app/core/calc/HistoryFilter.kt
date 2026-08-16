package pt.antares.app.core.calc

import kotlinx.datetime.TimeZone
import pt.antares.app.core.util.epochMillisToLocalDate

/**
 * Um mês, como filtro de histórico. Ano e mês juntos: filtrar só por «março» misturava o
 * março deste ano com o de há três, e é essa comparação que o histórico serve para não fazer.
 */
data class Mes(val ano: Int, val mes: Int) : Comparable<Mes> {
    override fun compareTo(other: Mes): Int =
        if (ano != other.ano) ano.compareTo(other.ano) else mes.compareTo(other.mes)
}

/**
 * Os filtros dos históricos de treino e de corrida.
 *
 * Não havia forma nenhuma de filtrar: quem tem duzentos treinos gravados percorre duzentos
 * cartões para chegar ao de fevereiro. A regra vive aqui, fora dos ecrãs, porque é a mesma
 * nos dois e porque um filtro que se engane não dá erro — dá uma lista mais curta.
 */
object HistoryFilter {

    fun mesDe(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): Mes =
        epochMillisToLocalDate(epochMillis, zone).let { Mes(it.year, it.monthNumber) }

    /**
     * Os meses que a lista cobre, do mais recente para o mais antigo — que é a ordem por que
     * a lista já se lê, e por isso a ordem em que se procura.
     */
    fun mesesDe(instantes: List<Long>, zone: TimeZone = TimeZone.currentSystemDefault()): List<Mes> =
        instantes.map { mesDe(it, zone) }.distinct().sortedDescending()

    fun <T> porMes(
        itens: List<T>,
        mes: Mes?,
        zone: TimeZone = TimeZone.currentSystemDefault(),
        instante: (T) -> Long,
    ): List<T> {
        // Sem mês escolhido a lista fica inteira: o filtro é uma redução voluntária, e não
        // um estado por onde se tenha de passar.
        if (mes == null) return itens
        return itens.filter { mesDe(instante(it), zone) == mes }
    }
}
