package pt.antares.app.feature.profile.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import pt.antares.app.core.calc.GoalHistoryCalc
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.calc.Targets
import pt.antares.app.core.coach.CoachRepository
import pt.antares.app.core.database.daos.DailyTargetOverrideDao
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.GoalHistoryDao
import pt.antares.app.core.database.daos.UserProfileDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.database.entities.GoalHistoryEntity
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import kotlinx.datetime.Clock
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.todayEpochDay
import kotlin.math.roundToInt

class ProfileRepository(
    private val profileDao: UserProfileDao,
    private val weightDao: WeightLogDao,
    private val overrideDao: DailyTargetOverrideDao,
    private val foodLogDao: FoodLogDao,
    private val goalDao: GoalHistoryDao,
    private val io: CoroutineDispatcher,
) {

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    fun observeProfile(): Flow<UserProfileEntity?> = profileDao.observe()

    suspend fun saveProfile(profile: UserProfileEntity) = withContext(io) {
        profileDao.upsert(profile.copy(updatedAt = now(), dirty = true))

        recordGoalIfNew(profile.goalWeightKg)
    }

    private suspend fun recordGoalIfNew(newTargetKg: Double?) {
        val anterior = goalDao.latest()?.targetKg
        if (!GoalHistoryCalc.shouldRecord(anterior, newTargetKg)) return
        goalDao.upsert(
            GoalHistoryEntity(
                id = Ids.newUuid(),
                targetKg = newTargetKg!!,
                setOnEpochDay = todayEpochDay(),

                startWeightKg = weightDao.latest()?.weightKg,
                updatedAt = now(),
            ),
        )
    }

    fun observeWeights(): Flow<List<WeightLogEntity>> = weightDao.observeAll()

    fun observeLatestWeight(): Flow<WeightLogEntity?> = weightDao.observeLatest()

    suspend fun upsertWeight(
        epochDay: Long,
        weightKg: Double,
        note: String?,

        averageWithExisting: Boolean = true,
    ) = withContext(io) {

        val existing = weightDao.byDayForWrite(epochDay)

        val vivo = existing?.takeIf { !it.deleted }
        val value = if (averageWithExisting && vivo != null) {
            (vivo.weightKg + weightKg) / 2.0
        } else {
            weightKg
        }
        weightDao.upsert(
            WeightLogEntity(
                id = existing?.id ?: Ids.newUuid(),
                epochDay = epochDay,
                weightKg = value,
                note = note,
                updatedAt = now(),
                dirty = true,
            ),
        )
    }

    suspend fun weightLooksLikeTypo(weightKg: Double): Double? = withContext(io) {
        val reference = weightDao.latest()?.weightKg ?: return@withContext null
        val ratio = weightKg / reference
        if (ratio < TYPO_LOW_RATIO || ratio > TYPO_HIGH_RATIO) reference else null
    }

    suspend fun deleteWeight(id: String) = withContext(io) {
        weightDao.softDelete(id, now())
    }

    fun observeTargets(epochDay: Long = todayEpochDay()): Flow<Targets?> =
        combine(
            profileDao.observe(),
            weightDao.observeLatest(),
            overrideDao.observeByDay(epochDay),
        ) { profile, weight, override ->
            when {
                profile == null -> null
                override != null -> Targets(
                    kcal = override.kcal,
                    proteinG = override.proteinG,
                    carbsG = override.carbsG,
                    fatG = override.fatG,
                )
                else -> NutritionCalc.dailyTargets(
                    profile = profile,
                    weightKg = weight?.weightKg ?: DEFAULT_WEIGHT_KG,
                    todayEpochDay = epochDay,
                )
            }
        }

    suspend fun targetsFor(epochDay: Long): Targets? = withContext(io) {
        val profile = profileDao.get() ?: return@withContext null
        overrideDao.byDay(epochDay)?.let {
            return@withContext Targets(kcal = it.kcal, proteinG = it.proteinG, carbsG = it.carbsG, fatG = it.fatG)
        }
        val weight = weightDao.latest()?.weightKg ?: DEFAULT_WEIGHT_KG
        NutritionCalc.dailyTargets(profile = profile, weightKg = weight, todayEpochDay = epochDay)
    }

    suspend fun learnedTdee(epochDay: Long = todayEpochDay()): Int? = withContext(io) {
        val profile = profileDao.get() ?: return@withContext null
        val override = overrideDao.byDay(epochDay) ?: return@withContext null
        if (override.source != CoachRepository.SOURCE_ADAPTIVE) return@withContext null
        override.kcal - profile.goalRateKcal
    }

    suspend fun loggedDaysPerWeek(weeks: Int, today: Long = todayEpochDay()): Int = withContext(io) {
        if (weeks <= 0) return@withContext 0
        val from = today - weeks * 7L + 1
        val dias = foodLogDao.loggedDaysSince(from).count { it in from..today }
        (dias.toDouble() / weeks).roundToInt()
    }

    suspend fun profileOnce(): UserProfileEntity? = withContext(io) { profileDao.get() }

    suspend fun weightsChronological(): List<Pair<Long, Double>> = withContext(io) {
        weightDao.exportRows().sortedBy { it.epochDay }.map { it.epochDay to it.weightKg }
    }

    suspend fun adaptiveWeeks(): Int = withContext(io) {
        overrideDao.exportRows()
            .filter { it.source == CoachRepository.SOURCE_ADAPTIVE }
            .map { it.epochDay / 7 }
            .distinct()
            .size
    }

    companion object {

        const val DEFAULT_WEIGHT_KG = 70.0

        const val TYPO_LOW_RATIO = 0.75
        const val TYPO_HIGH_RATIO = 1.25
    }
}
