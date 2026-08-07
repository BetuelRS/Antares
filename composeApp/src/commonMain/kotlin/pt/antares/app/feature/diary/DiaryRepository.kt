package pt.antares.app.feature.diary

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import pt.antares.app.core.calc.UsualPortion
import pt.antares.app.core.database.daos.DayKcal
import pt.antares.app.core.database.daos.DayTotals
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.database.daos.WaterLogDao
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.database.entities.WaterLogEntity
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.Ids
import kotlin.math.roundToInt

const val DEFAULT_PORTION_G = 100.0

data class RepeatableMeal(
    val slot: MealSlot,
    val fromEpochDay: Long,
    val names: List<String>,
    val kcal: Int,
)

class DiaryRepository(
    private val foodLogDao: FoodLogDao,
    private val waterDao: WaterLogDao,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()
    private val json = Json { ignoreUnknownKeys = true }

    private fun snapshotMicros(food: FoodEntity): String? {
        val micros = buildMap<String, Double> {
            food.microsJson?.let { runCatching { putAll(json.decodeFromString<Map<String, Double>>(it)) } }
            food.fiberG?.let { put("fiber_g", it) }
            food.sugarsG?.let { put("sugars_g", it) }
            food.satFatG?.let { put("satFat_g", it) }
            food.sodiumMg?.let { put("sodium_mg", it.toDouble()) }
        }
        return if (micros.isEmpty()) null else json.encodeToString(micros)
    }

    fun observeDay(epochDay: Long): Flow<List<FoodLogEntity>> = foodLogDao.observeDay(epochDay)

    fun observeDayTotals(epochDay: Long): Flow<DayTotals> = foodLogDao.observeDayTotals(epochDay)

    fun observeLoggedDaysSince(fromEpochDay: Long): Flow<List<Long>> =
        foodLogDao.observeLoggedDaysSince(fromEpochDay)

    fun observeWater(epochDay: Long): Flow<WaterLogEntity?> = waterDao.observeDay(epochDay)

    fun observeDailyKcal(from: Long, to: Long): Flow<List<DayKcal>> =
        foodLogDao.observeDailyKcal(from, to)

    suspend fun logFood(
        food: FoodEntity,
        quantityGrams: Double,
        slot: MealSlot,
        epochDay: Long,
        origin: LogOrigin = LogOrigin.MANUAL,
    ) = withContext(io) {
        val factor = quantityGrams / 100.0
        foodLogDao.upsert(
            FoodLogEntity(
                id = Ids.newUuid(),
                epochDay = epochDay,
                mealSlot = slot,
                foodId = food.id,
                nameSnapshot = food.namePt.ifBlank { food.nameEn },
                quantityGrams = quantityGrams,
                kcalSnapshot = (food.kcal * factor).roundToInt(),
                proteinSnapshot = food.proteinG * factor,
                carbsSnapshot = food.carbsG * factor,
                fatSnapshot = food.fatG * factor,
                microsPer100Json = snapshotMicros(food),
                origin = origin,
                isLiquid = food.isLiquid,
                updatedAt = now(),
            ),
        )
    }

    suspend fun logQuickCalories(
        kcal: Int,
        name: String,
        slot: MealSlot,
        epochDay: Long,
    ) = withContext(io) {
        foodLogDao.upsert(
            FoodLogEntity(
                id = Ids.newUuid(),
                epochDay = epochDay,
                mealSlot = slot,
                foodId = null,
                nameSnapshot = name,

                quantityGrams = 100.0,
                kcalSnapshot = kcal,
                proteinSnapshot = 0.0,
                carbsSnapshot = 0.0,
                fatSnapshot = 0.0,
                microsPer100Json = null,
                origin = LogOrigin.MANUAL,
                updatedAt = now(),
            ),
        )
    }

    suspend fun updateQuantity(logId: String, newGrams: Double) = withContext(io) {
        val log = foodLogDao.byId(logId) ?: return@withContext
        if (log.quantityGrams <= 0) return@withContext
        val factor = newGrams / log.quantityGrams
        foodLogDao.upsert(
            log.copy(
                quantityGrams = newGrams,
                kcalSnapshot = (log.kcalSnapshot * factor).roundToInt(),
                proteinSnapshot = log.proteinSnapshot * factor,
                carbsSnapshot = log.carbsSnapshot * factor,
                fatSnapshot = log.fatSnapshot * factor,
                updatedAt = now(),
                dirty = true,
            ),
        )
    }

    suspend fun move(logId: String, newSlot: MealSlot) = withContext(io) {
        val log = foodLogDao.byId(logId) ?: return@withContext
        foodLogDao.upsert(log.copy(mealSlot = newSlot, updatedAt = now(), dirty = true))
    }

    suspend fun duplicate(logId: String) = withContext(io) {
        val log = foodLogDao.byId(logId) ?: return@withContext
        foodLogDao.upsert(log.copy(id = Ids.newUuid(), updatedAt = now(), dirty = true))
    }

    suspend fun delete(logId: String) = withContext(io) {
        foodLogDao.softDelete(logId, now())
    }

    suspend fun copyDay(fromEpochDay: Long, toEpochDay: Long) = withContext(io) {
        foodLogDao.dayLogs(fromEpochDay).forEach { log ->
            foodLogDao.upsert(
                log.copy(id = Ids.newUuid(), epochDay = toEpochDay, updatedAt = now(), dirty = true),
            )
        }
    }

    suspend fun copyMeal(fromEpochDay: Long, toEpochDay: Long, slot: MealSlot) = withContext(io) {
        foodLogDao.mealLogs(fromEpochDay, slot).forEach { log ->
            foodLogDao.upsert(
                log.copy(id = Ids.newUuid(), epochDay = toEpochDay, updatedAt = now(), dirty = true),
            )
        }
    }

    suspend fun usualPortionOf(foodId: String): Double? = withContext(io) {
        UsualPortion.of(foodLogDao.recentAmounts(foodId))
    }

    suspend fun defaultPortionFor(food: FoodEntity): Double =
        usualPortionOf(food.id) ?: food.lastAmountG ?: food.servingGrams ?: DEFAULT_PORTION_G

    suspend fun moveMeal(epochDay: Long, from: MealSlot, to: MealSlot): Int = withContext(io) {
        if (from == to) return@withContext 0
        val logs = foodLogDao.mealLogs(epochDay, from)
        logs.forEach { foodLogDao.upsert(it.copy(mealSlot = to, updatedAt = now(), dirty = true)) }
        logs.size
    }

    suspend fun clearMeal(epochDay: Long, slot: MealSlot): Int = withContext(io) {
        val logs = foodLogDao.mealLogs(epochDay, slot)
        val ts = now()
        logs.forEach { foodLogDao.softDelete(it.id, ts) }
        logs.size
    }

    suspend fun lastMealBefore(slot: MealSlot, day: Long): RepeatableMeal? = withContext(io) {
        val from = foodLogDao.lastDayWithMeal(slot, day) ?: return@withContext null
        val logs = foodLogDao.mealLogs(from, slot)
        if (logs.isEmpty()) return@withContext null
        RepeatableMeal(
            slot = slot,
            fromEpochDay = from,
            names = logs.map { it.nameSnapshot },
            kcal = logs.sumOf { it.kcalSnapshot },
        )
    }

    suspend fun recentMeals(slot: MealSlot, beforeDay: Long, limit: Int = 10): List<RepeatableMeal> =
        withContext(io) {
            foodLogDao.recentDaysWithMeal(slot, beforeDay, limit).mapNotNull { dia ->
                val logs = foodLogDao.mealLogs(dia, slot)
                if (logs.isEmpty()) {
                    null
                } else {
                    RepeatableMeal(
                        slot = slot,
                        fromEpochDay = dia,
                        names = logs.map { it.nameSnapshot },
                        kcal = logs.sumOf { it.kcalSnapshot },
                    )
                }
            }
        }

    suspend fun repeatLastMeal(slot: MealSlot, toDay: Long): Boolean = withContext(io) {
        val from = foodLogDao.lastDayWithMeal(slot, toDay) ?: return@withContext false
        copyMeal(from, toDay, slot)
        true
    }

    suspend fun addWater(epochDay: Long, deltaMl: Int) = withContext(io) {

        val existing = waterDao.byDayForWrite(epochDay)

        val base = existing?.takeIf { !it.deleted }?.ml ?: 0
        val newMl = (base + deltaMl).coerceAtLeast(0)
        waterDao.upsert(
            WaterLogEntity(
                id = existing?.id ?: Ids.newUuid(),
                epochDay = epochDay,
                ml = newMl,
                updatedAt = now(),
                dirty = true,
            ),
        )
    }
}
