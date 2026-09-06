package pt.antares.app.feature.workout

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pt.antares.app.feature.workout.model.Exercise
import pt.antares.app.feature.workout.ui.ExercicioNaLista
import pt.antares.app.feature.workout.ui.ExerciseLibraryState
import pt.antares.app.feature.workout.ui.LibraryFilters

/**
 * A regra que decide se «os teus» aparecem: **antes de se procurar, e só antes**.
 *
 * São quatro maneiras de estreitar a lista e qualquer uma delas basta. Sem este teste, a que
 * ficasse de fora só se via no aparelho — e via-se como uma secção fixa a empurrar a resposta
 * para fora do ecrã, que é o defeito que a regra existe para evitar.
 */
class EstadoDaBibliotecaTest {

    @Test
    fun `sem filtro nenhum e com favoritos, os teus mostram-se`() {
        assertTrue(ExerciseLibraryState(favoritos = listOf(linha("supino"))).mostrarTeus)
    }

    @Test
    fun `os mais feitos sozinhos tambem chegam para mostrar a seccao`() {
        assertTrue(ExerciseLibraryState(maisFeitos = listOf(linha("agachamento"))).mostrarTeus)
    }

    @Test
    fun `cada um dos quatro filtros esconde os teus`() {
        val base = ExerciseLibraryState(favoritos = listOf(linha("supino")))
        assertFalse(base.copy(filtros = LibraryFilters(query = "sup")).mostrarTeus)
        assertFalse(base.copy(filtros = LibraryFilters(muscle = "chest")).mostrarTeus)
        assertFalse(base.copy(filtros = LibraryFilters(equipment = "barbell")).mostrarTeus)
        assertFalse(base.copy(filtros = LibraryFilters(soMeus = true)).mostrarTeus)
    }

    @Test
    fun `espacos em branco na pesquisa nao escondem nada`() {
        val base = ExerciseLibraryState(favoritos = listOf(linha("supino")))
        assertTrue(base.copy(filtros = LibraryFilters(query = "   ")).mostrarTeus)
    }

    @Test
    fun `sem favoritos nem mais feitos nao ha seccao para mostrar`() {
        assertFalse(ExerciseLibraryState().mostrarTeus)
    }

    private fun linha(id: String) = ExercicioNaLista(
        exercicio = Exercise(
            id = id, nameEn = id, namePt = id, category = "strength", force = null,
            mechanic = null, equipment = null, level = "beginner",
            primaryMuscles = emptyList(), secondaryMuscles = emptyList(),
            instructionsEn = emptyList(), instructionsPt = emptyList(),
            imageUrls = emptyList(), isCustom = false, verified = false,
        ),
        favorito = true,
    )
}
