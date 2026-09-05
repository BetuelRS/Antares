package pt.antares.app.feature.workout

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.WorkoutSessionEntity
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.SessionStatus
import pt.antares.app.feature.workout.data.WorkoutSessionRepository
import pt.antares.app.feature.workout.ui.WorkoutSummaryScreen
import pt.antares.app.feature.workout.ui.WorkoutSummaryViewModel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.workout_summary_prs
import pt.antares.app.generated.resources.workout_summary_sem_rotina
import pt.antares.app.generated.resources.workout_summary_vs_ultima
import pt.antares.app.testing.FluxoUiHarness
import kotlin.test.Test

/**
 * O resumo pós-treino, composto.
 *
 * Duas destas afirmações são sobre coisas que **não estão** no ecrã, e nenhum teste de estado
 * as via: a secção dos recordes deixou de existir quando não há nenhum — dizer «sem recordes»
 * a seguir a cada treino normal transforma a ausência num facto negativo repetido —, e a
 * comparação não aparece num treino livre, onde o que aparece é a razão.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = "w411dp-h891dp")
class ResumoDoTreinoUiTest : FluxoUiHarness() {

    private fun viewModel() = WorkoutSummaryViewModel(
        sessionRepository = WorkoutSessionRepository(
            db.workoutSessionDao(),
            db.workoutSetDao(),
            db.exerciseLogDao(),
            db.weightLogDao(),
            db.routineDao(),
            db.sessionExerciseNoteDao(),
            db.exerciseLoadDao(),
            io,
        ),
        exerciseDao = db.exerciseLibraryDao(),
    )

    private fun montar(comRotina: Boolean, comAnterior: Boolean) = runBlocking {
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = "supino", nameEn = "Bench", namePt = "Supino", searchText = "supino",
                category = "strength", force = null, mechanic = null,
                equipment = "barbell", level = "beginner",
                primaryMuscles = "|chest|", secondaryMuscles = "",
                instructionsEnJson = "[]", instructionsPtJson = "[]", imagesJson = "[]",
                updatedAt = 1L,
            ),
        )
        if (comRotina) {
            db.routineDao().upsertRoutine(
                RoutineEntity(id = "r1", name = "Empurrar A", note = null, position = 0, updatedAt = 1L),
            )
        }
        if (comAnterior) treino("antes", if (comRotina) "r1" else null, 1_000L)
        treino("hoje", if (comRotina) "r1" else null, 100_000L)
    }

    private suspend fun treino(id: String, rotinaId: String?, inicio: Long) {
        db.workoutSessionDao().upsertSession(
            WorkoutSessionEntity(
                id = id, startedAt = inicio, endedAt = inicio + 60_000L * 40,
                routineId = rotinaId, note = null, status = SessionStatus.DONE, updatedAt = inicio,
            ),
        )
        db.workoutSetDao().upsertSet(
            WorkoutSetEntity(
                id = "$id-s", sessionId = id, exerciseId = "supino", setIndex = 0,
                weightKg = 60.0, reps = 10, rpe = null, isWarmup = false, updatedAt = 1L,
            ),
        )
    }

    private fun carregado(sessionId: String) = viewModel().also { vm ->
        vm.load(sessionId)
        runBlocking { vm.state.first { !it.loading } }
    }

    /**
     * O primeiro treino de sempre bate um recorde — é o que o `PrDetector` documenta —, e por
     * isso é o **segundo** que serve para afirmar que a secção desaparece: aí o supino já tem
     * um melhor anterior igual, e não há recorde nenhum a mostrar.
     */
    @Test
    fun `sem recordes a seccao nao aparece`() = runComposeUiTest {
        arrancaKoin()
        montar(comRotina = true, comAnterior = true)

        val textos = Textos()
        val vm = carregado("hoje")
        setContent {
            WorkoutSummaryScreen(sessionId = "hoje", onDone = {}, viewModel = vm)
            textos.ler(Res.string.workout_summary_prs)
            textos.ler(Res.string.workout_summary_vs_ultima)
        }
        waitForIdle()

        onNodeWithText(textos[Res.string.workout_summary_prs]).assertDoesNotExist()
        // E a comparação está lá: sem esta linha, um ecrã em branco passava no teste de cima.
        onNodeWithText(textos[Res.string.workout_summary_vs_ultima]).assertExists()
    }

    @Test
    fun `com um recorde a seccao aparece`() = runComposeUiTest {
        arrancaKoin()
        montar(comRotina = true, comAnterior = false)

        val textos = Textos()
        val vm = carregado("hoje")
        setContent {
            WorkoutSummaryScreen(sessionId = "hoje", onDone = {}, viewModel = vm)
            textos.ler(Res.string.workout_summary_prs)
        }
        waitForIdle()

        onNodeWithText(textos[Res.string.workout_summary_prs]).assertExists()
    }

    /**
     * Dois treinos iguais dão três diferenças a zero, e o que se lê é **`=`** — sozinho, sem o
     * número. Escrito «= 0 série», que foi como saiu no aparelho, o zero não acrescenta nada e
     * o plural do português dá-lhe o singular.
     */
    @Test
    fun `sem diferenca nenhuma o ecra escreve um igual e nao um zero`() = runComposeUiTest {
        arrancaKoin()
        montar(comRotina = true, comAnterior = true)

        val vm = carregado("hoje")
        setContent { WorkoutSummaryScreen(sessionId = "hoje", onDone = {}, viewModel = vm) }
        waitForIdle()

        // Três métricas, três diferenças a zero.
        onAllNodesWithText("=").assertCountEquals(3)
    }

    /** Um treino livre não compara — e diz porquê, em vez de ficar calado. */
    @Test
    fun `um treino livre diz porque e que nao compara`() = runComposeUiTest {
        arrancaKoin()
        montar(comRotina = false, comAnterior = true)

        val textos = Textos()
        val vm = carregado("hoje")
        setContent {
            WorkoutSummaryScreen(sessionId = "hoje", onDone = {}, viewModel = vm)
            textos.ler(Res.string.workout_summary_sem_rotina)
            textos.ler(Res.string.workout_summary_vs_ultima)
        }
        waitForIdle()

        onNodeWithText(textos[Res.string.workout_summary_sem_rotina]).assertExists()
        onNodeWithText(textos[Res.string.workout_summary_vs_ultima]).assertDoesNotExist()
    }
}
