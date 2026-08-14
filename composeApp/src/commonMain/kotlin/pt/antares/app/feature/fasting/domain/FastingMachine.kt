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

/**
 * As transições de estado de um jejum, puras e sem dependências. Recebe o instante como
 * parâmetro em vez de ler o relógio: é isso que permite testar um jejum de dezasseis horas
 * sem esperar dezasseis horas.
 *
 * Devolve [FastingResult] em vez de lançar — as falhas aqui são recusas previstas, e o
 * ecrã trata-as como resposta.
 */
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
        // A duração do protocolo é preservada e a hora-alvo acompanha o novo início: quem
        // se esqueceu de marcar o jejum e corrige a hora quer as mesmas horas de jejum,
        // não acabar mais cedo.
        val durationMs = session.targetEndAt - session.startedAt
        return FastingResult.Ok(
            session.copy(startedAt = newStartedAt, targetEndAt = newStartedAt + durationMs),
        )
    }

    /**
     * Termina o jejum. O estado sai do relógio e não da intenção: quem carrega em terminar
     * antes da hora fica com um jejum interrompido, mesmo tendo carregado no outro botão.
     */
    fun finish(session: FastingSnapshot, now: Long): FastingResult<FastingSnapshot> {
        if (session.status != FastingStatus.ACTIVE) return FastingResult.Err(FastingError.NotActive)
        val status = if (now >= session.targetEndAt) FastingStatus.COMPLETED else FastingStatus.BROKEN
        return FastingResult.Ok(session.copy(endedAt = now, status = status))
    }

    /** Quebrar é sempre interrompido, mesmo depois da hora — foi o que a pessoa declarou. */
    fun breakFast(session: FastingSnapshot, now: Long): FastingResult<FastingSnapshot> {
        if (session.status != FastingStatus.ACTIVE) return FastingResult.Err(FastingError.NotActive)
        return FastingResult.Ok(session.copy(endedAt = now, status = FastingStatus.BROKEN))
    }

    fun progress(session: FastingSnapshot, now: Long): FastingProgress {
        // Os `coerceAtLeast` protegem de relógios acertados para trás e de um protocolo de
        // duração zero, que dariam progresso negativo ou divisão por zero.
        val elapsed = (now - session.startedAt).coerceAtLeast(0L)
        val total = (session.targetEndAt - session.startedAt).coerceAtLeast(1L)
        val fraction = (elapsed.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
        return FastingProgress(
            elapsedMs = elapsed,
            // Fica negativo depois da hora, de propósito: é o que deixa o ecrã mostrar
            // quanto tempo já se passou do objetivo em vez de parar em zero.
            remainingMs = session.targetEndAt - now,
            fraction = fraction,
            reachedGoal = now >= session.targetEndAt,
        )
    }
}
