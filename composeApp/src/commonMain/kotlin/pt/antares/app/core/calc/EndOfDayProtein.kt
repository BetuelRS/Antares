package pt.antares.app.core.calc

import kotlin.math.roundToInt

object EndOfDayProtein {

    const val THRESHOLD = 0.60

    fun gapToNotify(consumedG: Double, targetG: Int, hasLogs: Boolean): Int? {
        if (!hasLogs) return null
        if (targetG <= 0) return null
        if (consumedG >= targetG * THRESHOLD) return null
        val gap = (targetG - consumedG).roundToInt()
        return if (gap > 0) gap else null
    }
}
