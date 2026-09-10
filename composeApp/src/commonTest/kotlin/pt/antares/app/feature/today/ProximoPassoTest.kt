package pt.antares.app.feature.today

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A regra da linha de «a seguir», que é uma frase só e tem de continuar a ser a mesma frase.
 *
 * O que se guarda é a ordem das coisas — o treino agendado antes das faltas, as faltas só
 * depois do meio-dia, a maior em proporção e não em número — e o silêncio: sem nada que se
 * aplique, não há linha.
 */
class ProximoPassoTest {

    private val pernas = ProximoPasso.Treino(routineId = "r1", nome = "Pernas")

    private fun escolher(
        hora: Int = 15,
        treino: ProximoPasso.Treino? = null,
        treinouHoje: Boolean = false,
        activo: Boolean = false,
        protMeta: Int = 140,
        protComida: Double = 140.0,
        aguaMeta: Int = 2000,
        aguaBebida: Int = 2000,
    ) = ProximoPassoCalc.escolher(hora, treino, treinouHoje, activo, protMeta, protComida, aguaMeta, aguaBebida)

    @Test
    fun `o treino agendado vem primeiro, mesmo de manha`() {
        assertEquals(pernas, escolher(hora = 7, treino = pernas, protComida = 0.0))
    }

    @Test
    fun `o treino ja feito hoje, ou a decorrer, nao se volta a pedir`() {
        assertNull(escolher(treino = pernas, treinouHoje = true))
        assertNull(escolher(treino = pernas, activo = true))
    }

    @Test
    fun `de manha nao se fala de faltas`() {
        assertNull(escolher(hora = 11, protComida = 0.0, aguaBebida = 0))
    }

    @Test
    fun `a tarde, a maior falta em proporcao e nao em numero`() {
        // Faltam 70 g de 140 (metade) e 600 ml de 2000 (30 %): ganha a proteína, embora
        // 600 seja maior do que 70.
        assertEquals(ProximoPasso.Proteina(70), escolher(protComida = 70.0, aguaBebida = 1400))
        // E ao contrário.
        assertEquals(ProximoPasso.Agua(1200), escolher(protComida = 100.0, aguaBebida = 800))
    }

    @Test
    fun `menos de um quarto por fazer nao merece frase`() {
        // 30 de 140 é pouco mais de um quinto.
        assertNull(escolher(protComida = 110.0, aguaBebida = 1600))
    }
}
