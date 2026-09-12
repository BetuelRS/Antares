package pt.antares.app.core.designsystem.components

import android.content.ContentProvider
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * O apagar por deslizar do diário.
 *
 * **Este gesto não se prova por `adb`** — nenhuma das formas de o injectar reproduz um
 * deslizar a sério, a mesma lição do `ListaArrastavelUiTest` — e por isso o gesto é
 * construído aqui, passo a passo.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class DeslizarParaApagarUiTest {

    // O ícone de apagar do fundo lê `common_delete` por `stringResource`, e isso só
    // funciona com o `ContentProvider` dos recursos gerados já arrancado.
    @Before
    fun arrancaOsRecursos() {
        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()
    }

    @Test
    fun `deslizar para a esquerda chama o apagar`() = runComposeUiTest {
        var vezes = 0
        setContent {
            DeslizarParaApagar(onApagar = { vezes++ }) {
                Text("linha", modifier = Modifier.fillMaxWidth().height(60.dp))
            }
        }
        waitForIdle()

        onNodeWithText("linha").performTouchInput { swipeLeft() }
        waitForIdle()

        assertEquals(1, vezes, "o deslizar para a esquerda não chamou o apagar")
    }

    @Test
    fun `deslizar para a direita tambem chama o apagar`() = runComposeUiTest {
        var vezes = 0
        setContent {
            DeslizarParaApagar(onApagar = { vezes++ }) {
                Text("linha", modifier = Modifier.fillMaxWidth().height(60.dp))
            }
        }
        waitForIdle()

        onNodeWithText("linha").performTouchInput { swipeRight() }
        waitForIdle()

        assertEquals(1, vezes, "o deslizar para a direita não chamou o apagar")
    }
}
