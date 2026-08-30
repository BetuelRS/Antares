package pt.antares.app.feature.exercise

import pt.antares.app.core.exercise.MetActivity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Os recentes são um atalho para quem ainda não disse o que quer.
 *
 * Com uma palavra escrita ou uma categoria escolhida, quem está no ecrã já disse — e uma
 * secção de atalhos à cabeça de uma lista filtrada responde a outra pergunta que não a que
 * foi feita.
 */
class RecentesDoExercicioTest {

    private val padel = MetActivity("padel", "Padel", "Padel", 7.0, "sports")

    private val comRecentes = AddExerciseState(loading = false, recentes = listOf(padel))

    @Test
    fun `mostram-se com a caixa vazia e sem categoria`() {
        assertTrue(comRecentes.mostrarRecentes)
    }

    @Test
    fun `desaparecem assim que se escreve`() {
        assertFalse(comRecentes.copy(query = "cor").mostrarRecentes)
    }

    @Test
    fun `desaparecem com uma categoria escolhida`() {
        assertFalse(comRecentes.copy(category = "running").mostrarRecentes)
    }

    @Test
    fun `sem historico nao ha seccao nenhuma`() {
        assertFalse(AddExerciseState(loading = false).mostrarRecentes)
    }
}
