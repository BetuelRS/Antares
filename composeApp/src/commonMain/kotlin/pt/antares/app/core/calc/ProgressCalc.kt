package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.roundToInt

object ProgressCalc {

    // `inFuture` distingue-se de `logged = false`: os dias que ainda não chegaram são
    // desenhados a vazio mas não contam como falha.
    /**
     * Um dia da grelha. [antesDeComecar] é para os dias anteriores ao primeiro registo: a
     * grelha mostra doze semanas, e quem instalou a app ontem não falhou as onze anteriores.
     */
    data class DayCell(
        val epochDay: Long,
        val logged: Boolean,
        val inFuture: Boolean,
        val antesDeComecar: Boolean = false,
    )

    /**
     * A grelha de consistência, sempre a começar numa segunda-feira e a terminar no
     * domingo da semana corrente — inclui portanto dias futuros.
     */
    fun consistencyGrid(
        loggedDays: Set<Long>,
        today: Long,
        weeks: Int = CONSISTENCY_WEEKS,
    ): List<DayCell> {
        if (weeks <= 0) return emptyList()

        // O primeiro dia com registo é o dia em que a pessoa começou. Antes dele não há
        // falha nenhuma a contar — havia app, não havia esta pessoa.
        val comecou = loggedDays.minOrNull()

        // O dia 0 da era é uma quinta-feira; somar 3 antes do resto põe a segunda em zero.
        // O segundo `% 7` existe para datas anteriores a 1970, onde o resto sai negativo.
        val diaDaSemana = ((today + 3) % 7 + 7) % 7
        val ultimaSegunda = today - diaDaSemana
        val primeiraSegunda = ultimaSegunda - (weeks - 1) * 7L
        return (0 until weeks * 7).map { i ->
            val dia = primeiraSegunda + i
            DayCell(
                epochDay = dia,
                logged = dia in loggedDays,
                inFuture = dia > today,
                antesDeComecar = comecou != null && dia < comecou,
            )
        }
    }

    /**
     * A percentagem de dias com registo **desde que a pessoa começou**.
     *
     * Os dias por vir saem da conta: incluí-los fazia a percentagem cair todas as
     * segundas-feiras e recuperar ao longo da semana sem nada ter mudado.
     *
     * Os dias anteriores ao primeiro registo saem também, e essa é a correção que importa.
     * A grelha tem 84 dias; quem instalou a app ontem e registou o dia de ontem lia «1%» —
     * um número sobre onze semanas em que não havia ninguém para falhar. Agora lê 100%,
     * que é o que aconteceu.
     */
    fun consistencyPct(grid: List<DayCell>): Int {
        val contam = grid.filterNot { it.inFuture || it.antesDeComecar }
        if (contam.isEmpty()) return 0
        return (contam.count { it.logged } * 100.0 / contam.size).roundToInt()
    }

    data class Comparison(
        val current: Double,
        val previous: Double?,
    ) {
        val delta: Double? get() = previous?.let { current - it }

        val deltaPct: Double?
            get() {
                val ant = previous ?: return null
                // Sem período anterior não há percentagem: subir de zero é infinito, e
                // mostrar 100% seria inventar uma escala.
                if (abs(ant) < EPSILON) return null
                // O denominador vai em valor absoluto para o sinal vir da subtração — com
                // um valor anterior negativo a percentagem saía ao contrário.
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

    /**
     * Compara dois períodos. Devolve null se o período atual for curto de mais; se for só
     * o anterior a faltar, devolve a comparação com `previous` a null — o valor de hoje
     * mostra-se na mesma, apenas sem seta.
     */
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

    // Marcos de 5 em 5 kg, em qualquer direção: quem está a ganhar massa também os recebe.
    const val WEIGHT_STEP_KG = 5

    /**
     * Dias registados, não dias seguidos: isto não é uma sequência. A data do marco é a do
     * enésimo dia registado, para o histórico ficar no sítio certo mesmo com falhas pelo meio.
     */
    fun loggingMilestones(loggedDays: Set<Long>): List<Milestone> {
        if (loggedDays.isEmpty()) return emptyList()
        val ordenados = loggedDays.sorted()
        return LOGGING_MARKS
            .filter { it <= ordenados.size }
            .map { marca -> Milestone(Kind.LOGGING_DAYS, marca, ordenados[marca - 1]) }
    }

    /** Marcos de peso medidos desde a primeira pesagem de sempre, não desde o objetivo. */
    fun weightMilestones(weighIns: List<Pair<Long, Double>>): List<Milestone> {
        if (weighIns.size < 2) return emptyList()
        val ordenadas = weighIns.sortedBy { it.first }
        val partida = ordenadas.first().second
        val out = mutableListOf<Milestone>()
        var proximaMarca = WEIGHT_STEP_KG
        for ((dia, kg) in ordenadas) {
            val percorrido = abs(kg - partida)
            // `while` e não `if`: um intervalo grande entre pesagens pode saltar vários
            // marcos de uma vez, e todos ficam datados nessa pesagem.
            while (percorrido >= proximaMarca) {
                out += Milestone(Kind.WEIGHT_CHANGE_KG, proximaMarca, dia)
                proximaMarca += WEIGHT_STEP_KG
            }
        }
        // `proximaMarca` nunca recua: um regresso ao peso anterior não apaga o marco que
        // já foi atingido nem o volta a dar quando se passa outra vez.
        return out
    }

    private const val EPSILON = 1e-9
    // Doze semanas de grelha: cabe num ecrã de telemóvel sem células ilegíveis.
    const val CONSISTENCY_WEEKS = 12
    const val MIN_DAYS_TO_COMPARE = 5
}
