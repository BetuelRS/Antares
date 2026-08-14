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

/**
 * O dia de hoje como fluxo. Existe porque uma app aberta atravessa a meia-noite: sem isto,
 * o ecrã de hoje ficaria a mostrar ontem até alguém o fechar e reabrir.
 */
object DayTicker {

    private val _today = MutableStateFlow(todayEpochDay())

    val today: StateFlow<Long> = _today

    // Chamado também ao voltar do segundo plano: o alarme não corre com a app suspensa.
    fun refresh() {
        _today.value = todayEpochDay()
    }

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                // Dorme até à meia-noite seguinte em vez de acordar de minuto a minuto, e a
                // margem de um quarto de segundo garante que o relógio já virou o dia.
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

/**
 * Ao virar o dia, o diário acompanha se estava em hoje e fica quieto se a pessoa tinha
 * navegado para outra data. Mudar-lhe a data debaixo dos olhos enquanto revê uma semana
 * antiga seria perder o sítio onde estava.
 */
fun followDayChange(selected: Long, oldToday: Long, newToday: Long): Long =
    if (selected == oldToday) newToday else selected
