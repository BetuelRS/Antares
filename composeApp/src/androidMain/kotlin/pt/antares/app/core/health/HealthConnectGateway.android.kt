package pt.antares.app.core.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import pt.antares.app.core.nutrition.Nutrients
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt

/**
 * A implementação Android do [HealthGateway]. Toda a API do Health Connect fica presa
 * neste ficheiro: o resto da app fala pela interface e nem sabe que ele existe.
 */
class HealthConnectGateway(private val context: Context) : HealthGateway {

    // O próprio pacote serve para reconhecer o que foi esta app a escrever, e não voltar a
    // importar como se fosse de fora.
    private val selfPackage: String = context.packageName

    // `lazy` porque criar o cliente com o serviço ausente lança: só se tenta depois de a
    // disponibilidade estar confirmada, e o resultado nulo é um estado válido.
    private val client: HealthConnectClient? by lazy {
        if (availability() == HealthAvailability.AVAILABLE) {
            runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        } else {
            null
        }
    }

    override fun availability(): HealthAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthAvailability.PROVIDER_UPDATE_REQUIRED
            else -> HealthAvailability.NOT_SUPPORTED
        }

    override val readPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),

        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),

        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    )

    override val writePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class),

        HealthPermission.getWritePermission(BodyFatRecord::class),
        HealthPermission.getWritePermission(LeanBodyMassRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
    )

    override suspend fun hasReadPermissions(): Boolean = hasAll(readPermissions)

    override suspend fun hasWritePermissions(): Boolean = hasAll(writePermissions)

    /**
     * Exige o conjunto todo. É mais restritivo do que o necessário — com só a permissão de
     * peso ainda se poderia importar peso —, mas evita importações meias em que a pessoa
     * não percebe porque é que os treinos não aparecem.
     */
    private suspend fun hasAll(required: Set<String>): Boolean {
        val c = client ?: return false
        val granted = runCatching { c.permissionController.getGrantedPermissions() }
            .getOrDefault(emptySet())
        return granted.containsAll(required)
    }

    override suspend fun steps(startMs: Long, endMs: Long): Long? {
        val c = client ?: return null
        val result: AggregationResult = runCatching {
            c.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = range(startMs, endMs),
                ),
            )
        }.getOrNull() ?: return null
        return result[StepsRecord.COUNT_TOTAL]
    }

    override suspend fun weights(sinceMs: Long): List<HealthWeight> {
        val c = client ?: return emptyList()
        val records = runCatching {
            c.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.ofEpochMilli(sinceMs)),
                ),
            ).records
        }.getOrDefault(emptyList())

        // Filtrar o próprio pacote é a primeira defesa contra o ciclo: a app publica o que
        // sabe no Health Connect, e sem isto reimportava o que ela mesma escreveu. O
        // [HealthDedupe] trata do resto, que vem de outras apps.
        return records
            .filterNot { it.metadata.dataOrigin.packageName == selfPackage }
            .map {
                HealthWeight(
                    uid = it.metadata.id,
                    timestampMs = it.time.toEpochMilli(),
                    kg = it.weight.inKilograms,
                )
            }
    }

    override suspend fun bodyComposition(sinceMs: Long): List<HealthBodyComposition> {
        val c = client ?: return emptyList()
        val since = Instant.ofEpochMilli(sinceMs)

        val fat = runCatching {
            c.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(since),
                ),
            ).records
        }.getOrDefault(emptyList())
            .filterNot { it.metadata.dataOrigin.packageName == selfPackage }

        val lean = runCatching {
            c.readRecords(
                ReadRecordsRequest(
                    recordType = LeanBodyMassRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(since),
                ),
            ).records
        }.getOrDefault(emptyList())
            .filterNot { it.metadata.dataOrigin.packageName == selfPackage }

        val leanByInstant = lean.associateBy { it.time.toEpochMilli() }

        val fromFat = fat.map {
            val ms = it.time.toEpochMilli()
            HealthBodyComposition(
                uid = it.metadata.id,
                timestampMs = ms,
                bodyFatPct = it.percentage.value,
                leanMassKg = leanByInstant[ms]?.mass?.inKilograms,
            )
        }
        val fatInstants = fromFat.map { it.timestampMs }.toSet()
        val leanOnly = lean.filterNot { it.time.toEpochMilli() in fatInstants }.map {
            HealthBodyComposition(
                uid = it.metadata.id,
                timestampMs = it.time.toEpochMilli(),
                leanMassKg = it.mass.inKilograms,
            )
        }
        return (fromFat + leanOnly).sortedBy { it.timestampMs }
    }

    override suspend fun sessions(sinceMs: Long): List<HealthSession> {
        val c = client ?: return emptyList()
        val records = runCatching {
            c.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.ofEpochMilli(sinceMs)),
                ),
            ).records
        }.getOrDefault(emptyList())

        return records
            .filterNot { it.metadata.dataOrigin.packageName == selfPackage }
            .map { r ->
                HealthSession(
                    uid = r.metadata.id,
                    title = r.title,
                    activity = activityLabel(r.exerciseType),
                    startMs = r.startTime.toEpochMilli(),
                    endMs = r.endTime.toEpochMilli(),
                    kcal = energyKcal(c, r.startTime, r.endTime),
                    met = metOf(r.exerciseType),
                )
            }
    }

    private suspend fun energyKcal(c: HealthConnectClient, start: Instant, end: Instant): Int? {
        val result = runCatching {
            c.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                ),
            )
        }.getOrNull() ?: return null
        val kcal = result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
        return kcal?.takeIf { it > 0 }?.roundToInt()
    }

    private fun metOf(type: Int): Double = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
        -> 8.0

        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> 3.5
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> 5.3

        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
        -> 7.0

        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        -> 5.0

        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
        -> 6.0

        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> 8.0
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> 2.5
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> 3.0
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> 5.0

        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
        -> 6.0

        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN,
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER,
        -> 7.0

        ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> 6.5
        ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> 7.3
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> 5.0
        else -> 4.0
    }

    override suspend fun writeNutrition(
        epochDay: Long,
        kcal: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        micros: Map<String, Double>,
    ) {
        val c = client ?: return
        if (kcal <= 0) return

        val zone = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
        val start = java.time.LocalDate.ofEpochDay(epochDay).atStartOfDay(zone.normalized()).toInstant()
        val end = start.plusSeconds(24L * 60 * 60)

        fun g(key: String): Mass? = micros[key]?.takeIf { it > 0 }?.let {
            when {
                key.endsWith("_ug") -> Mass.micrograms(it)
                key.endsWith("_mg") -> Mass.milligrams(it)
                else -> Mass.grams(it)
            }
        }

        val record = NutritionRecord(
            energy = Energy.kilocalories(kcal.toDouble()),
            protein = Mass.grams(proteinG),
            totalCarbohydrate = Mass.grams(carbsG),
            totalFat = Mass.grams(fatG),

            dietaryFiber = g(Nutrients.FIBER),
            sugar = g(Nutrients.SUGARS),
            saturatedFat = g(Nutrients.SAT_FAT),
            sodium = g(Nutrients.SODIUM),

            vitaminA = g(Nutrients.VIT_A),
            thiamin = g(Nutrients.VIT_B1),
            riboflavin = g(Nutrients.VIT_B2),
            niacin = g(Nutrients.VIT_B3),
            pantothenicAcid = g(Nutrients.VIT_B5),
            vitaminB6 = g(Nutrients.VIT_B6),
            folate = g(Nutrients.VIT_B9),
            vitaminB12 = g(Nutrients.VIT_B12),
            vitaminC = g(Nutrients.VIT_C),
            vitaminD = g(Nutrients.VIT_D),
            vitaminE = g(Nutrients.VIT_E),
            vitaminK = g(Nutrients.VIT_K),

            calcium = g(Nutrients.CALCIUM),
            iron = g(Nutrients.IRON),
            magnesium = g(Nutrients.MAGNESIUM),
            zinc = g(Nutrients.ZINC),
            potassium = g(Nutrients.POTASSIUM),
            copper = g(Nutrients.COPPER),
            selenium = g(Nutrients.SELENIUM),
            phosphorus = g(Nutrients.PHOSPHORUS),
            manganese = g(Nutrients.MANGANESE),
            iodine = g(Nutrients.IODINE),

            cholesterol = g(Nutrients.CHOLESTEROL),
            monounsaturatedFat = g(Nutrients.FAT_MONO),
            polyunsaturatedFat = g(Nutrients.FAT_POLY),
            startTime = start,
            startZoneOffset = zone,
            endTime = end,
            endZoneOffset = zone,

            metadata = manualMetadata("antares-nutrition-$epochDay"),
        )
        runCatching { c.insertRecords(listOf(record)) }
    }

    override suspend fun writeBodyComposition(
        epochDay: Long,
        bodyFatPct: Double?,
        leanMassKg: Double?,
    ): Boolean {
        if (!hasAll(writePermissions)) return false
        val c = client ?: return false
        return c.writeBodyCompositionRecords(epochDay, bodyFatPct, leanMassKg)
    }

    override suspend fun writeSession(session: OutboundSession): Boolean {
        val c = client ?: return false
        val start = Instant.ofEpochMilli(session.startMs)
        val end = Instant.ofEpochMilli(session.endMs)
        val zone = ZoneOffset.systemDefault().rules.getOffset(start)

        val exerciseType = when (session.kind) {
            OutboundKind.RUN -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            OutboundKind.WORKOUT -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
        }

        val exercise = ExerciseSessionRecord(
            exerciseType = exerciseType,
            title = session.title,
            startTime = start,
            startZoneOffset = zone,
            endTime = end,
            endZoneOffset = zone,
            metadata = manualMetadata("antares-session-${session.clientId}"),
        )
        val records = buildList {
            add(exercise)
            if (session.kcal > 0) {
                add(
                    ActiveCaloriesBurnedRecord(
                        energy = Energy.kilocalories(session.kcal.toDouble()),
                        startTime = start,
                        startZoneOffset = zone,
                        endTime = end,
                        endZoneOffset = zone,
                        metadata = manualMetadata("antares-energy-${session.clientId}"),
                    ),
                )
            }
        }
        return runCatching { c.insertRecords(records) }.isSuccess
    }

    private fun manualMetadata(clientRecordId: String): Metadata =
        Metadata.manualEntry(clientRecordId = clientRecordId)

    private fun range(startMs: Long, endMs: Long): TimeRangeFilter =
        TimeRangeFilter.between(Instant.ofEpochMilli(startMs), Instant.ofEpochMilli(endMs))

    private fun activityLabel(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
        -> "Corrida"

        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Caminhada"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Caminhada na natureza"

        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
        -> "Ciclismo"

        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        -> "Musculação"

        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
        -> "Natação"

        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Ioga"
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "Pilates"
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Elíptica"
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
        -> "Remo"

        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN,
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER,
        -> "Futebol"

        ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "Basquetebol"
        ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "Ténis"
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "Dança"
        else -> "Exercício"
    }
}
