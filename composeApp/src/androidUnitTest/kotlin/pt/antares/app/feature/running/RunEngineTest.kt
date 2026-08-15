package pt.antares.app.feature.running

import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.GeoSample
import pt.antares.app.feature.running.domain.RunEngine
import pt.antares.app.feature.running.domain.RunMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunEngineTest {

    private fun feed(engine: RunEngine, samples: List<GeoSample>): List<RunMetrics> =
        samples.map { engine.onSample(it) }

    @Test
    fun `corrida limpa distancia dentro de mais menos 1 porcento`() {
        val engine = RunEngine(ActivityType.RUN, weightKg = 70.0)
        val samples = GpxTestReader.read("clean_run.gpx")
        feed(engine, samples)
        val r = engine.finish()

        assertTrue(r.metrics.distanceM in 990.0..1010.0, "distância=${r.metrics.distanceM}")

        assertTrue(r.metrics.avgPaceSecPerKm in 480..520, "pace=${r.metrics.avgPaceSecPerKm}")

        assertTrue(r.splits.isNotEmpty())
        assertTrue(r.splits.first().distanceM in 990.0..1010.0)
    }

    @Test
    fun `tunel atravessa o gap em linha reta sem teleporte`() {
        val engine = RunEngine(ActivityType.RUN, weightKg = 70.0)
        val samples = GpxTestReader.read("tunnel_run.gpx")
        feed(engine, samples)
        val r = engine.finish()

        assertTrue(r.metrics.distanceM in 960.0..1040.0, "distância=${r.metrics.distanceM}")
    }

    @Test
    fun `caminhada com paragem dispara auto-pausa e exclui tempo parado`() {
        val engine = RunEngine(ActivityType.WALK, weightKg = 70.0, autoPauseEnabled = true)
        val samples = GpxTestReader.read("noisy_walk.gpx")
        val metricsSeq = feed(engine, samples)
        val r = engine.finish()

        assertTrue(metricsSeq.any { it.paused }, "auto-pausa nunca disparou")

        assertTrue(r.metrics.movingMs < r.metrics.elapsedMs, "moving=${r.metrics.movingMs} elapsed=${r.metrics.elapsedMs}")
    }

    @Test
    fun `com auto-pausa desligada o tempo em movimento iguala o decorrido`() {
        val engine = RunEngine(ActivityType.WALK, weightKg = 70.0, autoPauseEnabled = false)
        val r = engine.let { feed(it, GpxTestReader.read("noisy_walk.gpx")); it.finish() }
        assertEquals(r.metrics.elapsedMs, r.metrics.movingMs)
    }

    @Test
    fun `spike de velocidade e rejeitado sem inflacionar a distancia`() {
        val engine = RunEngine(ActivityType.RUN, weightKg = 70.0)
        val t0 = 0L
        engine.onSample(GeoSample(t0, 38.7223, -9.1393, accM = 5.0))
        engine.onSample(GeoSample(t0 + 5000, 38.7223, -9.13920, accM = 5.0))
        val before = engine.onSample(GeoSample(t0 + 6000, 38.7223, -9.1393, accM = 5.0))

        val after = engine.onSample(GeoSample(t0 + 7000, 38.7300, -9.1393, accM = 5.0))
        assertEquals(before.distanceM, after.distanceM, 0.001)
    }

    @Test
    fun `leitura com precisao acima de 30m e ignorada`() {
        val engine = RunEngine(ActivityType.RUN, weightKg = 70.0)
        engine.onSample(GeoSample(0, 38.7223, -9.1393, accM = 5.0))
        val m = engine.onSample(GeoSample(5000, 38.7223, -9.1300, accM = 50.0))
        assertEquals(0.0, m.distanceM, 0.001)
    }

    @Test
    fun `ganho de elevacao nunca e negativo e ignora descidas`() {

        val engine = RunEngine(ActivityType.RUN, weightKg = 70.0)
        var lon = -9.1393
        val alts = listOf(10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 15.0, 10.0, 5.0)
        alts.forEachIndexed { i, alt ->
            lon += 0.0002
            engine.onSample(GeoSample(i * 5000L, 38.7223, lon, altM = alt, accM = 5.0))
        }
        val r = engine.finish()
        assertTrue(r.metrics.elevGainM > 0.0)
        assertTrue(r.metrics.elevGainM < 20.0, "ganho=${r.metrics.elevGainM}")
    }

    @Test
    fun `as calorias da corrida descontam o repouso do periodo`() {
        val engine = RunEngine(ActivityType.RUN, weightKg = 70.0)
        feed(engine, GpxTestReader.read("clean_run.gpx"))
        val r = engine.finish()

        // Um quilómetro a 70 kg custa cerca de 70 kcal em bruto. Estar vivo durante os
        // mesmos oito minutos custa cerca de 9, e essas já estão na meta do dia.
        val bruto = 1.0 * 70.0 * (r.metrics.distanceM / 1000.0)
        val repouso = 70.0 * (r.metrics.movingMs / 3_600_000.0)

        assertPerto((bruto - repouso).toInt(), r.metrics.kcal, tolerancia = 1)
        assertTrue(
            r.metrics.kcal < bruto.toInt(),
            "somar o valor bruto ao orçamento contava o repouso duas vezes",
        )
    }

    @Test
    fun `os parciais somam o total`() {
        val engine = RunEngine(ActivityType.RUN, weightKg = 70.0)
        feed(engine, GpxTestReader.read("clean_run.gpx"))
        val r = engine.finish()

        // O desconto aplica-se aos dois pela mesma via; se se aplicasse só ao total, os
        // quilómetros no ecrã deixavam de somar o número grande.
        assertPerto(r.metrics.kcal, r.splits.sumOf { it.kcal }, tolerancia = r.splits.size + 1)
    }
}

// Arredondamento por parcial: cada quilómetro arredonda o seu, e a soma pode afastar-se do
// total em uma caloria por parcial.
private fun assertPerto(esperado: Int, obtido: Int, tolerancia: Int) {
    kotlin.test.assertTrue(
        kotlin.math.abs(esperado - obtido) <= tolerancia,
        "esperava $esperado ± $tolerancia, veio $obtido",
    )
}
