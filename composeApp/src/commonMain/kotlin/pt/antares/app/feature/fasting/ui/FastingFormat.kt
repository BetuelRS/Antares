package pt.antares.app.feature.fasting.ui

import kotlin.math.abs

object FastingFormat {

    fun hm(ms: Long): String {
        val totalMin = abs(ms) / 60_000L
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun hours(ms: Long): String = "${abs(ms) / 3_600_000L}h"
}
