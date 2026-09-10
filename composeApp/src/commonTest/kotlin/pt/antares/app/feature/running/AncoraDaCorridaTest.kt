package pt.antares.app.feature.running

import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.GeoSample
import pt.antares.app.feature.running.domain.RunEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Uma posição velha no arranque não pode segurar a corrida.
 *
 * Visto a correr a 2.31.0: a primeira posição que chegou foi a última da sessão anterior —
 * Lisboa —, e as seguintes vinham do Porto. O motor fez da primeira a âncora e descartou as
 * outras como saltos impossíveis; a 12 m/s, ia continuar a descartá-las durante **seis horas**.
 * A corrida só se recompôs porque foi posta em pausa. E o mapa desenhou uma linha de Lisboa
 * ao Porto, e o desnível ganhou 90 m, porque o percurso guardava os saltos descartados.
 *
 * O que se guarda é a diferença entre as duas coisas que o motor tem de saber separar: um
 * pico, que fica sozinho e se descarta, e uma âncora errada, que se corrige.
 */
class AncoraDaCorridaTest {

    private val lisboa = 38.7223 to -9.1393
    private val porto = 41.1579 to -8.6291

    // Quatro metros para norte por segundo, a 90 m de altitude.
    private fun noPorto(i: Int) = GeoSample(
        tMs = 1000L * i,
        lat = porto.first + i * 4.0 / 111_320.0,
        lon = porto.second,
        altM = 90.0,
        accM = 5.0,
    )

    private fun emLisboa(i: Int) = GeoSample(1000L * i, lisboa.first, lisboa.second, altM = 0.0, accM = 5.0)

    private fun motor() = RunEngine(ActivityType.RUN, weightKg = 70.0, autoPauseEnabled = false)

    @Test
    fun `uma primeira posicao velha nao segura a corrida`() {
        val m = motor()
        m.onSample(emLisboa(0))
        (1..120).forEach { m.onSample(noPorto(it)) }
        val r = m.finish()

        // Do quinto segundo ao 120.º, a 4 m/s: 460 m. Os cinco primeiros são os que o motor
        // precisa para ter a certeza de que não é um pico.
        assertTrue(r.metrics.distanceM in 440.0..465.0, "distância=${r.metrics.distanceM}")
        assertEquals(0.0, r.metrics.elevGainM, 0.001, "a subida de Lisboa ao Porto não se correu")
        assertTrue(m.percurso().none { it.first < 40.0 }, "Lisboa ficou no percurso")
    }

    @Test
    fun `um pico isolado continua descartado, e fora do percurso`() {
        val m = motor()
        (0..10).forEach { m.onSample(noPorto(it)) }
        m.onSample(emLisboa(11))
        (12..20).forEach { m.onSample(noPorto(it)) }

        assertTrue(m.percurso().none { it.first < 40.0 }, "o pico desenhou um risco no mapa")
        val d = m.finish().metrics.distanceM
        assertTrue(d in 75.0..82.0, "distância=$d")
    }

    @Test
    fun `saltos alternados com posicoes boas nao mudam a ancora`() {
        // Um GPS a devolver a posição velha de dois em dois segundos: os saltos concordam uns
        // com os outros, mas nunca são seguidos, e por isso nunca chegam a mudar a âncora.
        val m = motor()
        m.onSample(noPorto(0))
        (1..40).forEach { i -> m.onSample(if (i % 2 == 0) noPorto(i) else emLisboa(i)) }

        assertTrue(m.percurso().none { it.first < 40.0 })
        val d = m.finish().metrics.distanceM
        assertTrue(d in 150.0..162.0, "distância=$d")
    }
}
