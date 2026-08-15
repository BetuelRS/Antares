package pt.antares.app.feature.stats

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pt.antares.app.core.database.DbInfoDao
import pt.antares.app.core.database.daos.FoodLogDao
import pt.antares.app.core.nutrition.EfsaReference
import pt.antares.app.feature.fooddata.FoodSeeder
import pt.antares.app.core.nutrition.MicroTotals
import pt.antares.app.generated.resources.Res
import pt.antares.app.core.nutrition.microsDeJson

class NutritionStatsRepository(
    private val foodLogDao: FoodLogDao,
    private val dbInfoDao: DbInfoDao,
    private val io: CoroutineDispatcher,
) {

    private var cached: EfsaReference? = null

    /**
     * Soma os micronutrientes de um período, e ao mesmo tempo mede sobre que parte da
     * comida cada soma fala. É esse segundo número que impede o ecrã de anunciar falta de
     * ferro quando o que falta é análise — ver [MicroTotals].
     */
    suspend fun totals(fromDay: Long, toDay: Long): MicroTotals = withContext(io) {
        val totals = HashMap<String, Double>()
        val measuredKcal = HashMap<String, Double>()
        var totalKcal = 0.0
        var measuredAnyKcal = 0.0
        for (log in foodLogDao.logsInRange(fromDay, toDay)) {
            // As calorias contam antes do `continue`: um registo sem análise entra no
            // denominador, e é isso que faz a cobertura descer quando ele existe.
            totalKcal += log.kcalSnapshot
            val per100 = microsDeJson(log.microsPer100Json)
            if (per100.isEmpty()) continue

            // Só conta como analisado se trouxer mais do que os quatro valores de rótulo,
            // que qualquer embalagem tem e não provam análise nenhuma.
            if (MicroTotals.hasRealMicros(per100.keys)) measuredAnyKcal += log.kcalSnapshot
            // Os micronutrientes estão guardados por 100 g, ao contrário dos macros.
            val factor = log.quantityGrams / 100.0
            for ((key, value) in per100) {
                totals[key] = (totals[key] ?: 0.0) + value * factor
                // Por nutriente e não só no total: um alimento pode declarar ferro e não
                // declarar cálcio, e a cobertura de cada um é diferente.
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
