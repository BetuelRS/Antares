package pt.antares.app.core.health

import kotlin.math.max
import kotlin.math.min

/**
 * Impede que o mesmo treino seja contado duas vezes. Quem regista um treino no Antares e
 * usa um relógio tem os dois a escrever no Health Connect, e sem isto as calorias do dia
 * apareciam a dobrar.
 *
 * Compara-se por sobreposição de tempo e não por identificador: são sistemas diferentes e
 * nenhum reconhece a linha do outro.
 */
object HealthDedupe {

    // Metade do tempo sobreposto chega para ser o mesmo esforço. Exigir mais falhava com
    // relógios que arrancam a contar antes ou param depois.
    const val OVERLAP_THRESHOLD = 0.5

    fun overlapMs(a: TimeWindow, b: TimeWindow): Long {
        val start = max(a.startMs, b.startMs)
        val end = min(a.endMs, b.endMs)
        return (end - start).coerceAtLeast(0L)
    }

    /**
     * Que fração da sessão de fora está coberta por uma da app. A divisão é pela duração da
     * sessão importada: uma corrida de uma hora não é duplicada por um treino de dez
     * minutos que calhe lá dentro.
     */
    fun coverageOf(session: TimeWindow, own: TimeWindow): Double {
        val duration = session.endMs - session.startMs
        if (duration <= 0) return 0.0
        return overlapMs(session, own).toDouble() / duration.toDouble()
    }

    fun isDuplicate(
        session: TimeWindow,
        own: List<TimeWindow>,
        threshold: Double = OVERLAP_THRESHOLD,
    ): Boolean = own.any { coverageOf(session, it) > threshold }
}
