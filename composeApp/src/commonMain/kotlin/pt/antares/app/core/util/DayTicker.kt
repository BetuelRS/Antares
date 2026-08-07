package pt.antares.app.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object DayTicker {

    private val _today = MutableStateFlow(todayEpochDay())

    val today: StateFlow<Long> = _today

    fun refresh() {
        _today.value = todayEpochDay()
    }

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(msUntilNextMidnight() + 250)
                refresh()
            }
        }
    }

    fun msUntilNextMidnight(
        now: Instant = Clock.System.now(),
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long {
        val today = now.toLocalDateTime(zone).date
        val nextMidnight = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        return (nextMidnight - now).inWholeMilliseconds
    }
}

fun followDayChange(selected: Long, oldToday: Long, newToday: Long): Long =
    if (selected == oldToday) newToday else selected
