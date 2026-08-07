package pt.antares.app.core.calc

enum class ProgressRange(val days: Int?) {
    DAYS_30(30),
    MONTHS_3(90),
    YEAR(365),

    ALL(null),
    ;

    fun <T> clip(series: List<Pair<Long, T>>, today: Long): List<Pair<Long, T>> {
        val janela = days ?: return series
        return series.filter { it.first >= today - janela }
    }
}
