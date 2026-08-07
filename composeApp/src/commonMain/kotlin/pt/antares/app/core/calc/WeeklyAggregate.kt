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

    val isSparse: Boolean get() = loggedDays < 4
}

object WeeklyAggregator {

    const val ON_TARGET_TOLERANCE = 0.10

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

        val byDay = foodLogs.filter { !it.deleted }.groupBy { it.epochDay }
        val dayKcal = byDay.mapValues { (_, logs) -> logs.sumOf { it.kcalSnapshot } }
        val loggedDays = dayKcal.size
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

            val endTrend = series.lastOrNull()
            val startIndex = (history.size - weekWeights.size - 1).coerceAtLeast(0)
            val startTrend = series.getOrNull(startIndex)
            if (endTrend != null && startTrend != null) endTrend - startTrend else null
        } else {
            null
        }

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
            weightTrendDeltaKg = trendDelta?.let { (it * 100).roundToInt() / 100.0 },
            workouts = workouts.count { !it.deleted },
            workoutVolumeKg = workoutVolumeKg,
            exerciseKcal = exerciseLogs.filter { !it.deleted }.sumOf { it.kcal },
            fastingSessions = fastingHours.size,
            fastingAvgHours = if (fastingHours.isEmpty()) 0 else fastingHours.average().roundToInt(),
            runs = runs.count { !it.deleted },
            runDistanceKm = runs.filter { !it.deleted }
                .sumOf { it.distanceM }
                .let { (it / 100).roundToInt() / 10.0 },
            runMinutes = runs.filter { !it.deleted }.sumOf { it.movingS }.toInt() / 60,
        )
    }
}
