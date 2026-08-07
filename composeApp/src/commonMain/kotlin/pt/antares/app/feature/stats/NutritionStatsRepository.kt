package pt.antares.app.feature.stats

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pt.antares.app.core.database.DbInfoDao
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.nutrition.EfsaReference
import pt.antares.app.feature.fooddata.FoodSeeder
import pt.antares.app.core.nutrition.MicroTotals
import pt.antares.app.generated.resources.Res

class NutritionStatsRepository(
    private val foodLogDao: FoodLogDao,
    private val dbInfoDao: DbInfoDao,
    private val io: CoroutineDispatcher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private var cached: EfsaReference? = null

    suspend fun totals(fromDay: Long, toDay: Long): MicroTotals = withContext(io) {
        val totals = HashMap<String, Double>()
        val measuredKcal = HashMap<String, Double>()
        var totalKcal = 0.0
        var measuredAnyKcal = 0.0
        for (log in foodLogDao.logsInRange(fromDay, toDay)) {
            totalKcal += log.kcalSnapshot
            val per100 = log.microsPer100Json?.let {
                runCatching { json.decodeFromString<Map<String, Double>>(it) }.getOrNull()
            } ?: continue

            if (MicroTotals.hasRealMicros(per100.keys)) measuredAnyKcal += log.kcalSnapshot
            val factor = log.quantityGrams / 100.0
            for ((key, value) in per100) {
                totals[key] = (totals[key] ?: 0.0) + value * factor
                measuredKcal[key] = (measuredKcal[key] ?: 0.0) + log.kcalSnapshot
            }
        }
        MicroTotals(
            byKey = totals,
            measuredKcalByKey = measuredKcal,
            totalKcal = totalKcal,
            measuredAnyKcal = measuredAnyKcal,
        )
    }

    suspend fun catalogueRebuiltDay(): Long? = withContext(io) {
        dbInfoDao.get(FoodSeeder.KEY_REBUILT_DAY)?.value?.toLongOrNull()
    }

    suspend fun loadReference(): EfsaReference = cached ?: withContext(io) {
        @OptIn(ExperimentalResourceApi::class)
        val bytes = Res.readBytes("files/seed_efsa_drv.csv")
        EfsaReference.parse(bytes.decodeToString()).also { cached = it }
    }
}
