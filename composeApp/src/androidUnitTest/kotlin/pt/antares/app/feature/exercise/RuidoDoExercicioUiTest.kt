package pt.antares.app.feature.exercise

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.ai.AiClient
import pt.antares.app.core.ai.AiRepository
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.exercise_cat_all
import pt.antares.app.generated.resources.exercise_cat_daily
import pt.antares.app.generated.resources.exercise_cat_walking
import pt.antares.app.testing.Fabricas
import pt.antares.app.testing.FluxoUiHarness

/**
 * As duas coisas que a `estudo/areas/13-exercicio-avulso.md` chama inúteis: o chip «Todas»
 * sempre à vista, sem nunca ter nada para desmarcar; e o `MET` repetido em cada linha da
 * lista, quando só é útil depois de a atividade estar escolhida.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = "w411dp-h891dp")
class RuidoDoExercicioUiTest : FluxoUiHarness() {

    private fun viewModel() = vivo(
        AddExerciseViewModel(
            repository = ExerciseRepository(db.exerciseLogDao(), io),
            profileRepository = Fabricas.profileRepository(db, io),
            ai = AiRepository(
                client = NenhumaIa,
                ensureAccount = {},
                saveFoodLog = {},
                latestWeightKg = { null },
                persistUsage = { _, _ -> },
                io = Dispatchers.Unconfined,
            ),
        ),
    )

    /**
     * O `AddExerciseViewModel` carrega o catálogo do `seed_mets.csv` num `withContext(io)`, e
     * mesmo com o `Unconfined` isso não fica pronto antes do primeiro `setContent`: o chip de
     * uma categoria só compõe depois de `state.categories` chegar.
     */
    private fun carregado(): AddExerciseViewModel = viewModel().also { vm ->
        runBlocking { vm.state.first { !it.loading } }
    }

    @Test
    fun `o chip Todas nao aparece sem categoria escolhida`() = runComposeUiTest {
        val vm = carregado()
        val textos = Textos()
        setContent {
            textos.ler(Res.string.exercise_cat_all)
            AddExerciseScreen(epochDay = 100, onDone = {}, onBack = {}, viewModel = vm)
        }
        waitForIdle()

        onNodeWithText(textos[Res.string.exercise_cat_all]).assertDoesNotExist()
    }

    @Test
    fun `o chip Todas aparece depois de escolher uma categoria`() = runComposeUiTest {
        val vm = carregado()
        val textos = Textos()
        setContent {
            textos.ler(Res.string.exercise_cat_all, Res.string.exercise_cat_walking)
            AddExerciseScreen(epochDay = 100, onDone = {}, onBack = {}, viewModel = vm)
        }
        waitForIdle()

        onNodeWithText(textos[Res.string.exercise_cat_walking]).performClick()
        waitForIdle()

        onNodeWithText(textos[Res.string.exercise_cat_all]).assertExists()
    }

    /**
     * O `scrollToItem(0)` que traz o «Todas» à vista não pode levar consigo o chip que se acabou
     * de tocar: é ele que diz qual categoria filtra a lista. A janela é estreita de propósito — o
     * Robolectric mede o texto a poucos pixels, e a 411 dp os nove chips cabiam todos e o teste
     * não conseguia falhar.
     */
    @Test
    @Config(qualifiers = "w200dp-h891dp")
    fun `a categoria escolhida fica a vista mesmo quando e a ultima da fila`() = runComposeUiTest {
        val vm = carregado()
        val textos = Textos()
        setContent {
            textos.ler(Res.string.exercise_cat_daily)
            AddExerciseScreen(epochDay = 100, onDone = {}, onBack = {}, viewModel = vm)
        }
        waitForIdle()

        // A fila acha-se pelo eixo — é a única lista horizontal do ecrã. Por um chip não dá: o
        // último ainda não foi composto, e o primeiro sai da árvore a meio da rolagem.
        onNode(hasScrollToNodeAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
            .performScrollToNode(hasText(textos[Res.string.exercise_cat_daily]))
        onNodeWithText(textos[Res.string.exercise_cat_daily]).performClick()
        waitForIdle()

        onNodeWithText(textos[Res.string.exercise_cat_daily]).assertIsDisplayed()
    }

    @Test
    fun `o MET nao aparece na lista antes de escolher a atividade`() = runComposeUiTest {
        val vm = carregado()
        setContent {
            AddExerciseScreen(epochDay = 100, onDone = {}, onBack = {}, viewModel = vm)
        }
        waitForIdle()

        onNode(hasSetTextAction()).performTextInput("Caminhada lenta")
        waitForIdle()

        onNodeWithText("Caminhada lenta (3 km/h)").assertExists()
        onNodeWithText("MET 2.8").assertDoesNotExist()
    }

    @Test
    fun `o MET aparece no cartao depois de escolher a atividade`() = runComposeUiTest {
        val vm = carregado()
        setContent {
            AddExerciseScreen(epochDay = 100, onDone = {}, onBack = {}, viewModel = vm)
        }
        waitForIdle()

        onNode(hasSetTextAction()).performTextInput("Caminhada lenta")
        waitForIdle()
        onNodeWithText("Caminhada lenta (3 km/h)").performClick()
        waitForIdle()

        onNodeWithText("MET 2.8").assertExists()
    }

    private object NenhumaIa : AiClient {
        override suspend fun analyzeFoodText(text: String, lang: String, day: String) =
            error("não usado")

        override suspend fun analyzeFoodPhoto(imageBase64: String, mime: String, lang: String, day: String) =
            error("não usado")

        override suspend fun readLabel(imageBase64: String, mime: String, lang: String, day: String) =
            error("não usado")

        override suspend fun analyzeExercise(text: String, weightKg: Double, lang: String, day: String) =
            error("não usado")
    }
}
