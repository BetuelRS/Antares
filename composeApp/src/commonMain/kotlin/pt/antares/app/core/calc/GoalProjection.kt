package pt.antares.app.core.calc

import kotlin.math.abs
import kotlin.math.ceil

data class Projection(

    val remainingKg: Double,

    val weeks: Int?,

    val etaEpochDay: Long?,

    val reached: Boolean,

    val movingAway: Boolean,
)

object GoalProjection {

    const val REACHED_TOLERANCE_KG = 0.3

    const val MIN_RATE_KG_WEEK = 0.05

    const val MAX_PROJECTED_WEEKS = 260

    fun project(
        currentKg: Double,
        goalKg: Double,
        measuredRateKgWeek: Double?,
        todayEpochDay: Long,
    ): Projection {
        val delta = currentKg - goalKg
        val remaining = abs(delta)
        if (remaining < REACHED_TOLERANCE_KG) {
            return Projection(remaining, null, null, reached = true, movingAway = false)
        }

        val rate = measuredRateKgWeek
        if (rate == null || abs(rate) < MIN_RATE_KG_WEEK) {
            return Projection(remaining, null, null, reached = false, movingAway = false)
        }

        val movingTowards = (delta > 0 && rate < 0) || (delta < 0 && rate > 0)
        if (!movingTowards) {
            return Projection(remaining, null, null, reached = false, movingAway = true)
        }

        val weeks = ceil(remaining / abs(rate)).toInt()
        if (weeks > MAX_PROJECTED_WEEKS) {
            return Projection(remaining, null, null, reached = false, movingAway = false)
        }
        return Projection(
            remainingKg = remaining,
            weeks = weeks,
            etaEpochDay = todayEpochDay + weeks * 7L,
            reached = false,
            movingAway = false,
        )
    }
}
