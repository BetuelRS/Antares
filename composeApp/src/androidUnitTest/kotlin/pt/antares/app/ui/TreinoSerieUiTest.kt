package pt.antares.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.RoutineEntity
import pt.antares.app.core.database.entities.RoutineItemEntity
import pt.antares.app.feature.workout.NoopWorkoutAlerts
import pt.antares.app.feature.workout.data.SessionPickBus
import pt.antares.app.feature.workout.ui.WorkoutSessionScreen
import pt.antares.app.feature.workout.ui.WorkoutSessionViewModel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.session_add_set
import pt.antares.app.generated.resources.session_reps
import pt.antares.app.generated.resources.session_rest_skip
import pt.antares.app.testing.Fabricas
import pt.antares.app.testing.FluxoUiHarness
import kotlin.test.assertEquals

/**
 * Gravar uma série é o gesto que se repete dezenas de vezes num treino, muitas vezes com o
 * telemóvel numa mão. Duas coisas têm de acontecer ao mesmo tempo: a série fica na base, e
 * o descanso arranca sozinho — quem tem de o carregar à mão deixa de o usar.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
// A janela por omissão do Robolectric tem 320 dp, e a linha de gravar a série não cabe
// nela: o botão de confirmar fica com zero de largura e deixa de ser tocável. Aqui usa-se
// a largura de um telemóvel corrente, que é onde a app corre.
@Config(application = android.app.Application::class, qualifiers = "w411dp-h891dp")
class TreinoSerieUiTest : FluxoUiHarness() {

    private fun rotinaComUmExercicio() = runBlocking {
        db.exerciseLibraryDao().upsert(
            ExerciseEntity(
                id = EXERCICIO_ID,
                nameEn = "Bench Press",
                namePt = "Supino",
                searchText = "bench press supino",
                category = "strength",
                force = null,
                mechanic = null,
                equipment = "barbell",
                level = "intermediate",
                primaryMuscles = "|chest|",
                secondaryMuscles = "",
                instructionsEnJson = "[]",
                instructionsPtJson = "[]",
                imagesJson = "[]",
                updatedAt = 1_000,
            ),
        )
        db.routineDao().upsertRoutine(
            RoutineEntity(id = ROTINA_ID, name = "Peito", note = null, position = 0, updatedAt = 1_000),
        )
        db.routineDao().upsertItem(
            RoutineItemEntity(
                id = "item-1",
                routineId = ROTINA_ID,
                exerciseId = EXERCICIO_ID,
                targetSets = 3,
                targetRepsMin = 6,
                targetRepsMax = 10,
                targetWeightKg = 60.0,
                restSec = DESCANSO_SEG,
                position = 0,
                supersetGroup = null,
                updatedAt = 1_000,
            ),
        )
    }

    @Test
    fun `gravar uma serie guarda-a e poe o descanso a andar`() = runComposeUiTest {
        arrancaKoin()
        rotinaComUmExercicio()

        val vm = WorkoutSessionViewModel(
            repository = Fabricas.workoutSessionRepository(db, io),
            routineDao = db.routineDao(),
            exerciseDao = db.exerciseLibraryDao(),
            alerts = NoopWorkoutAlerts(),
            pickBus = SessionPickBus(),
        )
        val textos = Textos()

        setContent {
            textos.ler(
                Res.string.session_reps,
                Res.string.session_add_set,
                Res.string.session_rest_skip,
            )
            WorkoutSessionScreen(
                routineId = ROTINA_ID,
                onAddExercise = {},
                onFinished = {},
                onDiscarded = {},
                viewModel = vm,
            )
        }

        // As caixas procuram-se pela ação de escrever, e não pelo rótulo: o rótulo de uma
        // caixa de texto é um nó ao lado dela, e escrever nele não escreve no campo.
        waitUntil("o exercício da rotina nunca apareceu no ecrã", ESPERA_MS) {
            onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= CAMPOS_DA_SERIE
        }

        val campos = onAllNodes(hasSetTextAction())
        campos[0].performTextInput("60")
        campos[1].performTextInput("8")
        waitForIdle()
        onNodeWithContentDescription(textos[Res.string.session_add_set]).performClick()

        // O descanso é uma contagem: o que interessa não é o número, que muda enquanto se
        // olha, mas o botão de o saltar — que só existe com o descanso a andar.
        waitUntil("gravar a série não arrancou o descanso", ESPERA_MS) {
            onAllNodesWithText(textos[Res.string.session_rest_skip]).fetchSemanticsNodes().isNotEmpty()
        }

        val series = runBlocking {
            val sessao = db.workoutSessionDao().exportRows().single()
            db.workoutSetDao().exportRows().filter { it.sessionId == sessao.id }
        }
        assertEquals(1, series.size, "a série não ficou na base")
        assertEquals(60.0, series.single().weightKg)
        assertEquals(8, series.single().reps)
    }

    private companion object {
        const val EXERCICIO_ID = "ex-supino"
        const val ROTINA_ID = "rot-peito"
        const val DESCANSO_SEG = 90

        // Peso, repetições e RPE, por esta ordem, na linha de gravar a série.
        const val CAMPOS_DA_SERIE = 3

        // Uma escrita na base em memória não demora nada; isto é o teto antes de desistir.
        const val ESPERA_MS = 5_000L
    }
}
