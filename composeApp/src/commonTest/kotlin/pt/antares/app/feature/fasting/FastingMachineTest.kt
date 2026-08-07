package pt.antares.app.feature.fasting

import pt.antares.app.core.model.FastingStatus
import pt.antares.app.feature.fasting.domain.FastingError
import pt.antares.app.feature.fasting.domain.FastingMachine
import pt.antares.app.feature.fasting.domain.FastingResult
import pt.antares.app.feature.fasting.domain.FastingSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FastingMachineTest {

    private val hour = 3_600_000L
    private val t0 = 1_700_000_000_000L

    private fun ok(r: FastingResult<FastingSnapshot>): FastingSnapshot =
        (r as FastingResult.Ok).value

    @Test
    fun `start cria sessao ACTIVE com alvo = fastingHours a partir de now`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)
        assertEquals(FastingStatus.ACTIVE, s.status)
        assertEquals(t0, s.startedAt)
        assertEquals(t0 + 16 * hour, s.targetEndAt)
        assertEquals(null, s.endedAt)
    }

    @Test
    fun `adjustStart recua o inicio e recalcula o alvo mantendo a duracao`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)
        val adjusted = ok(FastingMachine.adjustStart(s, newStartedAt = t0 - 2 * hour, now = t0))
        assertEquals(t0 - 2 * hour, adjusted.startedAt)

        assertEquals(t0 + 14 * hour, adjusted.targetEndAt)
    }

    @Test
    fun `adjustStart no futuro e rejeitado`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)
        val r = FastingMachine.adjustStart(s, newStartedAt = t0 + hour, now = t0)
        assertEquals(FastingError.StartInFuture, (r as FastingResult.Err).error)
    }

    @Test
    fun `finish no alvo ou depois fica COMPLETED`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)
        val done = ok(FastingMachine.finish(s, now = t0 + 16 * hour))
        assertEquals(FastingStatus.COMPLETED, done.status)
        assertEquals(t0 + 16 * hour, done.endedAt)
    }

    @Test
    fun `finish antes do alvo fica BROKEN`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)
        val done = ok(FastingMachine.finish(s, now = t0 + 10 * hour))
        assertEquals(FastingStatus.BROKEN, done.status)
    }

    @Test
    fun `breakFast e sempre BROKEN mesmo depois do alvo`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)
        val done = ok(FastingMachine.breakFast(s, now = t0 + 20 * hour))
        assertEquals(FastingStatus.BROKEN, done.status)
    }

    @Test
    fun `acoes sobre sessao terminada devolvem NotActive`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)
        val ended = ok(FastingMachine.finish(s, now = t0 + 16 * hour))
        assertEquals(FastingError.NotActive, (FastingMachine.finish(ended, t0) as FastingResult.Err).error)
        assertEquals(FastingError.NotActive, (FastingMachine.breakFast(ended, t0) as FastingResult.Err).error)
        assertEquals(FastingError.NotActive, (FastingMachine.adjustStart(ended, t0, t0) as FastingResult.Err).error)
    }

    @Test
    fun `progresso deriva dos timestamps e nao de contadores`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)

        val p = FastingMachine.progress(s, now = t0 + 8 * hour)
        assertEquals(8 * hour, p.elapsedMs)
        assertEquals(8 * hour, p.remainingMs)
        assertEquals(0.5f, p.fraction, 0.0001f)
        assertTrue(!p.reachedGoal)
    }

    @Test
    fun `progresso clampa a 1 e marca objetivo quando passa o alvo`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)
        val p = FastingMachine.progress(s, now = t0 + 20 * hour)
        assertEquals(1.0f, p.fraction, 0.0001f)
        assertTrue(p.remainingMs < 0)
        assertTrue(p.reachedGoal)
    }

    @Test
    fun `progresso nunca fica negativo antes do inicio`() {
        val s = FastingMachine.start("s1", "fp_16_8", 16, t0)
        val p = FastingMachine.progress(s, now = t0 - hour)
        assertEquals(0L, p.elapsedMs)
        assertEquals(0.0f, p.fraction, 0.0001f)
    }
}
