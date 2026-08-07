package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.daos.RoutineDao
import pt.antares.app.core.database.daos.RoutineScheduleDao
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.util.Ids

data class RoutineItemView(
    val item: RoutineItemEntity,
    val exerciseName: String,
)

data class RoutineWithItems(
    val routine: RoutineEntity,
    val items: List<RoutineItemView>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineRepository(
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseLibraryDao,
    private val scheduleDao: RoutineScheduleDao,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()

    fun observeRoutines(): Flow<List<RoutineEntity>> = routineDao.observeRoutines()

    fun observeDetail(routineId: String): Flow<RoutineWithItems?> =
        combine(
            routineDao.observeRoutine(routineId),
            routineDao.observeItems(routineId),
        ) { routine, items -> routine to items }
            .mapLatest { (routine, items) ->
                if (routine == null) return@mapLatest null

                val names = exerciseDao.namesByIds(items.map { it.exerciseId }.distinct())
                    .associate { it.id to it.namePt.ifBlank { it.nameEn } }
                RoutineWithItems(
                    routine = routine,
                    items = items.map { RoutineItemView(it, names[it.exerciseId] ?: it.exerciseId) },
                )
            }

    suspend fun createRoutine(name: String): String = withContext(io) {
        val id = Ids.newUuid()
        val position = routineDao.countRoutines()
        routineDao.upsertRoutine(
            RoutineEntity(id = id, name = name, note = null, position = position, updatedAt = now()),
        )
        id
    }

    suspend fun rename(routineId: String, name: String) = withContext(io) {
        val r = routineDao.routineById(routineId) ?: return@withContext
        routineDao.upsertRoutine(r.copy(name = name, updatedAt = now(), dirty = true))
    }

    suspend fun deleteRoutine(routineId: String) = withContext(io) {
        routineDao.softDeleteRoutine(routineId, now())

        scheduleDao.clearByRoutine(routineId, now())
    }

    fun observeSchedule(): Flow<List<pt.antares.app.core.database.entities.RoutineScheduleEntity>> =
        scheduleDao.observeAll()

    suspend fun routineDetailOnce(routineId: String): RoutineWithItems? = withContext(io) {
        val routine = routineDao.routineById(routineId) ?: return@withContext null
        val items = routineDao.itemsOf(routineId)
        val names = exerciseDao.namesByIds(items.map { it.exerciseId }.distinct())
            .associate { it.id to it.namePt.ifBlank { it.nameEn } }
        RoutineWithItems(routine, items.map { RoutineItemView(it, names[it.exerciseId] ?: it.exerciseId) })
    }

    suspend fun setScheduleDay(dayOfWeek: Int, routineId: String) = withContext(io) {
        scheduleDao.upsert(
            pt.antares.app.core.database.entities.RoutineScheduleEntity(
                dayOfWeek = dayOfWeek,
                routineId = routineId,
                updatedAt = now(),
            ),
        )
    }

    suspend fun clearScheduleDay(dayOfWeek: Int) = withContext(io) {
        scheduleDao.clearDay(dayOfWeek, now())
    }

    suspend fun addItem(routineId: String, exerciseId: String) = withContext(io) {
        val position = routineDao.itemsOf(routineId).size
        routineDao.upsertItem(
            RoutineItemEntity(
                id = Ids.newUuid(),
                routineId = routineId,
                exerciseId = exerciseId,
                targetSets = 3,
                targetRepsMin = 8,
                targetRepsMax = 12,
                targetWeightKg = null,
                restSec = 90,
                position = position,
                supersetGroup = null,
                updatedAt = now(),
            ),
        )
    }

    suspend fun updateTargets(
        itemId: String,
        sets: Int,
        repsMin: Int,
        repsMax: Int,
        weightKg: Double?,
        restSec: Int,
    ) = withContext(io) {
        val it = routineDao.itemById(itemId) ?: return@withContext
        routineDao.upsertItem(
            it.copy(
                targetSets = sets,
                targetRepsMin = repsMin,
                targetRepsMax = repsMax,
                targetWeightKg = weightKg,
                restSec = restSec,
                updatedAt = now(),
                dirty = true,
            ),
        )
    }

    suspend fun setSuperset(itemId: String, group: Int?) = withContext(io) {
        val it = routineDao.itemById(itemId) ?: return@withContext
        routineDao.upsertItem(it.copy(supersetGroup = group, updatedAt = now(), dirty = true))
    }

    suspend fun deleteItem(itemId: String) = withContext(io) {
        routineDao.softDeleteItem(itemId, now())
    }

    suspend fun move(routineId: String, itemId: String, up: Boolean) = withContext(io) {
        val items = routineDao.itemsOf(routineId).sortedBy { it.position }
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx < 0) return@withContext
        val swapIdx = if (up) idx - 1 else idx + 1
        if (swapIdx !in items.indices) return@withContext
        val a = items[idx]
        val b = items[swapIdx]
        val t = now()

        routineDao.upsertItems(
            listOf(
                a.copy(position = b.position, updatedAt = t, dirty = true),
                b.copy(position = a.position, updatedAt = t, dirty = true),
            ),
        )
    }
}
