package pt.antares.app.core.health

import kotlin.math.max
import kotlin.math.min

object HealthDedupe {

    const val OVERLAP_THRESHOLD = 0.5

    fun overlapMs(a: TimeWindow, b: TimeWindow): Long {
        val start = max(a.startMs, b.startMs)
        val end = min(a.endMs, b.endMs)
        return (end - start).coerceAtLeast(0L)
    }

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
