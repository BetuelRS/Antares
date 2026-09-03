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
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.core.calc.CargaDoCorpo
import pt.antares.app.core.calc.SetEntry
import pt.antares.app.core.calc.VolumeCalc
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
        db.sessionExerciseNoteDao(),
        db.exerciseLoadDao(),
        dispatcher,
    )

    private fun viewModel() = WorkoutSessionViewModel(
        repository = sessionRepository(),
        routineDao = db.routineDao(),
        exerciseDao = db.exerciseLibraryDao(),
        alerts = alerts,
        pickBus = bus,
    )

    private suspend fun exercicio(id: String, pt: String = "", en: String = id, equipamento: String? = null) {
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = id,
                nameEn = en,
                namePt = pt,
                searchText = en,
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
        vm.logSet(inicial.exercises.single(), weightKg = 100.0, reps = 5, warmup = false)
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
        vm.logSet(inicial.exercises.single(), weightKg = 90.0, reps = 5, warmup = false)
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
        vm.logSet(estado.exercises.single(), weightKg = 100.0, reps = 5, warmup = false)

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
        vm.logSet(estado.exercises.single(), weightKg = 40.0, reps = 10, warmup = true)

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
        vm.logSet(estado.exercises.single(), weightKg = 60.0, reps = 8, warmup = false)

        assertTrue(alerts.agendados.isEmpty(), "a supersérie mandou descansar pelo meio")
    }

    @Test
    fun `o ecra abre no primeiro exercicio por acabar`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        exercicio("supino", pt = "Supino")
        rotina(
            "r1",
            item("r1", "agachamento", position = 0, targetSets = 1),
            item("r1", "supino", position = 1, targetSets = 3),
        )

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.size == 2 }
        assertEquals("agachamento", estado.currentExerciseId)
    }

    @Test
    fun `com as series do plano feitas passa sozinho ao seguinte`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        exercicio("supino", pt = "Supino")
        rotina(
            "r1",
            item("r1", "agachamento", position = 0, targetSets = 2),
            item("r1", "supino", position = 1, targetSets = 3),
        )

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val primeira = vm.state.first { !it.loading && it.exercises.size == 2 }
        vm.logSet(primeira.exercises.first { it.exerciseId == "agachamento" }, 100.0, 5, false)
        advanceUntilIdle()

        // Esperar pelo estado com a série já lá dentro, e não ler o último valor: sem
        // coletor a viver, o `state.value` fica no que era antes de a série ser escrita.
        val meio = vm.state.first { e -> e.exercises.any { it.exerciseId == "agachamento" && it.setsDone == 1 } }
        assertEquals("agachamento", meio.currentExerciseId, "saltou com uma série a meio do plano")

        vm.logSet(meio.exercises.first { it.exerciseId == "agachamento" }, 100.0, 5, false)
        advanceUntilIdle()

        val fim = vm.state.first { e -> e.exercises.any { it.exerciseId == "agachamento" && it.setsDone == 2 } }
        assertEquals("supino", fim.currentExerciseId)
    }

    @Test
    fun `a escolha da pessoa manda enquanto faltarem series`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        exercicio("supino", pt = "Supino")
        rotina(
            "r1",
            item("r1", "agachamento", position = 0, targetSets = 3),
            item("r1", "supino", position = 1, targetSets = 3),
        )

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()
        vm.state.first { !it.loading && it.exercises.size == 2 }

        vm.select("supino")
        advanceUntilIdle()

        val escolhido = vm.state.first { it.currentExerciseId == "supino" }

        vm.logSet(escolhido.exercises.first { it.exerciseId == "supino" }, 60.0, 8, false)
        advanceUntilIdle()

        val depois = vm.state.first { e -> e.exercises.any { it.exerciseId == "supino" && it.setsDone == 1 } }
        assertEquals(
            "supino",
            depois.currentExerciseId,
            "gravar uma série atirou a pessoa de volta ao exercício de cima",
        )
    }

    @Test
    fun `o aquecimento nao conta para as series do plano`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        exercicio("supino", pt = "Supino")
        rotina(
            "r1",
            item("r1", "agachamento", position = 0, targetSets = 1),
            item("r1", "supino", position = 1, targetSets = 3),
        )

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.size == 2 }
        vm.logSet(estado.exercises.first { it.exerciseId == "agachamento" }, 40.0, 10, warmup = true)
        advanceUntilIdle()

        val depois = vm.state.first { e -> e.exercises.any { it.exerciseId == "agachamento" && it.sets.size == 1 } }
        val agachamento = depois.exercises.first { it.exerciseId == "agachamento" }
        assertEquals(0, agachamento.setsDone, "o aquecimento entrou na conta do plano")
        assertEquals("agachamento", depois.currentExerciseId, "aquecer deu o exercício por feito")
    }

    @Test
    fun `um exercicio que sai do plano nao deixa o ecra sem nenhum aberto`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        exercicio("supino", pt = "Supino")
        rotina(
            "r1",
            item("r1", "agachamento", position = 0, targetSets = 3),
            item("r1", "supino", position = 1, targetSets = 3),
        )

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()
        vm.state.first { !it.loading && it.exercises.size == 2 }

        // Uma escolha que já não existe: é o que fica depois de a rotina ser editada com o
        // treino a andar.
        vm.select("exercicio-que-nao-esta-ca")
        advanceUntilIdle()

        assertEquals("agachamento", vm.state.first { !it.loading }.currentExerciseId)
    }

    @Test
    fun `saltar o descanso apaga o numero e o alarme`() = runTest(dispatcher) {
        exercicio("agachamento", pt = "Agachamento")
        rotina("r1", item("r1", "agachamento", position = 0, restSec = 180))

        val vm = viewModel()
        vm.ensureStarted("r1")
        advanceUntilIdle()

        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        vm.logSet(estado.exercises.single(), weightKg = 100.0, reps = 5, warmup = false)
        vm.skipRest()

        assertEquals(null, vm.restRemaining.value)
        assertEquals(1, alerts.cancelamentos, "o alarme do sistema ficou a tocar sozinho")
    }
    /**
     * Prende o `state` a um coletor durante o teste inteiro.
     *
     * Sem isto, o `SharingStarted.WhileSubscribed(5_000)` larga o upstream assim que o
     * `first { }` do teste devolve e o `advanceUntilIdle` passa os cinco segundos de tempo
     * virtual — e a partir daí o `state.value` fica **parado no último valor**. A primeira
     * mudança aparece, a segunda não, e o teste passa a afirmar sobre um estado velho. Foi
     * assim que estes três nasceram vermelhos.
     */
    private fun TestScope.mantemVivo(vm: WorkoutSessionViewModel) {
        backgroundScope.launch { vm.state.collect { } }
    }

    /**
     * **A decisão de abertura da 2.21.0, e a que custa uma tabela.** «Ombro a doer» é do dia,
     * e a `RoutineItemEntity` diz de si própria que é o plano — *«o que se fez de facto está
     * no `workout_set`»*. Uma nota do dia escrita na rotina mudava a instrução de todas as
     * semanas seguintes por causa de um ombro de terça-feira.
     */
    @Test
    fun `a nota e do treino de hoje e nao da rotina`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        db.routineDao().upsertRoutine(RoutineEntity("r1", "Empurrar", null, 0, 0L))
        db.routineDao().upsertItem(item("r1", "supino", 0))

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        vm.saveNote("supino", "ombro direito sensível")
        advanceUntilIdle()

        assertEquals("ombro direito sensível", vm.state.value.exercises.single().nota)

        // Presa ao treino, e não ao exercício da rotina: é o par sessão-exercício que a
        // identifica. Que a `RoutineItemEntity` não tem sítio nenhum onde a guardar é uma
        // verdade do compilador, e por isso não se afirma aqui — afirma-se onde ela ficou.
        val nota = db.sessionExerciseNoteDao().exportRows().single()
        assertEquals(vm.state.value.sessionId, nota.sessionId)
        assertEquals("supino", nota.exerciseId)
    }

    @Test
    fun `um treino novo comeca sem a nota do anterior`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        db.routineDao().upsertRoutine(RoutineEntity("r1", "Empurrar", null, 0, 0L))
        db.routineDao().upsertItem(item("r1", "supino", 0))

        val primeiro = viewModel()
        mantemVivo(primeiro)
        primeiro.ensureStarted("r1")
        advanceUntilIdle()
        primeiro.state.first { !it.loading && it.exercises.isNotEmpty() }
        primeiro.saveNote("supino", "máquina 2 ocupada")
        advanceUntilIdle()
        primeiro.finish()
        advanceUntilIdle()

        val segundo = viewModel()
        mantemVivo(segundo)
        segundo.ensureStarted("r1")
        advanceUntilIdle()
        val estado = segundo.state.first { !it.loading && it.exercises.isNotEmpty() }

        assertEquals("", estado.exercises.single().nota, "a nota de terça apareceu na quinta")
    }

    /** Uma linha que existe para dizer que não há nada é uma linha a mais na cópia. */
    @Test
    fun `apagar o texto da nota apaga a linha`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        db.routineDao().upsertRoutine(RoutineEntity("r1", "Empurrar", null, 0, 0L))
        db.routineDao().upsertItem(item("r1", "supino", 0))

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        vm.saveNote("supino", "qualquer coisa")
        advanceUntilIdle()
        vm.saveNote("supino", "   ")
        advanceUntilIdle()

        assertEquals("", vm.state.value.exercises.single().nota)
        assertTrue(
            db.sessionExerciseNoteDao().exportRows().isEmpty(),
            "ficou uma nota em branco guardada",
        )
    }

    /**
     * O recorde no momento em que acontece, e não só no resumo do fim. O que ele compara é o
     * **melhor de sempre**, e não o do treino anterior — é o `PrDetector` que a app já usava
     * no resumo, chamado no sítio onde a série se escreve.
     */
    @Test
    fun `o recorde aparece na serie que o bate, e nao antes`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        db.routineDao().upsertRoutine(RoutineEntity("r1", "Empurrar", null, 0, 0L))
        db.routineDao().upsertItem(item("r1", "supino", 0))
        sessaoAntiga("antiga", "supino", listOf(100.0 to 5))

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val antes = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        assertTrue(!antes.exercises.single().recordeHoje, "recorde antes de haver série nenhuma")

        vm.logSet(antes.exercises.single(), weightKg = 90.0, reps = 5, warmup = false)
        advanceUntilIdle()
        assertTrue(
            !vm.state.value.exercises.single().recordeHoje,
            "90 kg × 5 não bate 100 kg × 5, e o ecrã disse que sim",
        )

        vm.logSet(vm.state.value.exercises.single(), weightKg = 105.0, reps = 5, warmup = false)
        advanceUntilIdle()
        assertTrue(vm.state.value.exercises.single().recordeHoje, "105 kg × 5 é recorde e não foi dito")
    }

    /**
     * O 1RM à vista leva o de hoje dentro. É o número com que se decide o peso da série
     * seguinte, e essa decisão toma-se depois da série que se acabou de fazer.
     */
    @Test
    fun `o 1RM estimado sobe com a serie que se acaba de gravar`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        db.routineDao().upsertRoutine(RoutineEntity("r1", "Empurrar", null, 0, 0L))
        db.routineDao().upsertItem(item("r1", "supino", 0))
        sessaoAntiga("antiga", "supino", listOf(100.0 to 5))

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val antes = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        // Epley: 100 × (1 + 5/30) = 116,67
        assertEquals(116.67, antes.exercises.single().melhorOneRmKg!!, 0.01)

        vm.logSet(antes.exercises.single(), weightKg = 110.0, reps = 5, warmup = false)
        advanceUntilIdle()

        // 110 × (1 + 5/30) = 128,33
        assertEquals(128.33, vm.state.value.exercises.single().melhorOneRmKg!!, 0.01)
    }

    /**
     * O RPE deixou de ser um campo na linha de registo e passou a escrever-se depois. Uma
     * série nasce sem ele, e escrevê-lo não mexe no peso nem nas repetições — que é o que
     * distingue esta correção da do [WorkoutSessionViewModel.updateSet].
     */
    @Test
    fun `o RPE escreve-se depois da serie estar gravada`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        db.routineDao().upsertRoutine(RoutineEntity("r1", "Empurrar", null, 0, 0L))
        db.routineDao().upsertItem(item("r1", "supino", 0))

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }
        vm.logSet(estado.exercises.single(), weightKg = 60.0, reps = 8, warmup = false)
        advanceUntilIdle()

        val serie = vm.state.value.exercises.single().sets.single()
        assertEquals(null, serie.rpe, "a série nasceu com um RPE que ninguém escreveu")

        vm.updateRpe(serie, 8.0)
        advanceUntilIdle()

        val corrigida = vm.state.value.exercises.single().sets.single()
        assertEquals(8.0, corrigida.rpe)
        assertEquals(60.0, corrigida.weightKg, "escrever o RPE mexeu no peso")
        assertEquals(8, corrigida.reps, "escrever o RPE mexeu nas repetições")
    }

    /** O relógio da barra conta a partir do que ficou gravado, e não de quando o ecrã abriu. */
    @Test
    fun `o estado leva o instante em que o treino comecou e o nome da rotina`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        db.routineDao().upsertRoutine(RoutineEntity("r1", "Empurrar A", null, 0, 0L))
        db.routineDao().upsertItem(item("r1", "supino", 0))

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        assertNotNull(estado.startedAt, "sem o instante de início não há relógio nenhum")
        assertEquals(
            estado.startedAt,
            db.workoutSessionDao().exportRows().single().startedAt,
            "o relógio contava de um instante que não é o do treino",
        )
        assertEquals("Empurrar A", estado.routineName)
    }
    /**
     * **Uma supersérie abre os dois exercícios ao mesmo tempo.** É o que ela é: alternar entre
     * eles sem descanso pelo meio. Até à 2.23.1 só o exercício aberto podia registar, e o ecrã
     * trocava a seleção sozinho quando as séries acabavam — numa supersérie isso lutava com
     * quem a faz, série sim série não. É o defeito 4 da área 08.
     */
    @Test
    fun `uma superserie abre os exercicios todos do grupo`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        exercicio("remada", pt = "Remada")
        exercicio("agachamento", pt = "Agachamento")
        rotina(
            "r1",
            item("r1", "supino", position = 0, supersetGroup = 1),
            item("r1", "remada", position = 1, supersetGroup = 1),
            item("r1", "agachamento", position = 2),
        )

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        assertEquals(setOf("supino", "remada"), estado.abertos)
    }

    /** Fora de um grupo, o conjunto é o de sempre: um exercício de cada vez. */
    @Test
    fun `sem superserie so um exercicio fica aberto`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        exercicio("remada", pt = "Remada")
        rotina("r1", item("r1", "supino", position = 0), item("r1", "remada", position = 1))

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        val estado = vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        assertEquals(setOf("supino"), estado.abertos)
    }

    /** Escolher um exercício de outro grupo muda o conjunto inteiro, e não só o escolhido. */
    @Test
    fun `escolher um exercicio de outro grupo abre o grupo dele`() = runTest(dispatcher) {
        exercicio("supino", pt = "Supino")
        exercicio("remada", pt = "Remada")
        exercicio("biceps", pt = "Bíceps")
        exercicio("triceps", pt = "Tríceps")
        rotina(
            "r1",
            item("r1", "supino", position = 0, supersetGroup = 1),
            item("r1", "remada", position = 1, supersetGroup = 1),
            item("r1", "biceps", position = 2, supersetGroup = 2),
            item("r1", "triceps", position = 3, supersetGroup = 2),
        )

        val vm = viewModel()
        mantemVivo(vm)
        vm.ensureStarted("r1")
        advanceUntilIdle()
        vm.state.first { !it.loading && it.exercises.isNotEmpty() }

        vm.select("biceps")
        advanceUntilIdle()

        assertEquals(setOf("biceps", "triceps"), vm.state.value.abertos)
    }
}
