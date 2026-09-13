package pt.antares.app.feature.diary

import android.content.ContentProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.diary_search_empty
import kotlin.test.Test

/**
 * O diálogo da pesquisa no diário, entre a tecla e a resposta.
 *
 * A pesquisa espera 300 ms antes de ir à base, e o diálogo decidia pelo campo e pela lista, que
 * não são do mesmo instante: à primeira letra, o campo já não estava vazio e a lista ainda era a
 * de antes. Dizia «Nada encontrado» antes de ter perguntado — visto a falhar sobre essa forma.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class PesquisaDoDiarioUiTest {

    // O diálogo lê as cadeias por `stringResource`, e isso só funciona com o `ContentProvider`
    // dos recursos gerados já arrancado — molde do `FluxoUiHarness`.
    @Before
    fun arrancaOsRecursos() {
        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()
    }

    private fun nadaEncontrado() = runBlocking { getString(Res.string.diary_search_empty) }

    @Test
    fun `antes de a pesquisa responder ao termo nao diz nada encontrado`() = runComposeUiTest {
        setContent {
            DiarySearchDialog(
                resultado = ResultadoDaPesquisa("", emptyList()),
                onQueryChange = {},
                onPick = {},
                onDismiss = {},
            )
        }
        waitForIdle()

        onNode(hasSetTextAction()).performTextInput("a")
        waitForIdle()

        onNodeWithText(nadaEncontrado()).assertDoesNotExist()
    }

    @Test
    fun `quando a pesquisa responde ao termo sem nada, diz nada encontrado`() = runComposeUiTest {
        var resultado by mutableStateOf(ResultadoDaPesquisa("", emptyList()))
        setContent {
            DiarySearchDialog(resultado = resultado, onQueryChange = {}, onPick = {}, onDismiss = {})
        }
        waitForIdle()

        onNode(hasSetTextAction()).performTextInput("a")
        resultado = ResultadoDaPesquisa("a", emptyList())
        waitForIdle()

        onNodeWithText(nadaEncontrado()).assertExists()
    }
}
