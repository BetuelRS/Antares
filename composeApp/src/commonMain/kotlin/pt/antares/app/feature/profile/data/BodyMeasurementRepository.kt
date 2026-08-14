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

        // Vê as lápides, para reaproveitar a linha do dia — o índice único conta-as.
        val row = dao.byDayForWrite(epochDay)

        // Mas só uma linha viva contribui com valores: reabrir um dia apagado começa
        // do zero em vez de ressuscitar medidas que a pessoa desfez.
        val existing = row?.takeIf { !it.deleted }
        // Funde em vez de substituir: cada `?:` deixa passar o que já lá estava. Registar
        // só a cintura hoje não pode apagar a massa gorda medida no mesmo dia.
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

        // Uma linha sem medida nenhuma não se grava: encheria o histórico de dias vazios.
        if (!merged.isEmpty) dao.upsert(merged)
    }

    suspend fun delete(id: String) = withContext(io) { dao.softDelete(id, now()) }
}
