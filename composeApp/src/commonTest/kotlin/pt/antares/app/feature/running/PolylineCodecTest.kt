package pt.antares.app.feature.running

import pt.antares.app.feature.running.domain.PolylineCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolylineCodecTest {

    @Test
    fun `exemplo canonico do Google`() {

        val points = listOf(38.5 to -120.2, 40.7 to -120.95, 43.252 to -126.453)
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", PolylineCodec.encode(points))
    }

    @Test
    fun `encode depois decode recupera os pontos dentro de 1e-5`() {
        val points = listOf(
            38.72230 to -9.13930,
            38.72240 to -9.13910,
            38.72255 to -9.13880,
            38.72300 to -9.13800,
        )
        val decoded = PolylineCodec.decode(PolylineCodec.encode(points))
        assertEquals(points.size, decoded.size)
        points.forEachIndexed { i, (lat, lon) ->
            assertTrue(kotlin.math.abs(lat - decoded[i].first) < 1e-5, "lat $i")
            assertTrue(kotlin.math.abs(lon - decoded[i].second) < 1e-5, "lon $i")
        }
    }

    @Test
    fun `lista vazia da string vazia`() {
        assertEquals("", PolylineCodec.encode(emptyList()))
        assertTrue(PolylineCodec.decode("").isEmpty())
    }
}
