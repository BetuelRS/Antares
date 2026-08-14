package pt.antares.app.core.calc

/** Os períodos dos gráficos de progresso. */
enum class ProgressRange(val days: Int?) {
    DAYS_30(30),
    MONTHS_3(90),
    YEAR(365),

    // Dias a null quer dizer sem corte, e é por isso que o campo é anulável em vez de
    // levar um número grande a fazer de infinito.
    ALL(null),
    ;

    /**
     * Corta a série ao período, contando para trás a partir de hoje. Genérico no valor
     * porque a mesma janela serve pesos, medidas e volume de treino.
     */
    fun <T> clip(series: List<Pair<Long, T>>, today: Long): List<Pair<Long, T>> {
        val janela = days ?: return series
        // Sem teto superior: pontos com data futura, vindos de um relógio adiantado,
        // continuam a aparecer em vez de desaparecerem sem explicação.
        return series.filter { it.first >= today - janela }
    }
}
