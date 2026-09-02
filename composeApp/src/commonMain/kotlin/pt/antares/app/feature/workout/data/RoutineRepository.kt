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

    /**
     * A rotina com os nomes dos exercícios já resolvidos. Os nomes vêm do catálogo a cada
     * emissão em vez de ficarem guardados na rotina: uma tradução corrigida aparece sem
     * ser preciso reescrever nada.
     */
    fun observeDetail(routineId: String): Flow<RoutineWithItems?> =
        combine(
            routineDao.observeRoutine(routineId),
            routineDao.observeItems(routineId),
        ) { routine, items -> routine to items }
            .mapLatest { (routine, items) ->
                // Null quando a rotina foi apagada. O fluxo do DAO emite a linha marcada
                // de propósito, para o ecrã se poder fechar em vez de ficar suspenso.
                if (routine == null) return@mapLatest null

                val names = exerciseDao.namesByIds(items.map { it.exerciseId }.distinct())
                    .associate { it.id to it.namePt.ifBlank { it.nameEn } }
                RoutineWithItems(
                    routine = routine,
                    // Sem nome no catálogo fica o identificador: feio, mas visível — a
                    // linha desaparecer escondia que o exercício ainda faz parte do plano.
                    items = items.map { RoutineItemView(it, names[it.exerciseId] ?: it.exerciseId) },
                )
            }

    suspend fun createRoutine(name: String): String = withContext(io) {
        val id = Ids.newUuid()
        // A posição é a contagem atual, o que põe a rotina nova no fim da lista.
        val position = routineDao.countRoutines()
        routineDao.upsertRoutine(
            RoutineEntity(id = id, name = name, note = null, position = position, updatedAt = now()),
        )
        id
    }

    /**
     * Copia a rotina inteira, com os exercícios, os alvos e os grupos de supersérie.
     *
     * **Não copia o calendário.** «Empurrar A (cópia)» não passa a ocupar os mesmos dias da
     * original: duplicar é para partir de uma base, e não para ficar com duas rotinas a
     * disputar a terça-feira. Quem a quiser no horário põe-na lá.
     */
    suspend fun duplicateRoutine(routineId: String, novoNome: String): String? = withContext(io) {
        val original = routineDao.routineById(routineId) ?: return@withContext null
        val itens = routineDao.itemsOf(routineId).sortedBy { it.position }
        val t = now()
        val novoId = Ids.newUuid()
        routineDao.upsertRoutine(
            original.copy(
                id = novoId,
                name = novoNome,
                position = routineDao.countRoutines(),
                updatedAt = t,
            ),
        )
        routineDao.upsertItems(
            itens.mapIndexed { i, item ->
                item.copy(id = Ids.newUuid(), routineId = novoId, position = i, updatedAt = t)
            },
        )
        novoId
    }

    /**
     * Põe os exercícios pela ordem que a lista traz. É o que o arrastar-para-reordenar grava
     * quando o dedo levanta: uma só escrita, e não uma troca por cada posição percorrida.
     */
    suspend fun reorderItems(ordem: List<String>) = withContext(io) {
        val t = now()
        val porId = ordem.withIndex().associate { (i, id) -> id to i }
        val itens = ordem.mapNotNull { routineDao.itemById(it) }
        routineDao.upsertItems(
            itens.map { it.copy(position = porId[it.id] ?: it.position, updatedAt = t) },
        )
    }

    suspend fun rename(routineId: String, name: String) = withContext(io) {

        val r = routineDao.routineById(routineId) ?: return@withContext
        routineDao.upsertRoutine(r.copy(name = name, updatedAt = now()))
    }

    /**
     * Devolve a rotina e os dias do calendário que foram com ela. A ordem é a inversa de
     * apagar: sem o calendário, desfazer devolvia uma rotina que a semana já não conhece.
     */
    suspend fun restoreRoutine(routineId: String) = withContext(io) {
        val ts = now()
        routineDao.restoreRoutine(routineId, ts)
        scheduleDao.restoreByRoutine(routineId, ts)
    }

    suspend fun deleteRoutine(routineId: String) = withContext(io) {
        routineDao.softDeleteRoutine(routineId, now())

        // Tirar do calendário faz parte de apagar: não há chave estrangeira, e os dias
        // continuariam a apontar a uma rotina que já não existe.
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
            ),
        )
    }

    suspend fun setSuperset(itemId: String, group: Int?) = withContext(io) {
        val it = routineDao.itemById(itemId) ?: return@withContext
        routineDao.upsertItem(it.copy(supersetGroup = group, updatedAt = now()))
    }

    suspend fun deleteItem(itemId: String) = withContext(io) {
        routineDao.softDeleteItem(itemId, now())
    }

    suspend fun restoreItem(itemId: String) = withContext(io) {
        routineDao.restoreItem(itemId, now())
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
                a.copy(position = b.position, updatedAt = t),
                b.copy(position = a.position, updatedAt = t),
            ),
        )
    }
}
