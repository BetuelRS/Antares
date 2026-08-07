package pt.antares.app.feature.running

import pt.antares.app.feature.running.domain.TrackPruner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackPrunerTest {

    @Test
    fun `remove pontos demasiado proximos e mantem extremos`() {

        val step = 1.0 / (111320 * kotlin.math.cos(38.7223 * kotlin.math.PI / 180))
        val points = (0..20).map { 38.7223 to (-9.1393 + it * step) }
        val pruned = TrackPruner.prune(points, minMeters = 5.0)

        assertTrue(pruned.size < points.size)
        assertEquals(points.first(), pruned.first())
        assertEquals(points.last(), pruned.last())
    }

    @Test
    fun `listas curtas nao sao alteradas`() {
        val two = listOf(1.0 to 1.0, 2.0 to 2.0)
        assertEquals(two, TrackPruner.prune(two))
    }
}
