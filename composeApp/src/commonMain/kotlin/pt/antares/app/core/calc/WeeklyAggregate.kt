package pt.antares.app.core.calc

import kotlinx.serialization.Serializable
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A semana inteira num objeto só. É serializável porque também é o que segue para o
 * resumo semanal e para o [AdaptiveTdee]: o cálculo e o ecrã leem os mesmos números.
 */
@Serializable
data class WeeklyAggregate(
    val weekStartEpochDay: Long,
    val weekEndEpochDay: Long,

    val loggedDays: Int,
    val avgKcal: Int,
    val targetKcal: Int,

    val daysOnTarget: Int,
    val avgProteinG: Int,
    val avgCarbsG: Int,
    val avgFatG: Int,

    val weighIns: Int,
    val weightStartKg: Double?,
    val weightEndKg: Double?,

    // Nulo com menos de duas pesagens na semana. Não é zero: zero afirmava que o peso
    // não mexeu, e a verdade é que não se sabe.
    val weightTrendDeltaKg: Double?,

    val workouts: Int,
    val workoutVolumeKg: Int,
    val exerciseKcal: Int,

    val fastingSessions: Int,
    val fastingAvgHours: Int,

    val runs: Int,
    val runDistanceKm: Double,
    val runMinutes: Int,

    val microGaps: Map<String, Int> = emptyMap(),
) {

    // Menos de quatro dias registados não descreve uma semana. Os ecrãs usam isto para
    // apresentar os números como parciais em vez de os afirmarem.
    val isSparse: Boolean get() = loggedDays < 4
}

object WeeklyAggregator {

    // 10% para cada lado. Acertar na meta ao kcal é impossível com porções reais, e uma
    // tolerância mais estreita fazia a contagem de dias bons dar quase sempre zero.
    const val ON_TARGET_TOLERANCE = 0.10

    /**
     * A semana da segunda ao domingo, a partir das listas já filtradas por data. Recebe
     * a semana anterior de pesagens só para dar passado à tendência.
     */
    fun build(
        weekStartEpochDay: Long,
        foodLogs: List<FoodLogEntity>,
        targetKcal: Int,
        weights: List<WeightLogEntity>,
        previousWeekWeights: List<WeightLogEntity> = emptyList(),
        workouts: List<WorkoutSessionEntity> = emptyList(),
        workoutVolumeKg: Int = 0,
        exerciseLogs: List<ExerciseLogEntity> = emptyList(),
        fastings: List<FastingSessionEntity> = emptyList(),
        runs: List<RunEntity> = emptyList(),
    ): WeeklyAggregate {
        val weekEnd = weekStartEpochDay + 6

        // Todas as contagens filtram `deleted`: o apagado fica na tabela como lápide, para
        // a linha do dia poder ser reaproveitada, e somá-lo dava dias que a pessoa já desfez.
        val byDay = foodLogs.filter { !it.deleted }.groupBy { it.epochDay }
        val dayKcal = byDay.mapValues { (_, logs) -> logs.sumOf { it.kcalSnapshot } }
        val loggedDays = dayKcal.size
        // Média sobre os dias registados, não sobre sete. Dias em branco são falta de
        // registo, não jejum, e diluí-los inventava um défice.
        val avgKcal = if (loggedDays == 0) 0 else dayKcal.values.sum() / loggedDays

        val daysOnTarget = if (targetKcal <= 0) 0 else {
            dayKcal.values.count { abs(it - targetKcal).toDouble() / targetKcal <= ON_TARGET_TOLERANCE }
        }

        fun avgMacro(selector: (FoodLogEntity) -> Double): Int {
            if (loggedDays == 0) return 0
            val total = byDay.values.sumOf { logs -> logs.sumOf(selector) }
            return (total / loggedDays).roundToInt()
        }

        val weekWeights = weights.filter { !it.deleted }.sortedBy { it.epochDay }
        val history = (previousWeekWeights.filter { !it.deleted } + weekWeights)
            .sortedBy { it.epochDay }
            .map { it.epochDay to it.weightKg }

        val trendDelta = if (weekWeights.size >= 2) {
            val series = WeightTrend.trendSeries(history)

            // O início é a última pesagem antes desta semana, não a primeira dela: a
            // tendência do primeiro dia já traz a semana anterior dentro e comparar contra
            // ela escondia parte da variação.
            val endTrend = series.lastOrNull()
            val startIndex = (history.size - weekWeights.size - 1).coerceAtLeast(0)
            val startTrend = series.getOrNull(startIndex)
            if (endTrend != null && startTrend != null) endTrend - startTrend else null
        } else {
            null
        }

        // Só jejuns terminados entram na média: um a decorrer ainda não tem duração, e
        // contá-lo com a hora atual puxava a média para baixo a cada leitura.
        val fastingHours = fastings.filter { !it.deleted }
            .mapNotNull { s -> s.endedAt?.let { end -> (end - s.startedAt) / 3_600_000.0 } }

        return WeeklyAggregate(
            weekStartEpochDay = weekStartEpochDay,
            weekEndEpochDay = weekEnd,
            loggedDays = loggedDays,
            avgKcal = avgKcal,
            targetKcal = targetKcal,
            daysOnTarget = daysOnTarget,
            avgProteinG = avgMacro { it.proteinSnapshot },
            avgCarbsG = avgMacro { it.carbsSnapshot },
            avgFatG = avgMacro { it.fatSnapshot },
            weighIns = weekWeights.size,
            weightStartKg = weekWeights.firstOrNull()?.weightKg,
            weightEndKg = weekWeights.lastOrNull()?.weightKg,
            // Duas casas: a tendência mexe gramas por dia, e uma casa só transformava
            // semanas diferentes no mesmo número.
            weightTrendDeltaKg = trendDelta?.let { (it * 100).roundToInt() / 100.0 },
            workouts = workouts.count { !it.deleted },
            workoutVolumeKg = workoutVolumeKg,
            exerciseKcal = exerciseLogs.filter { !it.deleted }.sumOf { it.kcal },
            fastingSessions = fastingHours.size,
            fastingAvgHours = if (fastingHours.isEmpty()) 0 else fastingHours.average().roundToInt(),
            runs = runs.count { !it.deleted },
            // Metros para quilómetros com uma casa, passando pelos hectómetros para o
            // arredondamento acontecer uma vez só.
            runDistanceKm = runs.filter { !it.deleted }
                .sumOf { it.distanceM }
                .let { (it / 100).roundToInt() / 10.0 },
            // Tempo em movimento, não tempo decorrido: as pausas não contam para o ritmo.
            runMinutes = runs.filter { !it.deleted }.sumOf { it.movingS }.toInt() / 60,
        )
    }
}
