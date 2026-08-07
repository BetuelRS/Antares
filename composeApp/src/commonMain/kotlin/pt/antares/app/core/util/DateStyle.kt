package pt.antares.app.core.util

object DateStyle {

    fun needsYear(epochDay: Long, todayEpochDay: Long): Boolean =
        epochDayToLocalDate(epochDay).year != epochDayToLocalDate(todayEpochDay).year

    fun axisStyle(spanDays: Long): AxisStyle =
        if (spanDays >= LONG_SPAN_DAYS) AxisStyle.MONTH_YEAR else AxisStyle.DAY_MONTH

    enum class AxisStyle {

        DAY_MONTH,

        MONTH_YEAR,
    }

    const val LONG_SPAN_DAYS = 540L
}
