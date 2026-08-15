package pt.antares.app.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

/**
 * Dias desde 1970-01-01, que é a unidade em que toda a app guarda datas: um número inteiro
 * não tem fuso nem hora, e o mesmo dia continua o mesmo dia depois de uma viagem.
 */
fun LocalDate.toEpochDay(): Long {

    return this.toEpochDays().toLong()
}

fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.fromEpochDays(epochDay.toInt())

fun todayEpochDay(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long =
    Clock.System.todayIn(timeZone).toEpochDay()

fun currentHour(timeZone: TimeZone = TimeZone.currentSystemDefault()): Int =
    Clock.System.now().toLocalDateTime(timeZone).hour

/**
 * Minutos desde a meia-noite local, de 0 a 1439. É a unidade em que a app guarda a hora a
 * que se comeu: cabe num inteiro, não arrasta fuso horário e compara-se por subtração.
 */
fun currentMinuteOfDay(timeZone: TimeZone = TimeZone.currentSystemDefault()): Int =
    Clock.System.now().toLocalDateTime(timeZone).let { it.hour * MINUTES_PER_HOUR + it.minute }

const val MINUTES_PER_HOUR = 60
const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

/**
 * A segunda-feira dessa semana. A app usa a semana ISO em todo o lado — relatório, grelha
 * de consistência, orçamento semanal — e é o que faz esses três concordarem.
 */
fun weekStartEpochDay(epochDay: Long): Long {
    val isoDay = epochDayToLocalDate(epochDay).dayOfWeek.isoDayNumber
    return epochDay - (isoDay - 1)
}

fun epochMillisToLocalDate(ms: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(timeZone).date
