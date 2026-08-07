package pt.antares.app.feature.running.ui

object RunFormat {

    fun km(distanceM: Double): String {
        val km = distanceM / 1000.0
        val whole = km.toInt()
        val dec = ((km - whole) * 100).toInt()
        return "$whole,${if (dec < 10) "0$dec" else "$dec"}"
    }

    fun clock(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        fun p(n: Long) = if (n < 10) "0$n" else "$n"
        return if (h > 0) "$h:${p(m)}:${p(s)}" else "${p(m)}:${p(s)}"
    }

    fun pace(secPerKm: Int): String {
        if (secPerKm <= 0) return "--:--"
        val m = secPerKm / 60
        val s = secPerKm % 60
        return "$m:${if (s < 10) "0$s" else "$s"}"
    }

    fun paceFromSpeed(speedMps: Double): String {
        if (speedMps < 0.3) return "--:--"
        return pace((1000.0 / speedMps).toInt())
    }
}
