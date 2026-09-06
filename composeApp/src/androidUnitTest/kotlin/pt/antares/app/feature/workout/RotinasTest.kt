package pt.antares.app.feature.workout

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.calc.Progressao
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineScheduleEntity
import pt.antares.app.core.model.RegraDeProgressao
import pt.antares.app.feature.workout.ui.RoutineEditViewModel
import pt.antares.app.testing.Fabricas
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O editor de rotinas.
 *
 * O que estes testes defendem são duas decisões que não se leem no ecrã: **duplicar não leva
 * o calendário**, e **reordenar é uma escrita só, com volta atrás**. A segunda existe porque
 * mover era a única acção do editor sem desfazer — e é mais fácil de fazer por engano do que
 * apagar, que o tem desde sempre.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RotinasTest : ViewModelHarness() {

    private fun repo() = Fabricas.routineRepository(db, dispatcher)

    private suspend fun exercicio(id: String) {
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = id,
                nameEn = id,
                namePt = id,
                searchText = id,
                category = "strength",
                force = null,
                mechanic = null,
                equipment = "barbell",
                level = "beginner",
                primaryMuscles = "[]",
                secondaryMuscles = "[]",
                instructionsEnJson = "[]",
                instructionsPtJson = "[]",
                imagesJson = "[]",
                updatedAt = 0L,
            ),
        )
    }

    private suspend fun rotinaCom(vararg exercicios: String): String {
        val id = repo().createRoutine("Empurrar A")
        exercicios.forEach { exercicio(it); repo().addItem(id, it) }
        return id
    }

    @Test
    fun `duplicar copia os exercicios, os alvos e os grupos`() = runTest(dispatcher) {
        val original = rotinaCom("supino", "desenvolvimento", "triceps")
        val itens = db.routineDao().itemsOf(original).sortedBy { it.position }
        repo().updateTargets(itens[0].id, sets = 5, repsMin = 3, repsMax = 5, weightKg = 100.0, restSec = 180)
        repo().setSuperset(itens[1].id, 1)
        repo().setSuperset(itens[2].id, 1)

        val copiaId = repo().duplicateRoutine(original, "Empurrar B")!!
        val copia = db.routineDao().itemsOf(copiaId).sortedBy { it.position }

        assertEquals("Empurrar B", db.routineDao().routineById(copiaId)!!.name)
        assertEquals(3, copia.size)
        assertEquals(listOf("supino", "desenvolvimento", "triceps"), copia.map { it.exerciseId })
        assertEquals(5, copia[0].targetSets)
        assertEquals(100.0, copia[0].targetWeightKg)
        assertEquals(180, copia[0].restSec)
        assertEquals(listOf(null, 1, 1), copia.map { it.supersetGroup })
    }

    @Test
    fun `duplicar nao copia os dias do calendario`() = runTest(dispatcher) {
        val original = rotinaCom("supino")
        db.routineScheduleDao().upsert(RoutineScheduleEntity(dayOfWeek = 2, routineId = original, updatedAt = 0L))

        val copiaId = repo().duplicateRoutine(original, "Empurrar B")!!

        val horario = db.routineScheduleDao().exportRows()
        assertEquals(1, horario.size, "a cópia foi ocupar um dia do calendário")
        assertEquals(original, horario.single().routineId)
        assertTrue(copiaId != horario.single().routineId)
    }

    /**
     * As linhas novas não herdam o id da original: seriam **a mesma linha com dois donos**, e
     * o `upsert` da cópia levava-as com ele — a original ficava vazia sem ninguém dar por isso.
     * A afirmação forte é essa: depois de duplicar, a original continua inteira.
     */
    @Test
    fun `a copia tem linhas proprias e a original fica intacta`() = runTest(dispatcher) {
        val original = rotinaCom("supino", "remada")
        val copiaId = repo().duplicateRoutine(original, "Puxar")!!

        val ids = db.routineDao().itemsOf(original).map { it.id }
        val idsCopia = db.routineDao().itemsOf(copiaId).map { it.id }

        assertEquals(2, ids.size, "a original perdeu os exercícios ao ser copiada")
        assertEquals(2, idsCopia.size)
        assertTrue(ids.intersect(idsCopia.toSet()).isEmpty(), "a cópia partilha linhas com a original")
    }

    /**
     * A ordem grava-se **de uma vez**, no fim do arrasto. Arrastar por cinco posições eram
     * cinco trocas gravadas, cada uma com o seu `updatedAt` e a sua ida à base.
     */
    @Test
    fun `reordenar poe os exercicios pela ordem pedida`() = runTest(dispatcher) {
        val id = rotinaCom("a", "b", "c", "d")
        val antes = db.routineDao().itemsOf(id).sortedBy { it.position }

        repo().reorderItems(listOf(antes[3].id, antes[0].id, antes[2].id, antes[1].id))

        val depois = db.routineDao().itemsOf(id).sortedBy { it.position }
        assertEquals(listOf("d", "a", "c", "b"), depois.map { it.exerciseId })
        assertEquals(listOf(0, 1, 2, 3), depois.map { it.position }, "as posições ficaram com buracos")
    }

    /** O desfazer do mover é a ordem anterior, reposta pelo mesmo caminho. */
    @Test
    fun `reordenar desfaz-se voltando a ordem anterior`() = runTest(dispatcher) {
        val id = rotinaCom("a", "b", "c")
        val antes = db.routineDao().itemsOf(id).sortedBy { it.position }.map { it.id }

        repo().reorderItems(antes.reversed())
        repo().reorderItems(antes)

        val depois = db.routineDao().itemsOf(id).sortedBy { it.position }
        assertEquals(listOf("a", "b", "c"), depois.map { it.exerciseId })
    }

    @Test
    fun `duplicar uma rotina que nao existe nao inventa nenhuma`() = runTest(dispatcher) {
        assertEquals(null, repo().duplicateRoutine("nao-existe", "Seja o que for"))
        assertTrue(db.routineDao().exportRows().isEmpty())
    }

    /** Uma rotina duplicada entra no fim da lista, como uma rotina nova. */
    @Test
    fun `a copia entra no fim da lista`() = runTest(dispatcher) {
        val primeira = rotinaCom("a")
        repo().createRoutine("Outra")

        val copiaId = repo().duplicateRoutine(primeira, "Cópia")!!

        val posicoes = db.routineDao().exportRows().associate { it.id to it.position }
        assertEquals(2, posicoes[copiaId], "a cópia não foi para o fim")
    }

    /**
     * O desfazer do arrasto lê a ordem **ao ViewModel**, e não à base — é a lista que estava no
     * ecrã antes de o dedo lá mexer.
     *
     * Nasceu de a ordem ter passado a ser lida de um segundo `StateFlow` que ninguém
     * coleccionava: ficava parado no `null` inicial, o desfazer gravava uma lista vazia, e nada
     * no ecrã o dizia. Os testes do repositório não viam nada — o defeito estava entre o ecrã e
     * o ViewModel, e é por isso que este teste passa pelos dois.
     */
    @Test
    fun `o editor sabe a ordem em que os exercicios estao`() = runTest(dispatcher) {
        val id = rotinaCom("a", "b", "c")
        val vm = vivo(RoutineEditViewModel(repo(), profileRepository()))
        backgroundScope.launch { vm.estado.collect { } }

        vm.start(id)
        advanceUntilIdle()

        val esperada = db.routineDao().itemsOf(id).sortedBy { it.position }.map { it.id }
        assertEquals(esperada, vm.ordemActual(), "a ordem que o desfazer iria repor")
    }

    /** A regra e o degrau chegam ao estado do editor, que é de onde as propostas saem. */
    @Test
    fun `a regra escolhida aparece no estado do editor`() = runTest(dispatcher) {
        val id = rotinaCom("a")
        val vm = vivo(RoutineEditViewModel(repo(), profileRepository()))
        backgroundScope.launch { vm.estado.collect { } }

        vm.start(id)
        advanceUntilIdle()
        vm.setProgressao(RegraDeProgressao.LINEAR, null)
        advanceUntilIdle()

        val estado = vm.estado.value!!
        assertEquals(RegraDeProgressao.LINEAR, estado.detalhe.routine.progressao)

        // Sem número escolhido, o degrau é o da unidade — e o perfil de teste é métrico.
        assertEquals(Progressao.DEGRAU_KG, estado.incrementoKg)
    }

    /**
     * A cópia leva a regra. É o `copy` da entidade que o faz, e por isso ninguém se lembra de
     * o partir — parte-se sozinho no dia em que alguém escrever os campos à mão.
     */
    @Test
    fun `duplicar leva tambem a regra e o degrau`() = runTest(dispatcher) {
        val original = rotinaCom("supino")
        repo().setProgressao(original, RegraDeProgressao.DUPLA, 2.0)

        val copiaId = repo().duplicateRoutine(original, "Empurrar B")!!
        val copia = db.routineDao().routineById(copiaId)!!

        assertEquals(RegraDeProgressao.DUPLA, copia.progressao)
        assertEquals(2.0, copia.incrementoKg)
    }
}
