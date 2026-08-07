package pt.antares.app.core.health

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pt.antares.app.core.calc.MetCalc
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.model.WeightSource
import pt.antares.app.core.util.Ids

data class HealthImport(
    val weights: Int = 0,
    val sessions: Int = 0,

    val skippedDuplicates: Int = 0,

    val bodyMeasurements: Int = 0,
) {
    val isEmpty: Boolean get() = weights == 0 && sessions == 0 && bodyMeasurements == 0
}

class HealthRepository(
    private val gateway: HealthGateway,
    private val weights: WeightWriter,
    private val exercise: ExerciseWriter,
    private val ownWindows: OwnSessionWindows,
    private val latestWeightKg: suspend () -> Double?,
    private val lastImportAt: suspend () -> Long,
    private val setLastImportAt: suspend (Long) -> Unit,
    private val io: CoroutineDispatcher,
    private val now: () -> Long,
    private val epochDayOf: (Long) -> Long,
    private val newId: () -> String = { Ids.newUuid() },

    private val measurements: MeasurementWriter? = null,
) {

    fun interface MeasurementWriter {
        suspend fun record(epochDay: Long, bodyFatPct: Double)
    }

    interface WeightWriter {
        suspend fun importedRefs(): Set<String>
        suspend fun existsOnDay(epochDay: Long): Boolean
        suspend fun insert(entry: WeightLogEntity)
    }

    interface ExerciseWriter {
        suspend fun importedRefs(): Set<String>
        suspend fun insert(log: ExerciseLogEntity)
    }

    fun interface OwnSessionWindows {
        suspend fun since(fromMs: Long): List<TimeWindow>
    }

    fun availability(): HealthAvailability = gateway.availability()

    suspend fun hasPermissions(): Boolean = gateway.hasReadPermissions()

    val permissions: Set<String> get() = gateway.readPermissions

    suspend fun stepsToday(startOfDayMs: Long, endOfDayMs: Long): Long? = withContext(io) {
        if (!gateway.hasReadPermissions()) null else gateway.steps(startOfDayMs, endOfDayMs)
    }

    suspend fun stepsPerDay(todayStartMs: Long, days: Int): List<Long> = withContext(io) {
        if (!gateway.hasReadPermissions()) return@withContext emptyList()
        (days downTo 1).mapNotNull { atras ->
            val inicio = todayStartMs - atras * DAY_MS
            gateway.steps(inicio, inicio + DAY_MS)
        }
    }

    suspend fun importNow(): HealthImport = withContext(io) {
        if (gateway.availability() != HealthAvailability.AVAILABLE) return@withContext HealthImport()
        if (!gateway.hasReadPermissions()) return@withContext HealthImport()

        val startedAt = now()

        val since = lastImportAt().takeIf { it > 0 } ?: (startedAt - FIRST_IMPORT_WINDOW_MS)

        val importedWeights = importWeights(since)
        val importedMeasurements = importBodyComposition(since)
        val (importedSessions, skipped) = importSessions(since)

        setLastImportAt(startedAt)

        HealthImport(
            weights = importedWeights,
            sessions = importedSessions,
            skippedDuplicates = skipped,
            bodyMeasurements = importedMeasurements,
        )
    }

    private suspend fun importBodyComposition(since: Long): Int {
        val writer = measurements ?: return 0
        var count = 0
        for (m in gateway.bodyComposition(since)) {
            val pct = m.bodyFatPct ?: continue
            writer.record(epochDayOf(m.timestampMs), pct)
            count++
        }
        return count
    }

    private suspend fun importWeights(since: Long): Int {
        val known = weights.importedRefs()
        var count = 0
        for (w in gateway.weights(since)) {
            if (w.uid in known) continue
            val day = epochDayOf(w.timestampMs)
            if (weights.existsOnDay(day)) continue
            weights.insert(
                WeightLogEntity(
                    id = newId(),
                    epochDay = day,
                    weightKg = w.kg,
                    note = null,
                    source = WeightSource.HEALTH_CONNECT,
                    sourceRef = w.uid,
                    updatedAt = now(),
                ),
            )
            count++
        }
        return count
    }

    private suspend fun importSessions(since: Long): Pair<Int, Int> {
        val known = exercise.importedRefs()

        val own = ownWindows.since(since - OWN_WINDOW_SLACK_MS)
        val weightKg = latestWeightKg() ?: DEFAULT_WEIGHT_KG

        var imported = 0
        var skipped = 0

        for (s in gateway.sessions(since)) {
            if (s.uid in known) continue

            if (HealthDedupe.isDuplicate(TimeWindow(s.startMs, s.endMs), own)) {
                skipped++
                continue
            }

            val durationMin = ((s.endMs - s.startMs) / 60_000L).toInt()
            if (durationMin <= 0) continue

            val met = s.met

            val kcal = s.kcal ?: MetCalc.kcal(met ?: 0.0, weightKg, durationMin)

            exercise.insert(
                ExerciseLogEntity(
                    id = newId(),
                    epochDay = epochDayOf(s.startMs),
                    origin = ExerciseOrigin.HEALTH_CONNECT,
                    label = s.title?.takeIf { it.isNotBlank() } ?: s.activity,
                    metId = null,
                    met = met,
                    durationMin = durationMin,
                    kcal = kcal,
                    refId = s.uid,
                    updatedAt = now(),
                ),
            )
            imported++
        }
        return imported to skipped
    }

    companion object {

        const val DEFAULT_WEIGHT_KG = 70.0

        private const val DAY_MS = 24L * 60 * 60 * 1000

        const val ACTIVITY_WINDOW_DAYS = 14

        const val FIRST_IMPORT_WINDOW_MS = 30L * 24 * 60 * 60 * 1000

        const val OWN_WINDOW_SLACK_MS = 24L * 60 * 60 * 1000
    }
}
