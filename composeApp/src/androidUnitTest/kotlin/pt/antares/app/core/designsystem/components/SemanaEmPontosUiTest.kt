package pt.antares.app.core.designsystem.components

import android.content.ContentProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
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
 * treinador e a grelha do Progresso, que só mostram a semana, não podem ganhar um toque
 * que não faz nada.
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
}
