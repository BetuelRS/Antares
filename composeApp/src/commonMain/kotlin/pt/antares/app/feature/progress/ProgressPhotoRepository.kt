package pt.antares.app.feature.progress

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.ProgressPhotoDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.database.entities.ProgressPhotoEntity
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.LocalPhotoStore
import pt.antares.app.core.util.todayEpochDay

class ProgressPhotoRepository(
    private val dao: ProgressPhotoDao,
    private val weightDao: WeightLogDao,
    private val photos: LocalPhotoStore,
    private val io: CoroutineDispatcher,
) {

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    fun observeAll(): Flow<List<ProgressPhotoEntity>> = dao.observeAll()

    suspend fun add(
        base64Jpeg: String,
        epochDay: Long = todayEpochDay(),
        note: String? = null,
    ): Boolean = withContext(io) {
        val id = Ids.newUuid()
        val caminho = photos.save(id, base64Jpeg) ?: return@withContext false
        dao.upsert(
            ProgressPhotoEntity(
                id = id,
                epochDay = epochDay,
                localPath = caminho,
                weightKgSnapshot = weightDao.byDay(epochDay)?.weightKg
                    ?: weightDao.latest()?.weightKg,
                note = note,
                createdAt = now(),
            ),
        )
        true
    }

    suspend fun remove(id: String) = withContext(io) {
        dao.byId(id)?.let { photos.delete(it.localPath) }
        dao.delete(id)
    }

    suspend fun deleteEverything() = withContext(io) {
        photos.deleteAll()
        dao.all().forEach { dao.delete(it.id) }
    }

    suspend fun orphans(): List<String> = withContext(io) {
        dao.all().filterNot { photos.exists(it.localPath) }.map { it.id }
    }
}
