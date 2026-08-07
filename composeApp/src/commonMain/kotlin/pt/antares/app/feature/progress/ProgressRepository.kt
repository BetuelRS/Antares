package pt.antares.app.feature.progress

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.calc.BeenHereCalc
import pt.antares.app.core.calc.EatingPatterns
import pt.antares.app.core.calc.GoalHistoryCalc
import pt.antares.app.core.calc.ProgressCalc
import pt.antares.app.core.database.daos.BodyMeasurementDao
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.GoalHistoryDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.database.entities.GoalHistoryEntity
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.todayEpochDay

class ProgressRepository(
    private val weightDao: WeightLogDao,
    private val foodLogDao: FoodLogDao,
    private val goalDao: GoalHistoryDao,
    private val measurementDao: BodyMeasurementDao,
    private val io: CoroutineDispatcher,
) {

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    suspend fun weighIns(): List<Pair<Long, Double>> = withContext(io) {
        weightDao.exportRows().sortedBy { it.epochDay }.map { it.epochDay to it.weightKg }
    }

    suspend fun loggedDays(days: Int, today: Long = todayEpochDay()): Set<Long> = withContext(io) {
        foodLogDao.loggedDaysSince(today - days).toSet()
    }

    suspend fun dailyKcal(from: Long, to: Long): List<Double> = withContext(io) {
        foodLogDao.logsInRange(from, to)
            .groupBy { it.epochDay }
            .map { (_, logs) -> logs.sumOf { it.kcalSnapshot }.toDouble() }
    }

    suspend fun patternDays(days: Int, today: Long = todayEpochDay()): List<EatingPatterns.Day> =
        withContext(io) {
            foodLogDao.logsInRange(today - days, today)
                .groupBy { it.epochDay }
                .map { (dia, logs) ->
                    EatingPatterns.Day(
                        epochDay = dia,
                        kcal = logs.sumOf { it.kcalSnapshot }.toDouble(),
                        proteinG = logs.sumOf { it.proteinSnapshot },
                        kcalBySlot = logs.groupBy { it.mealSlot.name }
                            .mapValues { (_, l) -> l.sumOf { it.kcalSnapshot }.toDouble() },
                    )
                }
                .sortedBy { it.epochDay }
        }

    suspend fun lastVisitAtCurrentWeight(today: Long = todayEpochDay()): BeenHereCalc.Visit? =
        withContext(io) {
            val pesagens = weightDao.exportRows()
                .sortedBy { it.epochDay }
                .map { it.epochDay to it.weightKg }
            val atual = pesagens.lastOrNull()?.second ?: return@withContext null
            val medicoes = measurementDao.all()
            BeenHereCalc.lastVisit(
                currentWeightKg = atual,
                today = today,
                weighIns = pesagens,
                waistByDay = medicoes.mapNotNull { m -> m.waistCm?.let { m.epochDay to it } },
                bodyFatByDay = medicoes.mapNotNull { m -> m.bodyFatPct?.let { m.epochDay to it } },
            )
        }

    suspend fun goalTimeline(): List<GoalHistoryCalc.Goal> = withContext(io) {
        val pesagens = weightDao.exportRows()
            .sortedBy { it.epochDay }
            .map { it.epochDay to it.weightKg }
        val linhas = goalDao.all()
        val objetivos = linhas.map {
            GoalHistoryCalc.Goal(
                targetKg = it.targetKg,
                setOnEpochDay = it.setOnEpochDay,
                startWeightKg = it.startWeightKg,
                reachedOnEpochDay = it.reachedOnEpochDay,
            )
        }
        val fechados = GoalHistoryCalc.settle(objetivos, pesagens)

        fechados.forEachIndexed { i, goal ->
            val linha = linhas[i]
            if (goal.reachedOnEpochDay != null && linha.reachedOnEpochDay == null) {
                goalDao.upsert(
                    linha.copy(
                        reachedOnEpochDay = goal.reachedOnEpochDay,
                        updatedAt = now(),
                        dirty = true,
                    ),
                )
            }
        }
        fechados
    }

    suspend fun milestones(today: Long = todayEpochDay()): List<ProgressCalc.Milestone> =
        withContext(io) {
            val dias = foodLogDao.loggedDaysSince(0).toSet()
            val pesagens = weighIns()
            (ProgressCalc.loggingMilestones(dias) + ProgressCalc.weightMilestones(pesagens))
                .sortedBy { it.epochDay }
        }
}
