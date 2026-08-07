package pt.antares.app.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

fun LocalDate.toEpochDay(): Long {

    return this.toEpochDays().toLong()
}

fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.fromEpochDays(epochDay.toInt())

fun todayEpochDay(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long =
    Clock.System.todayIn(timeZone).toEpochDay()

fun currentHour(timeZone: TimeZone = TimeZone.currentSystemDefault()): Int =
    Clock.System.now().toLocalDateTime(timeZone).hour

fun weekStartEpochDay(epochDay: Long): Long {
    val isoDay = epochDayToLocalDate(epochDay).dayOfWeek.isoDayNumber
    return epochDay - (isoDay - 1)
}

fun epochMillisToLocalDate(ms: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(timeZone).date
