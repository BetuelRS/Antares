package pt.antares.app.feature.running

import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.GpxWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GpxWriterTest {

    private val path = listOf(38.72230 to -9.13930, 38.72240 to -9.13910, 38.72255 to -9.13880)

    @Test
    fun `estrutura gpx 1_1 correta`() {
        val gpx = GpxWriter.write("Corrida matinal", ActivityType.RUN, 1_700_000_000_000L, path)
        assertTrue(gpx.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(gpx.contains("<gpx version=\"1.1\""))
        assertTrue(gpx.contains("xmlns=\"http://www.topografix.com/GPX/1/1\""))
        assertTrue(gpx.contains("<trkseg>") && gpx.contains("</trkseg>"))
        assertTrue(gpx.trimEnd().endsWith("</gpx>"))

        assertEquals(path.size, Regex("<trkpt ").findAll(gpx).count())

        assertTrue(gpx.contains("lat=\"38.722300\""))
    }

    @Test
    fun `nome com caracteres especiais e escapado`() {
        val gpx = GpxWriter.write("A & B <x>", ActivityType.WALK, 0L, path)
        assertTrue(gpx.contains("A &amp; B &lt;x&gt;"))
        assertTrue(!gpx.contains("<x>"))
    }

    @Test
    fun `tags abrem e fecham em numero igual (bem formado)`() {
        val gpx = GpxWriter.write("t", ActivityType.RIDE, 0L, path)
        assertEquals(Regex("<trkpt ").findAll(gpx).count(), Regex("</trkpt>").findAll(gpx).count())
    }
}
