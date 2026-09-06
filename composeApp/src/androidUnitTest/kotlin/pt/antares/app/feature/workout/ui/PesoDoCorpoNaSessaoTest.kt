package pt.antares.app.feature.workout.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.calc.CargaDoCorpo
import pt.antares.app.core.calc.SetEntry
import pt.antares.app.core.calc.VolumeCalc
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.feature.workout.NoopWorkoutAlerts
import pt.antares.app.feature.workout.data.SessionPickBus
import pt.antares.app.testing.Fabricas
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Os exercícios de peso do corpo, na sessão.
 *
 * Classe à parte da [WorkoutSessionBuildTest] por uma razão simples: são cento e onze
 * exercícios do catálogo que **não se conseguiam registar de todo**, e o que se defende aqui
 * não é a montagem da lista — é que o `weightKg` de uma série continua a querer dizer a mesma
 * coisa depois de o corpo entrar na conta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PesoDoCorpoNaSessaoTest : ViewModelHarness() {

    private fun viewModel() = vivo(WorkoutSessionViewModel(
        repository = Fabricas.workoutSessionRepository(db, dispatcher),
        routineDao = db.routineDao(),
        exerciseDao = db.exerciseLibraryDao(),
        alerts = NoopWorkoutAlerts(),
        profileRepository = Fabricas.profileRepository(db, dispatcher),
        pickBus = SessionPickBus(),
    ),
    )

    /** Ver a razão em [WorkoutSessionBuildTest]: sem coletor, o `state.value` fica parado. */
    private fun TestScope.mantemVivo(vm: WorkoutSessionViewModel) {
        backgroundScope.launch { vm.state.collect { } }
    }

    private suspend fun exercicio(id: String, pt: String, equipamento: String?) {
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = id,
                nameEn = id,
                namePt = pt,
                searchText = id,
                category = "strength",
                force = null,
                mechanic = null,
                equipment = equipamento,
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

    private fun item(routineId: String, exerciseId: String) = RoutineItemEntity(
        id = "$routineId-$exerciseId",
        routineId = routineId,
        exerciseId = exerciseId,
        targetSets = 3,
        targetRepsMin = 8,
        targetRepsMax = 12,
        targetWeightKg = null,
        restSec = 90,
        position = 0,
        supersetGroup = null,
        updatedAt = 0L,
    )

    private suspend fun flexao() {
        exercicio("flexao", pt = "Flexão", equipamento = CargaDoCorpo.EQUIPAMENTO_DO_CORPO)
        db.routineDao().upsertRoutine(RoutineEntity("r1", "Casa", null, 0, updatedAt = 0L))
        db.routineDao().upsertItem(item("r1", "flexao"))
    }

    private suspend fun pesa(kg: Double) {
        db.weightLogDao().upsert(
            WeightLogEntity(id = "w", epochDay = 20_000L, weightKg = kg, note = null, updatedAt = 0L),
        )
    }

    /**
     * **O buraco que esta versão fecha.** Cento e onze exercícios do catálogo são `body only`,
     * e a validação exige um peso maior do que zero — uma flexão escrevia-se com `0` e o botão
     * ficava cinzento. O estado tem de dizer que este exercício é desses, e o peso da pessoa
     * tem de lá chegar.
     */
    @Test
    fun `um exercicio de peso do corpo diz que o e, e traz o peso da pessoa`() = runTest(dispatcher) {
        flexao()
        pesa(78.0)

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        assertTrue(estado.exercises.single().dePesoDoCorpo)
        assertEquals(78.0, estado.pesoDoCorpoKg)
        assertEquals(100, estado.exercises.single().percentagemDoCorpo, "começa no peso todo")
    }

    /**
     * O `weightKg` **continua a ser a carga total**, e é isso que faz o volume, o 1RM e os
     * recordes funcionarem sem saber nada de peso do corpo. O que é novo é a coluna que diz
     * quanto dessa carga veio do corpo — e é ela que deixa o ecrã escrever «peso do corpo»
     * em vez de deixar a pessoa a olhar para 78 kg sem perceber de onde saíram.
     */
    @Test
    fun `a serie guarda a carga total e diz quanto dela veio do corpo`() = runTest(dispatcher) {
        flexao()
        pesa(78.0)

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        // Uma dominada com dez quilos no cinto: 78 do corpo, 88 no total.
        vm.logSet(estado.exercises.single(), weightKg = 88.0, reps = 6, warmup = false, bodyweightKg = 78.0)
        advanceUntilIdle()

        val serie = vm.state.value.exercises.single().sets.single()
        assertEquals(88.0, serie.weightKg)
        assertEquals(78.0, serie.bodyweightKg)
        assertEquals(
            88.0 * 6,
            VolumeCalc.volume(listOf(SetEntry(serie.weightKg, serie.reps, serie.isWarmup))),
            "o volume tem de contar a carga toda, ou uma dominada continua a valer zero",
        )
    }

    /**
     * Sem peso registado, a app **não inventa um**. Um `70` de recurso seria um número que a
     * pessoa nunca escreveu a entrar no volume, no 1RM e nos recordes dela — que é exactamente
     * o veneno que o `motor/05` descreve em quem escrevia `1` para contornar o botão cinzento.
     */
    @Test
    fun `sem peso registado nao ha carga, e o estado di-lo`() = runTest(dispatcher) {
        flexao()

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        assertEquals(null, estado.pesoDoCorpoKg)
        assertEquals(
            null,
            CargaDoCorpo.calcular(estado.pesoDoCorpoKg, 100, adicionalKg = 0.0),
            "sem peso não há carga nenhuma para gravar",
        )
    }

    /** A percentagem é da pessoa e fica; voltar aos 100 % apaga a linha em vez de a gravar. */
    @Test
    fun `a percentagem por exercicio guarda-se, e voltar aos cem apaga a linha`() = runTest(dispatcher) {
        flexao()
        pesa(78.0)

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        vm.savePercentagem("flexao", 65)
        advanceUntilIdle()
        assertEquals(65, vm.state.value.exercises.single().percentagemDoCorpo)
        assertEquals(1, db.exerciseLoadDao().exportRows().size)

        vm.savePercentagem("flexao", 100)
        advanceUntilIdle()
        assertEquals(100, vm.state.value.exercises.single().percentagemDoCorpo)
        assertTrue(
            db.exerciseLoadDao().exportRows().isEmpty(),
            "a ausência de linha já quer dizer 100 %; guardá-la era dizê-lo duas vezes",
        )
    }

    /**
     * **A razão de esta versão ser MENOR.** As séries antigas não se tocam: a coluna nova nasce
     * nula, e nulo quer dizer «nenhuma parte desta carga veio do corpo» — que é o que era
     * verdade antes de ela existir. Reescrever números que a pessoa gravou é o que a app não
     * faz, e um `1` escrito à mão não se distingue de um haltere de 1 kg.
     */
    @Test
    fun `uma serie de barra continua sem parte nenhuma do corpo`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino", equipamento = "barbell")
        db.routineDao().upsertRoutine(RoutineEntity("r1", "Empurrar", null, 0, updatedAt = 0L))
        db.routineDao().upsertItem(item("r1", "supino"))

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        assertTrue(!estado.exercises.single().dePesoDoCorpo)

        vm.logSet(estado.exercises.single(), weightKg = 60.0, reps = 8, warmup = false)
        advanceUntilIdle()

        assertEquals(null, vm.state.value.exercises.single().sets.single().bodyweightKg)
    }
    /**
     * Corrigir uma série de peso do corpo para menos do que o corpo pesava deixa de dizer
     * que parte dela veio de lá — porque deixou de ser verdade. O que fica é o total, que é
     * o que a app sabe.
     */
    @Test
    fun `corrigir a serie abaixo do corpo tira-lhe a reparticao`() = runTest(dispatcher) {
        flexao()
        pesa(78.0)

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        vm.logSet(estado.exercises.single(), weightKg = 88.0, reps = 6, warmup = false, bodyweightKg = 78.0)
        advanceUntilIdle()

        // Corrigida para 85: os 78 do corpo continuam a caber.
        vm.updateSet(vm.state.value.exercises.single().sets.single(), weightKg = 85.0, reps = 6)
        advanceUntilIdle()
        assertEquals(78.0, vm.state.value.exercises.single().sets.single().bodyweightKg)

        // Corrigida para 40: não cabem.
        vm.updateSet(vm.state.value.exercises.single().sets.single(), weightKg = 40.0, reps = 6)
        advanceUntilIdle()
        val corrigida = vm.state.value.exercises.single().sets.single()
        assertEquals(40.0, corrigida.weightKg)
        assertEquals(null, corrigida.bodyweightKg, "78 kg de corpo dentro de uma série de 40")
    }
}
