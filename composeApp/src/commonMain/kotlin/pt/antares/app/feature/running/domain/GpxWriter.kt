package pt.antares.app.feature.running.domain

import kotlinx.datetime.Instant

object GpxWriter {

    fun write(
        name: String,
        type: ActivityType,
        startedAtMs: Long,
        path: List<Pair<Double, Double>>,
    ): String {
        val safeName = escape(name.ifBlank { "Antares" })
        val time = Instant.fromEpochMilliseconds(startedAtMs).toString()
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"Antares\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <metadata><name>").append(safeName).append("</name><time>").append(time).append("</time></metadata>\n")
        sb.append("  <trk><name>").append(safeName).append("</name><type>").append(type.name).append("</type><trkseg>\n")
        for ((lat, lon) in path) {
            sb.append("    <trkpt lat=\"").append(fmt(lat)).append("\" lon=\"").append(fmt(lon)).append("\"></trkpt>\n")
        }
        sb.append("  </trkseg></trk>\n")
        sb.append("</gpx>\n")
        return sb.toString()
    }

    private fun fmt(v: Double): String {
        val scaled = kotlin.math.round(v * 1_000_000.0).toLong()
        val neg = scaled < 0
        val abs = if (neg) -scaled else scaled
        val intPart = abs / 1_000_000
        val frac = (abs % 1_000_000).toString().padStart(6, '0')
        return "${if (neg) "-" else ""}$intPart.$frac"
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
