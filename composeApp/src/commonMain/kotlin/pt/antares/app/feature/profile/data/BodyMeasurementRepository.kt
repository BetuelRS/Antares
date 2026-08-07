package pt.antares.app.feature.profile.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.BodyMeasurementDao
import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.todayEpochDay

class BodyMeasurementRepository(
    private val dao: BodyMeasurementDao,
    private val io: CoroutineDispatcher,
) {

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    fun observeAll(): Flow<List<BodyMeasurementEntity>> = dao.observeAll()

    fun observeLatest(): Flow<BodyMeasurementEntity?> = dao.observeLatest()

    suspend fun latest(): BodyMeasurementEntity? = withContext(io) { dao.latest() }

    suspend fun record(
        epochDay: Long = todayEpochDay(),
        bodyFatPct: Double? = null,
        bodyFatSource: BodyFatSource? = null,
        waistCm: Double? = null,
        neckCm: Double? = null,
        hipCm: Double? = null,
        armCm: Double? = null,
        thighCm: Double? = null,
        chestCm: Double? = null,
    ) = withContext(io) {

        val row = dao.byDayForWrite(epochDay)

        val existing = row?.takeIf { !it.deleted }
        val merged = BodyMeasurementEntity(
            id = row?.id ?: Ids.newUuid(),
            epochDay = epochDay,
            bodyFatPct = bodyFatPct ?: existing?.bodyFatPct,
            bodyFatSource = bodyFatSource ?: existing?.bodyFatSource,
            waistCm = waistCm ?: existing?.waistCm,
            neckCm = neckCm ?: existing?.neckCm,
            hipCm = hipCm ?: existing?.hipCm,
            armCm = armCm ?: existing?.armCm,
            thighCm = thighCm ?: existing?.thighCm,
            chestCm = chestCm ?: existing?.chestCm,
            updatedAt = now(),
            dirty = true,
        )

        if (!merged.isEmpty) dao.upsert(merged)
    }

    suspend fun delete(id: String) = withContext(io) { dao.softDelete(id, now()) }
}
