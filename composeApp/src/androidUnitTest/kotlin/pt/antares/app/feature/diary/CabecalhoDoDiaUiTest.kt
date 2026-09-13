package pt.antares.app.feature.diary

import android.content.ContentProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.diary_back_to_today
import pt.antares.app.generated.resources.diary_day_menu
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * O caminho de volta a hoje, a partir de qualquer dia.
 *
 * O ícone do calendário era o botão «hoje» e passou a abrir um calendário, que era o que devia.
 * O atalho foi dado por passado para a tira de semana — e a tira mostra a semana do dia aberto:
 * de há três semanas, hoje não está lá, e voltar custava o calendário, mudar de mês, escolher e
 * guardar.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class CabecalhoDoDiaUiTest {

    // O cabeçalho lê as cadeias por `stringResource`, e isso só funciona com o `ContentProvider`
    // dos recursos gerados já arrancado — molde do `FluxoUiHarness`.
    @Before
    fun arrancaOsRecursos() {
        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()
    }

    private fun texto(recurso: org.jetbrains.compose.resources.StringResource) =
        runBlocking { getString(recurso) }

    @Test
    fun `fora de hoje o menu do dia leva de volta a hoje`() = runComposeUiTest {
        var voltou = false
        setContent {
            DayHeader(
                isToday = false,
                epochDay = 20_000,
                onPrevious = {},
                onNext = {},
                onPickDate = {},
                onToday = { voltou = true },
                onCopyDay = {},
                onSearch = {},
            )
        }
        waitForIdle()

        onNodeWithContentDescription(texto(Res.string.diary_day_menu)).performClick()
        waitForIdle()
        onNodeWithText(texto(Res.string.diary_back_to_today)).performClick()
        waitForIdle()

        assertTrue(voltou, "o «voltar a hoje» do menu do dia não levou a hoje")
    }

    @Test
    fun `em hoje o menu nao oferece voltar a hoje`() = runComposeUiTest {
        setContent {
            DayHeader(
                isToday = true,
                epochDay = 20_000,
                onPrevious = {},
                onNext = {},
                onPickDate = {},
                onToday = {},
                onCopyDay = {},
                onSearch = {},
            )
        }
        waitForIdle()

        onNodeWithContentDescription(texto(Res.string.diary_day_menu)).performClick()
        waitForIdle()
        onNodeWithText(texto(Res.string.diary_back_to_today)).assertDoesNotExist()
    }
}
