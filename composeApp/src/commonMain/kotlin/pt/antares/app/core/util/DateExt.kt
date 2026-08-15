package pt.antares.app.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
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
 * O instante em que aquele minuto daquele dia aconteceu, no fuso do telemóvel.
 *
 * É a ponte entre as duas maneiras de a app guardar tempo: o diário guarda dia e minuto
 * local, o jejum guarda instantes em milissegundos. Sem esta conversão os dois nunca se
 * conseguem comparar.
 */
fun epochMillisAt(
    epochDay: Long,
    minuteOfDay: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Long = LocalDateTime(
    date = epochDayToLocalDate(epochDay),
    time = LocalTime(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR),
).toInstant(timeZone).toEpochMilliseconds()

/**
 * O caminho inverso: a que minuto do dia local aconteceu aquele instante.
 */
fun minuteOfDayAt(
    epochMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Int = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
    .let { it.hour * MINUTES_PER_HOUR + it.minute }

/**
 * Uma duração como `12h 45m`, ou `45m` abaixo de uma hora.
 *
 * É o mesmo formato do relógio do jejum, de propósito: a janela alimentar e o jejum são
 * dois lados do mesmo dia, e aparecem lado a lado.
 */
fun formatDurationMin(minutes: Int): String {
    val total = if (minutes < 0) -minutes else minutes
    val h = total / MINUTES_PER_HOUR
    val m = total % MINUTES_PER_HOUR
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/**
 * Uma hora do dia como `08:05`, a partir dos minutos desde a meia-noite.
 *
 * Sempre em 24 horas, e sem `am`/`pm` mesmo em inglês: a app mostra estas horas ao lado de
 * durações de jejum e de janelas alimentares, e misturar os dois formatos na mesma linha
 * lê-se pior do que usar um só.
 */
fun formatMinuteOfDay(minuteOfDay: Int): String {
    val m = minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
    val horas = m / MINUTES_PER_HOUR
    val minutos = m % MINUTES_PER_HOUR
    return "${horas.toString().padStart(2, '0')}:${minutos.toString().padStart(2, '0')}"
}

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
