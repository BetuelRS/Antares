package pt.antares.app.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesempenhoTest {

    private fun serie(peso: Double, reps: Int, sessao: String, quando: Long) =
        SerieFeita(weightKg = peso, reps = reps, sessionId = sessao, startedAt = quando)

    @Test
    fun `sem series feitas nao ha cartao`() {
        assertNull(Desempenho.de(emptyList()))
    }

    @Test
    fun `uma serie a zero nao conta como serie feita`() {
        assertNull(Desempenho.de(listOf(serie(0.0, 12, "s1", 100L))))
        assertNull(Desempenho.de(listOf(serie(60.0, 0, "s1", 100L))))
    }

    @Test
    fun `a melhor serie e a de maior peso vezes repeticoes`() {
        val d = Desempenho.de(
            listOf(
                serie(100.0, 1, "s1", 100L),
                serie(80.0, 8, "s1", 100L),
            ),
        )!!
        assertEquals(80.0, d.melhorPesoKg)
        assertEquals(8, d.melhorReps)
    }

    @Test
    fun `empatadas no volume, ganha a mais pesada`() {
        val d = Desempenho.de(
            listOf(
                serie(60.0, 10, "s1", 100L),
                serie(100.0, 6, "s1", 100L),
            ),
        )!!
        assertEquals(100.0, d.melhorPesoKg)
        assertEquals(6, d.melhorReps)
    }

    @Test
    fun `as vezes contam treinos e nao series`() {
        val d = Desempenho.de(
            listOf(
                serie(60.0, 10, "s1", 100L),
                serie(60.0, 10, "s1", 100L),
                serie(60.0, 10, "s1", 100L),
                serie(62.5, 8, "s2", 200L),
            ),
        )!!
        assertEquals(2, d.vezes)
    }

    @Test
    fun `a ultima vez e o treino mais recente`() {
        val d = Desempenho.de(
            listOf(
                serie(60.0, 10, "s2", 200L),
                serie(60.0, 10, "s1", 100L),
            ),
        )!!
        assertEquals(200L, d.ultimaEm)
    }

    @Test
    fun `sem nenhuma serie dentro das doze repeticoes nao ha 1RM`() {
        val d = Desempenho.de(listOf(serie(40.0, 20, "s1", 100L)))!!
        assertNull(d.umRmKg)

        // E o resto do cartão continua a existir: não ter 1RM não é não ter desempenho.
        assertEquals(1, d.vezes)
        assertEquals(40.0, d.melhorPesoKg)
    }

    @Test
    fun `o 1RM e o melhor de todas as series, e nao o da melhor serie`() {
        // 60 × 10 dá 600 de volume e 80 kg de 1RM; 100 × 3 dá 300 e 110 kg. A melhor série
        // é a primeira e o melhor 1RM é o da segunda — são duas perguntas diferentes.
        val d = Desempenho.de(
            listOf(
                serie(60.0, 10, "s1", 100L),
                serie(100.0, 3, "s1", 100L),
            ),
        )!!
        assertEquals(60.0, d.melhorPesoKg)
        assertTrue(d.umRmKg!! > 109.0 && d.umRmKg!! < 111.0, "1RM foi ${d.umRmKg}")
    }
}
