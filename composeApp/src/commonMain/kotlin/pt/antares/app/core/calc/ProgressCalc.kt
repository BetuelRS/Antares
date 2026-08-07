package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.roundToInt

object ProgressCalc {

    data class DayCell(val epochDay: Long, val logged: Boolean, val inFuture: Boolean)

    fun consistencyGrid(
        loggedDays: Set<Long>,
        today: Long,
        weeks: Int = CONSISTENCY_WEEKS,
    ): List<DayCell> {
        if (weeks <= 0) return emptyList()

        val diaDaSemana = ((today + 3) % 7 + 7) % 7
        val ultimaSegunda = today - diaDaSemana
        val primeiraSegunda = ultimaSegunda - (weeks - 1) * 7L
        return (0 until weeks * 7).map { i ->
            val dia = primeiraSegunda + i
            DayCell(
                epochDay = dia,
                logged = dia in loggedDays,
                inFuture = dia > today,
            )
        }
    }

    fun consistencyPct(grid: List<DayCell>): Int {
        val passados = grid.filterNot { it.inFuture }
        if (passados.isEmpty()) return 0
        return (passados.count { it.logged } * 100.0 / passados.size).roundToInt()
    }

    data class Comparison(
        val current: Double,
        val previous: Double?,
    ) {
        val delta: Double? get() = previous?.let { current - it }

        val deltaPct: Double?
            get() {
                val ant = previous ?: return null
                if (abs(ant) < EPSILON) return null
                return (current - ant) / abs(ant) * 100.0
            }

        val direction: Direction
            get() {
                val d = delta ?: return Direction.UNKNOWN
                return when {
                    abs(d) < EPSILON -> Direction.FLAT
                    d > 0 -> Direction.UP
                    else -> Direction.DOWN
                }
            }
    }

    enum class Direction { UP, DOWN, FLAT, UNKNOWN }

    fun meanOrNull(values: List<Double>): Double? =
        if (values.isEmpty()) null else values.sum() / values.size

    fun compare(
        currentValues: List<Double>,
        previousValues: List<Double>,
        minDays: Int = MIN_DAYS_TO_COMPARE,
    ): Comparison? {
        val atual = meanOrNull(currentValues) ?: return null
        if (currentValues.size < minDays) return null
        val anterior = meanOrNull(previousValues).takeIf { previousValues.size >= minDays }
        return Comparison(current = atual, previous = anterior)
    }

    data class Milestone(val kind: Kind, val value: Int, val epochDay: Long)

    enum class Kind {

        LOGGING_DAYS,

        WEIGHT_CHANGE_KG,

        GOAL_REACHED,
    }

    val LOGGING_MARKS = listOf(7, 30, 100, 365)

    const val WEIGHT_STEP_KG = 5

    fun loggingMilestones(loggedDays: Set<Long>): List<Milestone> {
        if (loggedDays.isEmpty()) return emptyList()
        val ordenados = loggedDays.sorted()
        return LOGGING_MARKS
            .filter { it <= ordenados.size }
            .map { marca -> Milestone(Kind.LOGGING_DAYS, marca, ordenados[marca - 1]) }
    }

    fun weightMilestones(weighIns: List<Pair<Long, Double>>): List<Milestone> {
        if (weighIns.size < 2) return emptyList()
        val ordenadas = weighIns.sortedBy { it.first }
        val partida = ordenadas.first().second
        val out = mutableListOf<Milestone>()
        var proximaMarca = WEIGHT_STEP_KG
        for ((dia, kg) in ordenadas) {
            val percorrido = abs(kg - partida)
            while (percorrido >= proximaMarca) {
                out += Milestone(Kind.WEIGHT_CHANGE_KG, proximaMarca, dia)
                proximaMarca += WEIGHT_STEP_KG
            }
        }
        return out
    }

    private const val EPSILON = 1e-9
    const val CONSISTENCY_WEEKS = 12
    const val MIN_DAYS_TO_COMPARE = 5
}
