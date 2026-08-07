package pt.antares.app.core.coach

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import pt.antares.app.core.ai.AiClient
import pt.antares.app.core.ai.CoachAdaptive
import pt.antares.app.core.nutrition.CoverageCalc
import pt.antares.app.core.notifications.CoachNotifier
import pt.antares.app.core.notifications.NoopCoachNotifier
import pt.antares.app.core.calc.AdaptiveTdee
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.calc.WeightTrend
import pt.antares.app.core.calc.WeeklyAggregate
import pt.antares.app.core.calc.WeeklyAggregator
import pt.antares.app.core.database.daos.CoachReportDao
import pt.antares.app.core.database.daos.DailyTargetOverrideDao
import pt.antares.app.core.database.daos.ExerciseLogDao
import pt.antares.app.core.database.daos.FastingSessionDao
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.RunDao
import pt.antares.app.core.database.daos.UserProfileDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.database.daos.WorkoutSessionDao
import pt.antares.app.core.database.daos.WorkoutSetDao
import pt.antares.app.core.database.entities.CoachReportEntity
import pt.antares.app.core.database.entities.DailyTargetOverrideEntity
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.core.util.weekStartEpochDay
import kotlin.math.roundToInt

class CoachRepository(
    private val coachDao: CoachReportDao,
    private val foodLogDao: FoodLogDao,
    private val weightDao: WeightLogDao,
    private val profileDao: UserProfileDao,
    private val overrideDao: DailyTargetOverrideDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutSetDao: WorkoutSetDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val fastingDao: FastingSessionDao,
    private val runDao: RunDao,
    private val statsRepository: pt.antares.app.feature.stats.NutritionStatsRepository,
    private val client: AiClient,
    private val prefs: AppPreferences,

    private val ensureAccount: suspend () -> Unit,

    private val notifier: CoachNotifier = NoopCoachNotifier(),
    private val io: CoroutineDispatcher,
    private val lang: () -> String = { "pt" },
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
    private val json: Json = Json,
    private val newId: () -> String = { Ids.newUuid() },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val today: () -> Long = { todayEpochDay(zone) },
) {

    fun observeReports(): Flow<List<CoachReportEntity>> = coachDao.observeAll()

    fun observeLatest(): Flow<CoachReportEntity?> = coachDao.observeLatest()

    suspend fun byId(id: String): CoachReportEntity? = withContext(io) { coachDao.byId(id) }

    suspend fun generateIfDue(): Boolean = withContext(io) {

        val week = CoachTrigger.targetWeekStart(today())
        val lastWeek = coachDao.exportRows().maxOfOrNull { it.weekStartEpochDay }

        val aggregate = buildAggregate(week) ?: return@withContext false
        if (!CoachTrigger.shouldGenerate(today(), lastWeek, aggregate.loggedDays)) {
            return@withContext false
        }
        val ok = runCatching { generate(week, aggregate) }.getOrNull() is AppResult.Success

        if (ok) notifier.notifyReportReady()
        ok
    }

    suspend fun generateManual(): AppResult<CoachReportEntity> = withContext(io) {
        val today = today()
        val currentWeek = weekStartEpochDay(today)
        val previousWeek = currentWeek - 7
        val logged = foodLogDao.loggedDaysSince(previousWeek).toSet()
        val loggedPrev = logged.count { it in previousWeek..(previousWeek + 6) }
        val loggedCurrent = logged.count { it in currentWeek..(currentWeek + 6) }
        val weekStart = CoachTrigger.manualWeekStart(today, loggedPrev, loggedCurrent)
        generate(weekStart)
    }

    suspend fun generate(
        weekStart: Long = CoachTrigger.targetWeekStart(today()),
        prebuilt: WeeklyAggregate? = null,
    ): AppResult<CoachReportEntity> = withContext(io) {
        val aggregate = prebuilt ?: buildAggregate(weekStart)
            ?: return@withContext AppResult.Failure(AppError.Unknown("no profile"))

        val proposal = evaluateAdaptive(aggregate)

        val existing = coachDao.byWeekForWrite(weekStart)
        val entity = CoachReportEntity(
            id = existing?.id ?: newId(),
            weekStartEpochDay = weekStart,

            winsJson = "[]",
            observationsJson = "[]",
            adjustmentsJson = "[]",
            focus = "",
            aggregateJson = json.encodeToString(aggregate),
            proposedKcal = proposal?.newTargetKcal,
            previousKcal = proposal?.previousTargetKcal,
            observedTdee = proposal?.observedTdee,

            createdAt = existing?.takeIf { !it.deleted }?.createdAt ?: now(),
            updatedAt = now(),
        )
        coachDao.upsert(entity)
        AppResult.Success(entity)
    }

    suspend fun buildAggregate(weekStart: Long): WeeklyAggregate? = withContext(io) {
        val profile = profileDao.get() ?: return@withContext null
        val weekEnd = weekStart + 6

        val fromMs = epochDayToLocalDate(weekStart).atStartOfDayIn(zone).toEpochMilliseconds()
        val toMs = epochDayToLocalDate(weekEnd)
            .atTime(LocalTime(23, 59, 59)).toInstant(zone).toEpochMilliseconds()

        val totals = statsRepository.totals(weekStart, weekEnd)
        val gaps = if (totals.measuredAnyPct >= MICRO_MIN_COVERAGE) {
            CoverageCalc.compute(totals, profile.sex, statsRepository.loadReference().all(), profile.lifeStage)
                .filter { it.hasData && !it.isPartial }

                .map { it.key to it.coveragePct / DAYS_IN_WEEK }
                .filter { (_, dailyPct) -> dailyPct < MICRO_GAP_BELOW }
                .toMap()
        } else {
            emptyMap()
        }

        WeeklyAggregator.build(
            weekStartEpochDay = weekStart,
            foodLogs = foodLogDao.logsInRange(weekStart, weekEnd),
            targetKcal = targetKcalFor(weekStart, profile),
            weights = weightDao.range(weekStart, weekEnd),

            previousWeekWeights = weightDao.range(weekStart - 7, weekStart - 1),
            workouts = workoutSessionDao.sessionsBetween(fromMs, toMs),
            workoutVolumeKg = workoutSetDao.volumeBetween(fromMs, toMs).roundToInt(),
            exerciseLogs = exerciseLogDao.logsInRange(weekStart, weekEnd),
            fastings = fastingDao.sessionsBetween(fromMs, toMs),
            runs = runDao.runsBetween(fromMs, toMs),
        ).copy(microGaps = gaps)
    }

    private suspend fun targetKcalFor(weekStart: Long, profile: UserProfileEntity): Int {
        val override = overrideDao.byDay(weekStart)
        if (override != null) return override.kcal
        val weight = weightDao.latest()?.weightKg ?: DEFAULT_WEIGHT_KG
        return NutritionCalc.dailyTargets(profile, weight, weekStart).kcal
    }

    suspend fun evaluateAdaptive(aggregate: WeeklyAggregate): AdaptiveTdee.Proposal? =
        withContext(io) {

            if (!prefs.adaptiveTargets.first()) return@withContext null

            val profile = profileDao.get() ?: return@withContext null
            val trend = aggregate.weightTrendDeltaKg ?: return@withContext null

            val currentTdee = (aggregate.targetKcal - profile.goalRateKcal).toDouble()

            val result = AdaptiveTdee.evaluate(
                AdaptiveTdee.WeekInput(
                    avgIntakeKcal = aggregate.avgKcal.toDouble(),
                    loggedDays = aggregate.loggedDays,
                    weightTrendDeltaKg = trend,
                    weighIns = aggregate.weighIns,
                    currentTdee = currentTdee,
                    goalRateKcal = profile.goalRateKcal,
                    sex = profile.sex,

                    bmr = weightDao.latest()?.weightKg?.let { w ->
                        NutritionCalc.energy(profile, w, today()).bmr
                    },

                    consecutiveStallWeeks = WeightTrend.consecutiveStallWeeks(
                        weightDao.exportRows()
                            .sortedBy { it.epochDay }
                            .map { it.epochDay to it.weightKg },
                    ),
                ),
            )
            when (result) {
                is AdaptiveTdee.Result.Skip -> null
                is AdaptiveTdee.Result.Propose ->
                    result.proposal.takeIf { it.isMeaningful }
            }
        }

    suspend fun acceptProposal(report: CoachReportEntity): AppResult<Unit> = withContext(io) {
        val kcal = report.proposedKcal
            ?: return@withContext AppResult.Failure(AppError.Unknown("no proposal"))
        val profile = profileDao.get()
            ?: return@withContext AppResult.Failure(AppError.Unknown("no profile"))
        val weight = weightDao.latest()?.weightKg ?: DEFAULT_WEIGHT_KG

        val base = NutritionCalc.dailyTargets(profile, weight, today())
        val scaled = scaleMacros(kcal, base.proteinG, base.carbsG, base.fatG)

        val weekStart = report.weekStartEpochDay + 7
        for (d in weekStart until weekStart + 7) {
            overrideDao.upsert(
                DailyTargetOverrideEntity(
                    id = "adaptive-$d",
                    epochDay = d,
                    kcal = kcal,
                    proteinG = scaled.first,
                    carbsG = scaled.second,
                    fatG = scaled.third,
                    source = SOURCE_ADAPTIVE,
                    updatedAt = now(),
                ),
            )
        }
        coachDao.setProposalAccepted(report.id, true, now())
        AppResult.Success(Unit)
    }

    suspend fun dismissProposal(report: CoachReportEntity) = withContext(io) {
        coachDao.setProposalAccepted(report.id, false, now())
    }

    private fun scaleMacros(kcal: Int, protein: Int, carbs: Int, fat: Int): Triple<Int, Int, Int> {
        val fromPF = protein * NutritionCalc.KCAL_PER_G_PROTEIN + fat * NutritionCalc.KCAL_PER_G_FAT
        val carbKcal = (kcal - fromPF).coerceAtLeast(0)
        val newCarbs = carbKcal / NutritionCalc.KCAL_PER_G_CARB
        return Triple(protein, if (newCarbs > 0) newCarbs else carbs, fat)
    }

    companion object {
        const val SOURCE_ADAPTIVE = "ADAPTIVE"
        const val DEFAULT_WEIGHT_KG = 70.0

        const val MICRO_MIN_COVERAGE = 60

        const val MICRO_GAP_BELOW = 70

        private const val DAYS_IN_WEEK = 7
    }
}
