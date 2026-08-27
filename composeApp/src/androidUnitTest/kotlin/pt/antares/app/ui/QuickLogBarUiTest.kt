package pt.antares.app.ui

import android.content.ContentProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.feature.fooddata.QuickLogBar
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.quick_log_hint
import pt.antares.app.generated.resources.quick_log_photo
import pt.antares.app.generated.resources.quick_log_scan
import pt.antares.app.generated.resources.quick_log_voice
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)

@Config(application = android.app.Application::class)
class QuickLogBarUiTest {

    @Before
    fun arrancaOsRecursos() {

        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()
    }

    @Test
    fun `escrever e confirmar entrega o texto e limpa a caixa`() = runComposeUiTest {
        var submetido: String? = null
        val textos = Textos()
        setContent {
            textos.ler()
            QuickLogBar(onSubmit = { submetido = it }, onVoice = {}, onPhoto = {}, onScan = {})
        }

        onNodeWithText(textos.hint).performTextInput("aveia")
        onNodeWithText("aveia").performImeAction()

        assertEquals("aveia", submetido)

        onNodeWithText(textos.hint).assertIsDisplayed()
    }

    @Test
    fun `espacos em branco nao contam como registo`() = runComposeUiTest {
        var chamadas = 0
        val textos = Textos()
        setContent {
            textos.ler()
            QuickLogBar(onSubmit = { chamadas++ }, onVoice = {}, onPhoto = {}, onScan = {})
        }

        onNodeWithText(textos.hint).performTextInput("   ")
        onNodeWithText("   ").performImeAction()

        assertEquals(0, chamadas)
    }

    @Test
    fun `a foto e o codigo de barras tem alvos proprios`() = runComposeUiTest {
        var foto = 0
        var codigo = 0
        val textos = Textos()
        setContent {
            textos.ler()
            QuickLogBar(onSubmit = {}, onVoice = {}, onPhoto = { foto++ }, onScan = { codigo++ })
        }

        onNodeWithContentDescription(textos.foto).performClick()
        onNodeWithContentDescription(textos.codigo).performClick()

        assertEquals(1, foto)
        assertEquals(1, codigo)
    }

    @Test
    fun `o microfone nao aparece onde nao ha reconhecimento de voz`() = runComposeUiTest {

        val textos = Textos()
        setContent {
            textos.ler()
            QuickLogBar(onSubmit = {}, onVoice = {}, onPhoto = {}, onScan = {})
        }

        val microfones = onAllNodes(hasContentDescription(textos.voz)).fetchSemanticsNodes().size
        assertEquals(0, microfones)
    }

    private class Textos {
        var hint: String = ""
        var foto: String = ""
        var codigo: String = ""
        var voz: String = ""

        @androidx.compose.runtime.Composable
        fun ler() {
            hint = leitura(Res.string.quick_log_hint)
            foto = leitura(Res.string.quick_log_photo)
            codigo = leitura(Res.string.quick_log_scan)
            voz = leitura(Res.string.quick_log_voice)
        }

        @androidx.compose.runtime.Composable
        private fun leitura(res: StringResource): String = stringResource(res)
    }
}
