package pt.antares.app.feature.running

import pt.antares.app.feature.running.domain.GeoSample
import java.time.Instant

object GpxTestReader {

    private val TRKPT = Regex(
        """<trkpt lat="([^"]+)" lon="([^"]+)"><ele>([^<]+)</ele><time>([^<]+)</time>""",
    )

    fun read(fixtureName: String, accM: Double = 5.0): List<GeoSample> {
        val xml = javaClass.classLoader!!
            .getResourceAsStream("fixtures/$fixtureName")!!
            .bufferedReader().use { it.readText() }
        return TRKPT.findAll(xml).map { m ->
            val (lat, lon, ele, time) = m.destructured
            GeoSample(
                tMs = Instant.parse(time).toEpochMilli(),
                lat = lat.toDouble(),
                lon = lon.toDouble(),
                altM = ele.toDouble(),
                accM = accM,
            )
        }.toList()
    }
}
