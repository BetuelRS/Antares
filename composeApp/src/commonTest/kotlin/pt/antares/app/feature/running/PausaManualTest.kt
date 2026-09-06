package pt.antares.app.feature.running

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.GeoSample
import pt.antares.app.feature.running.domain.RunEngine

/**
 * A pausa manual, que é o que faltava ao motor.
 *
 * **Não é a mesma bandeira da pausa automática**, e é isso que estes testes defendem: a
 * automática é um *detector* — o `paused` dela apaga-se sozinho assim que há movimento —, e
 * uma pausa que se desligue por a pessoa dar dois passos não é uma pausa.
 */
class PausaManualTest {

    /** Um metro de latitude são cerca de 1/111 320 de grau. Andar em linha recta para norte. */
    private fun ponto(metros: Double, segundos: Long) = GeoSample(
        lat = 38.7 + metros / 111_320.0,
        lon = -9.14,
        altM = 10.0,
        accM = 5.0,
        tMs = segundos * 1000L,
    )

    private fun motor() = RunEngine(ActivityType.RUN, weightKg = 70.0)

    @Test
    fun `a correr, a distancia cresce`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        val m = e.onSample(ponto(20.0, 10))
        assertTrue(m.distanceM > 15.0, "distância=${m.distanceM}")
    }

    @Test
    fun `em pausa manual a distancia nao cresce`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        e.pausar()
        val m = e.onSample(ponto(20.0, 10))
        assertEquals(0.0, m.distanceM)
    }

    /**
     * **O tempo decorrido não pode parar**, e não é uma escolha de ecrã: o `RunRepository`
     * grava `startedAt = agora − elapsedMs`, porque a corrida só vai à base quando termina.
     * Congelar o decorrido durante a pausa punha a hora de início de todas as corridas
     * pausadas à frente do que foi verdade, e ninguém daria por isso.
     */
    @Test
    fun `em pausa manual o tempo em movimento nao cresce, e o decorrido cresce`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        e.pausar()
        val m = e.onSample(ponto(20.0, 60))

        assertEquals(0L, m.movingMs, "o tempo em movimento andou durante a pausa")
        assertEquals(60_000L, m.elapsedMs, "o tempo decorrido é de relógio e não pára")
    }

    @Test
    fun `andar durante a pausa nao desliga a pausa`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        e.pausar()

        // Três amostras de vinte metros cada: é movimento de sobra para a bandeira da pausa
        // automática se apagar. A manual não se apaga.
        e.onSample(ponto(20.0, 10))
        e.onSample(ponto(40.0, 20))
        val m = e.onSample(ponto(60.0, 30))

        assertTrue(m.pausaManual, "a pausa manual desligou-se sozinha")
        assertEquals(0.0, m.distanceM)
    }

    @Test
    fun `o passeio dado em pausa nao entra ao retomar`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        e.pausar()
        e.onSample(ponto(100.0, 60))
        e.retomar()

        // A âncora ficou onde a pessoa está, e não onde ela pausou: os cem metros até ao
        // bebedouro não podem aparecer como um segmento ao retomar.
        val m = e.onSample(ponto(120.0, 70))
        assertTrue(m.distanceM in 15.0..25.0, "distância=${m.distanceM}")
    }

    @Test
    fun `retomar volta a contar tempo`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        e.pausar()
        e.onSample(ponto(0.0, 60))
        e.retomar()
        val m = e.onSample(ponto(20.0, 70))

        assertTrue(!m.pausaManual)
        assertEquals(10_000L, m.movingMs, "o tempo em movimento não recomeçou no sítio certo")
    }

    @Test
    fun `o ritmo deste quilometro conta desde o ultimo parcial, e nao desde o inicio`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))

        // O primeiro quilómetro a 5:00, e os trezentos metros seguintes a 5:33. O ritmo médio
        // fica a 5:07 — e é por isso que os dois números não podem ser o mesmo.
        e.onSample(ponto(1000.0, 300))
        val m = e.onSample(ponto(1300.0, 400))

        assertTrue(m.ritmoDoKmSecPerKm in 328..338, "ritmo do km=${m.ritmoDoKmSecPerKm}")
        assertTrue(m.avgPaceSecPerKm in 303..313, "ritmo médio=${m.avgPaceSecPerKm}")
    }

    @Test
    fun `sem distancia nenhuma o ritmo deste quilometro e zero`() {
        val e = motor()
        val m = e.onSample(ponto(0.0, 0))
        assertEquals(0, m.ritmoDoKmSecPerKm)
    }

    @Test
    fun `os parciais veem-se antes de a corrida acabar`() {
        val e = motor()
        e.onSample(ponto(0.0, 0))
        e.onSample(ponto(1500.0, 400))

        val ateAgora = e.parciaisAteAgora()
        assertEquals(1, ateAgora.size, "o quilómetro fechado devia estar à vista")
        assertTrue(ateAgora.first().distanceM in 990.0..1010.0)
    }
}
