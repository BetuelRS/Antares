package pt.antares.app.core.designsystem.components

import android.content.ContentProvider
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.unit.dp
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * O toque num dia da semana. Nasceu para o diário, que salta para o dia tocado — e o
 * treinador e o centro de treino, que só mostram a semana, não podem ganhar um toque que não
 * faz nada.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class SemanaEmPontosUiTest {

    // Arbitrário: o teste não depende de que dia da semana isto é.
    private val inicio = 19_000L

    // O `SemanaEmPontos` lê o nome do dia por `stringResource`, e isso só funciona com o
    // `ContentProvider` dos recursos gerados já arrancado — molde do `FluxoUiHarness`.
    @Before
    fun arrancaOsRecursos() {
        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()
    }

    @Test
    fun `tocar no terceiro dia devolve o terceiro dia`() = runComposeUiTest {
        var tocado: Long? = null
        setContent {
            SemanaEmPontos(
                inicioEpochDay = inicio,
                diasMarcados = emptyList(),
                onDiaClick = { tocado = it },
            )
        }
        waitForIdle()

        onAllNodes(hasClickAction())[2].performClick()
        waitForIdle()

        assertEquals(inicio + 2, tocado)
    }

    @Test
    fun `sem onDiaClick nao ha nada tocavel`() = runComposeUiTest {
        setContent {
            SemanaEmPontos(inicioEpochDay = inicio, diasMarcados = emptyList())
        }
        waitForIdle()

        assertEquals(0, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    /**
     * No diário os dias tocam-se, e um dia é um alvo de dedo: 48 dp, e não os 28 do quadrado,
     * encostados uns aos outros à esquerda da linha. E o leitor de ecrã ouvia sete dias iguais —
     * que dias têm registo dizia-se só pela cor, e o dia aberto não se dizia de todo.
     */
    @Test
    fun `no diario cada dia e um alvo de 48 dp e diz se tem registo e se esta aberto`() = runComposeUiTest {
        setContent {
            SemanaEmPontos(
                inicioEpochDay = inicio,
                diasMarcados = listOf(inicio + 1),
                aberto = inicio + 1,
                onDiaClick = {},
                // A largura do diário num telemóvel de 411 dp, menos as margens de 16 dp: a
                // janela do Robolectric é mais estreita do que qualquer telemóvel de hoje.
                modifier = Modifier.requiredWidth(379.dp),
            )
        }
        waitForIdle()

        val dias = onAllNodes(hasClickAction())
        dias[0].assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)

        val vazio = dias[0].fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        val registado = dias[1].fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        assertNotNull(vazio, "um dia sem registo não diz o estado ao leitor de ecrã")
        assertNotEquals(vazio, registado, "um dia com registo e um sem dizem o mesmo")

        dias[1].assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        dias[0].assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }
}
