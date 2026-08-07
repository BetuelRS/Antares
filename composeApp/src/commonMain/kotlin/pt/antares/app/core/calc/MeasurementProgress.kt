package pt.antares.app.core.calc

import pt.antares.app.core.database.entities.BodyMeasurementEntity
import kotlin.math.abs

data class MeasurementProgress(
    val firstEpochDay: Long,
    val lastEpochDay: Long,
    val waistFrom: Double?,
    val waistTo: Double?,
    val fatFrom: Double?,
    val fatTo: Double?,
) {
    val waistDelta: Double? get() = both(waistFrom, waistTo) { a, b -> b - a }
    val fatDelta: Double? get() = both(fatFrom, fatTo) { a, b -> b - a }

    val spanDays: Long get() = lastEpochDay - firstEpochDay

    val isMeaningful: Boolean
        get() = (waistDelta?.let { abs(it) >= MIN_WAIST_CHANGE_CM } ?: false) ||
            (fatDelta?.let { abs(it) >= MIN_FAT_CHANGE_PCT } ?: false)

    private inline fun both(a: Double?, b: Double?, f: (Double, Double) -> Double): Double? =
        if (a != null && b != null) f(a, b) else null

    companion object {

        const val MIN_WAIST_CHANGE_CM = 1.0
        const val MIN_FAT_CHANGE_PCT = 1.0
    }
}

object MeasurementProgressCalc {

    fun compute(entries: List<BodyMeasurementEntity>): MeasurementProgress? {
        if (entries.size < 2) return null
        val first = entries.first()
        val last = entries.last()

        val waistFirst = entries.firstOrNull { it.waistCm != null }
        val waistLast = entries.lastOrNull { it.waistCm != null }
        val fatFirst = entries.firstOrNull { it.bodyFatPct != null }
        val fatLast = entries.lastOrNull { it.bodyFatPct != null }
        return MeasurementProgress(
            firstEpochDay = first.epochDay,
            lastEpochDay = last.epochDay,
            waistFrom = waistFirst?.takeIf { it !== waistLast }?.waistCm,
            waistTo = waistLast?.takeIf { it !== waistFirst }?.waistCm,
            fatFrom = fatFirst?.takeIf { it !== fatLast }?.bodyFatPct,
            fatTo = fatLast?.takeIf { it !== fatFirst }?.bodyFatPct,
        )
    }
}
