package pt.antares.app.feature.workout.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.feature.workout.WorkoutAlerts
import pt.antares.app.feature.workout.data.SessionPickBus
import pt.antares.app.feature.workout.data.WorkoutSessionRepository
import pt.antares.app.testing.ViewModelHarness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A lista de exercícios do treino em curso é montada a partir de três origens que se sobrepõem:
 * o plano da rotina, o que se acrescentou a meio, e o que já tem séries escritas. A ordem e a
 * ausência de repetições são o que a pessoa vê; se partirem, o treino continua a gravar bem e o
 * ecrã é que passa a mentir. Daí o teste ser sobre a montagem, e não sobre a gravação.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WorkoutSessionBuildTest : ViewModelHarness() {

    /** Conta o que o ViewModel pediu ao sistema, para o descanso poder ser afirmado. */
    private class AlertasEspiao : WorkoutAlerts {
        var agendados = mutableListOf<Int>()
        var cancelamentos = 0
        var emCurso: Boolean? = null
        override fun scheduleRestEnd(seconds: Int) { agendados += seconds }
        override fun cancelRestEnd() { cancelamentos++ }
        override fun setSessionOngoing(active: Boolean) { emCurso = active }
    }

    private val alerts = AlertasEspiao()
    private val bus = SessionPickBus()

    private fun sessionRepository() = WorkoutSessionRepository(
        db.workoutSessionDao(),
        db.workoutSetDao(),
        db.exerciseLogDao(),
        db.weightLogDao(),
        db.routineDao(),
        dispatcher,
    )

    private fun viewModel() = WorkoutSessionViewModel(
        repository = sessionRepository(),
        routineDao = db.routineDao(),
        exerciseDao = db.exerciseLibraryDao(),
        alerts = alerts,
        pickBus = bus,
    )

    private suspend fun exercicio(id: String, pt: String = "", en: String = id) {
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = id,
                nameEn = en,
                namePt = pt,
                searchText = en,
                category = "strength",
                force = null,
                mechanic = null,
                equipment = null,
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

    private suspend fun rotina(id: String, vararg itens: RoutineItemEntity) {
        db.routineDao().upsertRoutine(
            RoutineEntity(id = id, name = "Rotina", note = null, position = 0, updatedAt = 0L),
        )
        itens.forEach { db.routineDao().upsertItem(it) }
    }

    private fun item(
        routineId: String,
        exerciseId: String,
        position: Int,
        targetSets: Int = 4,
        repsMin: Int = 5,
        repsMax: Int = 8,
        restSec: Int = 180,
        supersetGroup: Int? = null,
    ) = RoutineItemEntity(
        id = "$routineId-$exerciseId",
        routineId = routineId,
        exerciseId = exerciseId,
        targetSets = targetSets,
        targetRepsMin = repsMin,
        targetRepsMax = repsMax,
        targetWeightKg = null,
        restSec = restSec,
        position = position,
        supersetGroup = supersetGroup,
        updatedAt = 0L,
    )

    /** Uma sessão já terminada, que é de onde saem as séries-fantasma. */
    private suspend fun sessaoAntiga(id: String, exerciseId: String, series: List<Pair<Double, Int>>) {
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = id,
                startedAt = 1_000L,
                endedAt = 2_000L,
                routineId = null,
                note = null,
                status = SessionStatus.DONE,
                updatedAt = 0L,
            ),
        )
        series.forEachIndexed { i, (peso, reps) ->
            db.workoutSetDao().upsertSet(
                WorkoutSetEntity(
                    id = "$id-$exerciseId-$i",
                    sessionId = id,
                    exerciseId = exerciseId,
                    setIndex = i,
                    weightKg = peso,
                    reps = reps,
                    rpe = null,
                    updatedAt = 0L,
                ),
            )
        }
    }

    @Test
    fun `sem treino em curso o ecra nao inventa exercicios`() = runTest(dispatcher) {
        val vm = viewModel()
        val estado = vm.state.first { !it.loading }

        assertEquals(null, estado.sessionId)
        assertTrue(estado.exercises.isEmpty())
    }

    @Test
    fun `os exercicios saem pela ordem do plano`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        exercicio("supino", pt = "Supino")
        exercicio("remada", pt = "Remada")
        rotina(
            "r1",
            item("r1", "agachamento", position = 0),
            item("r1", "supino", position = 1),
            item("r1", "remada", position = 2),
        )

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        assertEquals(
            listOf("agachamento", "supino", "remada"),
            estado.exercises.map { it.exerciseId },
        )
        assertEquals(listOf("Agachamento", "Supino", "Remada"), estado.exercises.map { it.name })
    }

    @Test
    fun `sem nome em portugues fica o ingles, e nao um espaco em branco`() = runTest(dispatcher) {
        exercicio("hip-thrust", pt = "", en = "Hip Thrust")
        rotina("r1", item("r1", "hip-thrust", position = 0))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        assertEquals("Hip Thrust", estado.exercises.single().name)
    }

    @Test
    fun `o exercicio acrescentado a meio vai para o fim, e so uma vez`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        exercicio("biceps", pt = "Bíceps")
        rotina("r1", item("r1", "agachamento", position = 0))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        vm.addExercise("biceps")
        vm.addExercise("biceps")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.size > 1 }
        assertEquals(listOf("agachamento", "biceps"), estado.exercises.map { it.exerciseId })
    }

    @Test
    fun `um exercicio do plano que ja tem series nao aparece duas vezes`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        rotina("r1", item("r1", "agachamento", position = 0))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val inicial = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        vm.logSet(inicial.exercises.single(), weightKg = 100.0, reps = 5, rpe = null, warmup = false)
        advanceUntilIdle()

        val depois = vm.state.value
        assertEquals(1, depois.exercises.size, "o exercício duplicou ao ganhar séries")
        assertEquals(1, depois.exercises.single().sets.size)
    }

    @Test
    fun `series de um exercicio que saiu da rotina continuam a aparecer`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        exercicio("orphan", pt = "Esquecido")
        rotina("r1", item("r1", "agachamento", position = 0))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val sessionId = assertNotNull(vm.state.first { !it.loading }.sessionId)

        // Escrito por baixo do ViewModel de propósito: é o que acontece quando a rotina é
        // editada depois do treino ter começado, e o exercício deixa de estar no plano.
        sessionRepository().putSet(
            id = "s1",
            sessionId = sessionId,
            exerciseId = "orphan",
            setIndex = 0,
            weightKg = 20.0,
            reps = 10,
            rpe = null,
            isWarmup = false,
        )
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.size == 2 }
        assertEquals(listOf("agachamento", "orphan"), estado.exercises.map { it.exerciseId })
    }

    @Test
    fun `fora do plano os alvos sao os de recurso, e nao campos vazios`() = runTest(dispatcher) {
        exercicio("biceps", pt = "Bíceps")
        exercicio("agachamento", pt = "Agachamento")
        rotina("r1", item("r1", "agachamento", position = 0, targetSets = 4, restSec = 180))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()
        vm.addExercise("biceps")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.size == 2 }
        val extra = estado.exercises.first { it.exerciseId == "biceps" }
        assertEquals(3, extra.targetSets)
        assertEquals(8, extra.repsMin)
        assertEquals(12, extra.repsMax)
        assertEquals(90, extra.restSec)

        val planeado = estado.exercises.first { it.exerciseId == "agachamento" }
        assertEquals(4, planeado.targetSets, "o exercício do plano perdeu os alvos da rotina")
        assertEquals(180, planeado.restSec)
    }

    @Test
    fun `as series da ultima vez aparecem como fantasma, ordenadas`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        rotina("r1", item("r1", "agachamento", position = 0))
        sessaoAntiga("antiga", "agachamento", listOf(100.0 to 5, 102.5 to 5, 105.0 to 3))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        val fantasma = estado.exercises.single().ghost
        assertEquals(listOf(100.0, 102.5, 105.0), fantasma.map { it.weightKg })
        assertEquals(listOf(0, 1, 2), fantasma.map { it.setIndex })
    }

    @Test
    fun `o treino em curso nao se copia a si mesmo`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        rotina("r1", item("r1", "agachamento", position = 0))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val inicial = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        vm.logSet(inicial.exercises.single(), weightKg = 90.0, reps = 5, rpe = null, warmup = false)
        advanceUntilIdle()

        assertTrue(
            vm.state.value.exercises.single().ghost.isEmpty(),
            "a série escrita agora voltou como fantasma dela própria",
        )
    }

    @Test
    fun `uma serie a serio arranca o descanso da rotina`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        rotina("r1", item("r1", "agachamento", position = 0, restSec = 180))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        vm.logSet(estado.exercises.single(), weightKg = 100.0, reps = 5, rpe = null, warmup = false)

        assertEquals(listOf(180), alerts.agendados)
        assertEquals(180, vm.restRemaining.value)
    }

    @Test
    fun `o aquecimento nao arranca descanso nenhum`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        rotina("r1", item("r1", "agachamento", position = 0, restSec = 180))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        vm.logSet(estado.exercises.single(), weightKg = 40.0, reps = 10, rpe = null, warmup = true)

        assertTrue(alerts.agendados.isEmpty(), "o aquecimento agendou descanso")
        assertEquals(null, vm.restRemaining.value)
    }

    @Test
    fun `numa supersérie passa-se logo ao exercicio seguinte`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        rotina("r1", item("r1", "supino", position = 0, restSec = 180, supersetGroup = 1))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        vm.logSet(estado.exercises.single(), weightKg = 60.0, reps = 8, rpe = null, warmup = false)

        assertTrue(alerts.agendados.isEmpty(), "a supersérie mandou descansar pelo meio")
    }

    @Test
    fun `saltar o descanso apaga o numero e o alarme`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        rotina("r1", item("r1", "agachamento", position = 0, restSec = 180))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        vm.logSet(estado.exercises.single(), weightKg = 100.0, reps = 5, rpe = null, warmup = false)
        vm.skipRest()

        assertEquals(null, vm.restRemaining.value)
        assertEquals(1, alerts.cancelamentos, "o alarme do sistema ficou a tocar sozinho")
    }
}
