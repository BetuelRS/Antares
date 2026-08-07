package pt.antares.app.feature.fasting.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.FastingProtocolDao
import pt.antares.app.core.database.entities.FastingProtocolEntity

class FastingProtocolSeeder(
    private val dao: FastingProtocolDao,
    private val io: CoroutineDispatcher,
) {
    suspend fun seedIfNeeded() = withContext(io) {
        if (dao.count() > 0) return@withContext
        val now = Clock.System.now().toEpochMilliseconds()
        dao.upsertAll(
            listOf(
                protocol("fp_16_8", "16:8", 16, now),
                protocol("fp_18_6", "18:6", 18, now),
                protocol("fp_20_4", "20:4", 20, now),
                protocol("fp_omad", "OMAD", 23, now),
            ),
        )
    }

    private fun protocol(id: String, name: String, hours: Int, now: Long) =
        FastingProtocolEntity(id = id, name = name, fastingHours = hours, isCustom = false, updatedAt = now)
}
