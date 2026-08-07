package pt.antares.app.core.calc

object CycleCalc {

    val PLAUSIBLE_GAP_DAYS = 15..60

    const val MIN_CYCLES_FOR_AVERAGE = 2

    fun averageCycleDays(startsChronological: List<Long>): Int? {
        val intervalos = gaps(startsChronological)
        if (intervalos.isEmpty()) return null
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

    fun dayOfCycle(startsChronological: List<Long>, today: Long): Int? {
        val ultimo = startsChronological.lastOrNull { it <= today } ?: return null
        return (today - ultimo + 1).toInt()
    }

    fun predictedNextStart(startsChronological: List<Long>): Long? {
        val ultimo = startsChronological.lastOrNull() ?: return null
        val comprimento = averageCycleDays(startsChronological) ?: return null
        return ultimo + comprimento
    }

    fun retentionLikely(startsChronological: List<Long>, today: Long): Boolean {
        val dia = dayOfCycle(startsChronological, today) ?: return false

        if (dia <= MENSES_WINDOW_DAYS) return true
        val comprimento = averageCycleDays(startsChronological) ?: return false

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
