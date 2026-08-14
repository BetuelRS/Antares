package pt.antares.app.core.calc

/**
 * O eixo horizontal dos gráficos. Separado do [ChartScale] porque o tempo tem regras
 * próprias: nunca leva folga, para o último ponto ficar mesmo na borda direita.
 */
data class TimeAxis(

    val firstDay: Long,

    val lastDay: Long,
) {

    val spanDays: Long get() = lastDay - firstDay

    fun fraction(epochDay: Long): Double {
        // Um único dia no gráfico desenha-se ao centro em vez de dividir por zero.
        if (spanDays <= 0L) return 0.5
        return ((epochDay - firstDay).toDouble() / spanDays).coerceIn(0.0, 1.0)
    }

    /** As datas a rotular, sempre incluindo as duas pontas. */
    fun tickDays(count: Int = DEFAULT_TICKS): List<Long> {
        if (count <= 0) return emptyList()
        if (spanDays <= 0L || count == 1) return listOf(firstDay)

        // A divisão por `count - 1` faz a última marca cair exatamente no último dia. O
        // `distinct` trata dos períodos curtos, onde várias marcas calham no mesmo dia.
        return (0 until count)
            .map { firstDay + (spanDays * it / (count - 1).toLong()) }
            .distinct()
    }

    companion object {

        // Três: início, meio e fim. Mais do que isto e as datas sobrepõem-se num telemóvel.
        const val DEFAULT_TICKS = 3

        fun of(days: List<Long>): TimeAxis? {
            if (days.isEmpty()) return null
            // Mínimo e máximo em vez de primeiro e último: o eixo não depende da lista vir
            // ordenada.
            return TimeAxis(days.min(), days.max())
        }
    }
}
