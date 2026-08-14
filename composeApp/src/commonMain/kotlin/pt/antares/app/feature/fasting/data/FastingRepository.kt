package pt.antares.app.feature.fasting.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.antares.app.core.calc.FinishedFast
import pt.antares.app.core.database.daos.FastingProtocolDao
import pt.antares.app.core.database.daos.FastingSessionDao
import pt.antares.app.core.database.entities.FastingProtocolEntity
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.util.Ids
import pt.antares.app.feature.fasting.FastingNotifier
import pt.antares.app.feature.fasting.domain.FastingMachine
import pt.antares.app.feature.fasting.domain.FastingResult
import pt.antares.app.feature.fasting.domain.FastingSnapshot

/**
 * Jejum. As transições de estado não estão aqui: vivem na [FastingMachine], que é pura e
 * testável. Este repositório só grava o resultado e trata dos alarmes — a máquina não sabe
 * de base de dados nem de notificações.
 */
class FastingRepository(
    private val protocolDao: FastingProtocolDao,
    private val sessionDao: FastingSessionDao,
    private val notifier: FastingNotifier,
    private val io: CoroutineDispatcher,
) {
    private fun now() = Clock.System.now().toEpochMilliseconds()

    fun observeProtocols(): Flow<List<FastingProtocolEntity>> = protocolDao.observeAll()

    fun observeActive(): Flow<FastingSessionEntity?> = sessionDao.observeActive()

    fun observeHistory(): Flow<List<FastingSessionEntity>> = sessionDao.observeHistory()

    suspend fun activeSession(): FastingSessionEntity? = withContext(io) { sessionDao.activeSession() }

    fun observeFinished(): Flow<List<FastingSessionEntity>> = sessionDao.observeFinished()

    // Como nos treinos: havendo um jejum a decorrer, devolve-se esse. Dois jejuns abertos
    // ao mesmo tempo não fazem sentido, e a base por si não os impede.
    suspend fun startOrResume(protocolId: String): String = withContext(io) {
        sessionDao.activeSession()?.let { return@withContext it.id }
        val protocol = protocolDao.byId(protocolId) ?: error("protocolo inexistente: $protocolId")
        val snap = FastingMachine.start(Ids.newUuid(), protocolId, protocol.fastingHours, now())
        sessionDao.upsert(snap.toEntity(updatedAt = now()))
        notifier.scheduleGoal(snap.id, snap.targetEndAt)
        snap.id
    }

    suspend fun adjustStart(newStartedAt: Long): Boolean = withContext(io) {
        val current = sessionDao.activeSession() ?: return@withContext false
        when (val r = FastingMachine.adjustStart(current.toSnapshot(), newStartedAt, now())) {
            is FastingResult.Ok -> {
                sessionDao.upsert(r.value.toEntity(updatedAt = now()))

                // Corrigir a hora de início muda a hora-alvo, por isso o alarme antigo tem
                // de morrer antes de se marcar o novo — senão ficavam dois avisos.
                notifier.cancel(r.value.id)
                notifier.scheduleGoal(r.value.id, r.value.targetEndAt)
                true
            }
            is FastingResult.Err -> false
        }
    }

    suspend fun finish(): Boolean = withContext(io) { end(breakEarly = false) }

    suspend fun breakFast(): Boolean = withContext(io) { end(breakEarly = true) }

    private suspend fun end(breakEarly: Boolean): Boolean {
        val current = sessionDao.activeSession() ?: return false
        val snap = current.toSnapshot()
        val r = if (breakEarly) FastingMachine.breakFast(snap, now()) else FastingMachine.finish(snap, now())
        return when (r) {
            is FastingResult.Ok -> {
                sessionDao.upsert(r.value.toEntity(updatedAt = now()))
                notifier.cancel(r.value.id)
                true
            }
            is FastingResult.Err -> false
        }
    }
}

fun FastingSessionEntity.toSnapshot() = FastingSnapshot(
    id = id, protocolId = protocolId, startedAt = startedAt,
    targetEndAt = targetEndAt, endedAt = endedAt, status = status,
)

fun FastingSnapshot.toEntity(updatedAt: Long) = FastingSessionEntity(
    id = id, protocolId = protocolId, startedAt = startedAt,
    targetEndAt = targetEndAt, endedAt = endedAt, status = status,
    updatedAt = updatedAt, deleted = false, dirty = true,
)

fun FastingSessionEntity.toFinishedFast(): FinishedFast = FinishedFast(
    startedAt = startedAt,
    // Duração zero para uma sessão sem fim. Só devia acontecer com um jejum a decorrer,
    // que as estatísticas já filtram antes de chegar aqui.
    endedAt = endedAt ?: startedAt,
    completed = status == pt.antares.app.core.model.FastingStatus.COMPLETED,
)
