package pt.antares.app.core.calc

/**
 * Ciclo menstrual a partir das datas de início registadas. Serve um fim concreto: avisar
 * que a retenção de água pode explicar o que a balança mostra, para a pessoa não reagir
 * a um quilo que não é gordura.
 */
object CycleCalc {

    // Fora deste intervalo não é um ciclo: abaixo de 15 dias é hemorragia intermédia
    // registada como início, acima de 60 é um registo em falta pelo meio.
    val PLAUSIBLE_GAP_DAYS = 15..60

    const val MIN_CYCLES_FOR_AVERAGE = 2

    /** Mediana, não média — daí o nome ser aproximado. */
    fun averageCycleDays(startsChronological: List<Long>): Int? {
        val intervalos = gaps(startsChronological)
        if (intervalos.isEmpty()) return null
        // Mediana e não média: um único ciclo atípico deslocava a previsão vários dias,
        // e o filtro de plausibilidade não apanha os que ficam dentro do intervalo.
        val ordenados = intervalos.sorted()
        val meio = ordenados.size / 2
        return if (ordenados.size % 2 == 1) {
            ordenados[meio].toInt()
        } else {
            ((ordenados[meio - 1] + ordenados[meio]) / 2.0).toInt()
        }
    }

    fun gaps(startsChronological: List<Long>): List<Long> {
        if (startsChronological.size < MIN_CYCLES_FOR_AVERAGE) return emptyList()
        return startsChronological
            .zipWithNext { anterior, seguinte -> seguinte - anterior }
            .filter { it.toInt() in PLAUSIBLE_GAP_DAYS }
    }

    /** Dia 1 é o próprio dia do início, como é convenção clínica — daí o `+ 1`. */
    fun dayOfCycle(startsChronological: List<Long>, today: Long): Int? {
        // Ignora inícios futuros para a contagem funcionar ao consultar dias passados no
        // histórico.
        val ultimo = startsChronological.lastOrNull { it <= today } ?: return null
        return (today - ultimo + 1).toInt()
    }

    fun predictedNextStart(startsChronological: List<Long>): Long? {
        val ultimo = startsChronological.lastOrNull() ?: return null
        val comprimento = averageCycleDays(startsChronological) ?: return null
        return ultimo + comprimento
    }

    /**
     * Se a retenção de líquidos é provável hoje. Não afirma nada sobre a pessoa: é o que
     * permite ao ecrã do peso sugerir uma causa em vez de deixar o número sozinho.
     */
    fun retentionLikely(startsChronological: List<Long>, today: Long): Boolean {
        val dia = dayOfCycle(startsChronological, today) ?: return false

        // Os primeiros dias respondem sem precisar de média: bastam para saber que estamos
        // na menstruação, e é a única janela que funciona com um ciclo só registado.
        if (dia <= MENSES_WINDOW_DAYS) return true
        val comprimento = averageCycleDays(startsChronological) ?: return false

        // A segunda janela é a semana anterior ao próximo início, contada para trás a
        // partir do comprimento previsto — é a fase lútea tardia.
        val faltam = comprimento - dia
        return faltam in 0 until LUTEAL_WINDOW_DAYS
    }

    fun periodLengthDays(startEpochDay: Long, endEpochDay: Long?): Int? {
        val fim = endEpochDay ?: return null
        if (fim < startEpochDay) return null
        return (fim - startEpochDay + 1).toInt()
    }

    const val MENSES_WINDOW_DAYS = 3

    const val LUTEAL_WINDOW_DAYS = 7
}
