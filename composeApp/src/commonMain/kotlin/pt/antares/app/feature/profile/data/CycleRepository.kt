package pt.antares.app.feature.profile.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.CycleDao
import pt.antares.app.core.database.entities.CycleEntity
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.todayEpochDay

class CycleRepository(
    private val dao: CycleDao,
    private val io: CoroutineDispatcher,
) {

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    fun observeAll(): Flow<List<CycleEntity>> = dao.observeAll()

    suspend fun all(): List<CycleEntity> = withContext(io) { dao.all() }

    suspend fun recordStart(epochDay: Long = todayEpochDay(), note: String? = null) = withContext(io) {
        val existente = dao.byStart(epochDay)
        if (existente != null) return@withContext
        dao.upsert(
            CycleEntity(
                id = Ids.newUuid(),
                startEpochDay = epochDay,
                note = note,
                createdAt = now(),
            ),
        )
    }

    suspend fun recordEnd(epochDay: Long = todayEpochDay()) = withContext(io) {
        val ultimo = dao.latest() ?: return@withContext
        if (epochDay < ultimo.startEpochDay) return@withContext
        dao.upsert(ultimo.copy(endEpochDay = epochDay))
    }

    /**
     * Corrige as datas de um período já registado. Quem se enganou a marcar o início não
     * tinha como o arranjar: só apagando e voltando a marcar, e a marcação era sempre hoje.
     */
    suspend fun updateDates(id: String, startEpochDay: Long, endEpochDay: Long?) = withContext(io) {
        val existente = dao.all().firstOrNull { it.id == id } ?: return@withContext
        dao.upsert(existente.copy(startEpochDay = startEpochDay, endEpochDay = endEpochDay))
    }

    suspend fun delete(id: String) = withContext(io) { dao.delete(id) }
}
