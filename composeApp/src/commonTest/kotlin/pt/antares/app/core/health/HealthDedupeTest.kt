package pt.antares.app.core.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthDedupeTest {

    private fun min(m: Long) = m * 60_000L
    private fun window(startMin: Long, endMin: Long) = TimeWindow(min(startMin), min(endMin))

    @Test
    fun `sem sobreposicao nenhuma - a sessao entra`() {

        val session = window(480, 520)
        val own = listOf(window(0, 30))
        assertEquals(0L, HealthDedupe.overlapMs(session, own[0]))
        assertFalse(HealthDedupe.isDuplicate(session, own))
    }

    @Test
    fun `sobreposicao total - e a mesma corrida vista pelo relogio`() {

        val session = window(0, 30)
        val own = listOf(window(0, 30))
        assertEquals(1.0, HealthDedupe.coverageOf(session, own[0]))
        assertTrue(HealthDedupe.isDuplicate(session, own))
    }

    @Test
    fun `sobreposicao parcial - 60 por cento e duplicado, 40 por cento nao`() {
        val own = listOf(window(0, 30))

        val quaseIgual = window(12, 42)
        assertEquals(0.6, HealthDedupe.coverageOf(quaseIgual, own[0]))
        assertTrue(HealthDedupe.isDuplicate(quaseIgual, own))

        val outraCoisa = window(18, 48)
        assertEquals(0.4, HealthDedupe.coverageOf(outraCoisa, own[0]))
        assertFalse(HealthDedupe.isDuplicate(outraCoisa, own))
    }

    @Test
    fun `exatamente 50 por cento entra - o limiar e MAIOR que, nao maior ou igual`() {

        val session = window(15, 45)
        val own = listOf(window(0, 30))
        assertEquals(0.5, HealthDedupe.coverageOf(session, own[0]))
        assertFalse(HealthDedupe.isDuplicate(session, own))
    }

    @Test
    fun `a fracao e sobre a SESSAO, nunca sobre o nosso registo`() {

        val treinoNosso = window(0, 120)
        val corridaDoRelogio = window(30, 50)

        assertEquals(1.0, HealthDedupe.coverageOf(corridaDoRelogio, treinoNosso))
        assertTrue(HealthDedupe.isDuplicate(corridaDoRelogio, listOf(treinoNosso)))

        assertEquals(0.16, (HealthDedupe.coverageOf(treinoNosso, corridaDoRelogio) * 100).toInt() / 100.0)
    }

    @Test
    fun `basta UM dos nossos registos bater certo`() {
        val session = window(60, 90)
        val own = listOf(window(0, 30), window(55, 95), window(200, 230))
        assertTrue(HealthDedupe.isDuplicate(session, own))
    }

    @Test
    fun `sessao de duracao zero ou invertida nao rebenta`() {
        val own = listOf(window(0, 30))
        assertEquals(0.0, HealthDedupe.coverageOf(window(10, 10), own[0]))
        assertEquals(0.0, HealthDedupe.coverageOf(window(30, 10), own[0]))
        assertFalse(HealthDedupe.isDuplicate(window(10, 10), own))
    }
}
