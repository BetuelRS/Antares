package pt.antares.app.core.calc

data class TimeAxis(

    val firstDay: Long,

    val lastDay: Long,
) {

    val spanDays: Long get() = lastDay - firstDay

    fun fraction(epochDay: Long): Double {
        if (spanDays <= 0L) return 0.5
        return ((epochDay - firstDay).toDouble() / spanDays).coerceIn(0.0, 1.0)
    }

    fun tickDays(count: Int = DEFAULT_TICKS): List<Long> {
        if (count <= 0) return emptyList()
        if (spanDays <= 0L || count == 1) return listOf(firstDay)

        return (0 until count)
            .map { firstDay + (spanDays * it / (count - 1).toLong()) }
            .distinct()
    }

    companion object {

        const val DEFAULT_TICKS = 3

        fun of(days: List<Long>): TimeAxis? {
            if (days.isEmpty()) return null
            return TimeAxis(days.min(), days.max())
        }
    }
}
