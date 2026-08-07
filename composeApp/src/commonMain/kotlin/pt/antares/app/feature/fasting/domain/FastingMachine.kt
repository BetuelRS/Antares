package pt.antares.app.feature.fasting.domain

import pt.antares.app.core.model.FastingStatus

private const val HOUR_MS = 3_600_000L

data class FastingSnapshot(
    val id: String,
    val protocolId: String,
    val startedAt: Long,
    val targetEndAt: Long,
    val endedAt: Long?,
    val status: FastingStatus,
)

data class FastingProgress(
    val elapsedMs: Long,
    val remainingMs: Long,
    val fraction: Float,
    val reachedGoal: Boolean,
)

sealed interface FastingError {

    data object StartInFuture : FastingError

    data object NotActive : FastingError
}

sealed interface FastingResult<out T> {
    data class Ok<T>(val value: T) : FastingResult<T>
    data class Err(val error: FastingError) : FastingResult<Nothing>
}

object FastingMachine {

    fun start(id: String, protocolId: String, fastingHours: Int, now: Long): FastingSnapshot =
        FastingSnapshot(
            id = id,
            protocolId = protocolId,
            startedAt = now,
            targetEndAt = now + fastingHours * HOUR_MS,
            endedAt = null,
            status = FastingStatus.ACTIVE,
        )

    fun adjustStart(session: FastingSnapshot, newStartedAt: Long, now: Long): FastingResult<FastingSnapshot> {
        if (session.status != FastingStatus.ACTIVE) return FastingResult.Err(FastingError.NotActive)
        if (newStartedAt > now) return FastingResult.Err(FastingError.StartInFuture)
        val durationMs = session.targetEndAt - session.startedAt
        return FastingResult.Ok(
            session.copy(startedAt = newStartedAt, targetEndAt = newStartedAt + durationMs),
        )
    }

    fun finish(session: FastingSnapshot, now: Long): FastingResult<FastingSnapshot> {
        if (session.status != FastingStatus.ACTIVE) return FastingResult.Err(FastingError.NotActive)
        val status = if (now >= session.targetEndAt) FastingStatus.COMPLETED else FastingStatus.BROKEN
        return FastingResult.Ok(session.copy(endedAt = now, status = status))
    }

    fun breakFast(session: FastingSnapshot, now: Long): FastingResult<FastingSnapshot> {
        if (session.status != FastingStatus.ACTIVE) return FastingResult.Err(FastingError.NotActive)
        return FastingResult.Ok(session.copy(endedAt = now, status = FastingStatus.BROKEN))
    }

    fun progress(session: FastingSnapshot, now: Long): FastingProgress {
        val elapsed = (now - session.startedAt).coerceAtLeast(0L)
        val total = (session.targetEndAt - session.startedAt).coerceAtLeast(1L)
        val fraction = (elapsed.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
        return FastingProgress(
            elapsedMs = elapsed,
            remainingMs = session.targetEndAt - now,
            fraction = fraction,
            reachedGoal = now >= session.targetEndAt,
        )
    }
}
