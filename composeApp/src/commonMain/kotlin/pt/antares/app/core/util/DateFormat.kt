package pt.antares.app.core.util

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import org.jetbrains.compose.resources.stringArrayResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.months_short
import pt.antares.app.generated.resources.weekdays_short

@Composable
fun dayShort(date: LocalDate): String {
    val weekdays = stringArrayResource(Res.array.weekdays_short)
    val months = stringArrayResource(Res.array.months_short)

    val weekdayIndex = date.dayOfWeek.isoDayNumber % 7
    val weekday = weekdays.getOrElse(weekdayIndex) { "" }
    val month = months.getOrElse(date.monthNumber - 1) { "" }
    return "$weekday, ${date.dayOfMonth} $month"
}

@Composable
fun dayShort(epochDay: Long): String = dayShort(epochDayToLocalDate(epochDay))

@Composable
fun dayShortDated(epochDay: Long, today: Long = todayEpochDay()): String {
    val base = dayShort(epochDay)
    if (!DateStyle.needsYear(epochDay, today)) return base
    return "$base ${epochDayToLocalDate(epochDay).year}"
}

@Composable
fun axisDate(epochDay: Long, spanDays: Long): String {
    val date = epochDayToLocalDate(epochDay)
    val months = stringArrayResource(Res.array.months_short)
    val month = months.getOrElse(date.monthNumber - 1) { "" }
    return when (DateStyle.axisStyle(spanDays)) {
        DateStyle.AxisStyle.DAY_MONTH -> "${date.dayOfMonth} $month"

        DateStyle.AxisStyle.MONTH_YEAR -> "$month ${(date.year % 100).toString().padStart(2, '0')}"
    }
}
