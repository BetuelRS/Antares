package pt.antares.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.designsystem.components.LinhaDaLista
import kotlin.test.assertEquals

/**
 * A linha da lista responde a toque **nas duas formas**, com cartão e sem.
 *
 * Sem cartão não respondia: o `onClick` era aceite pela assinatura e deitado fora ao
 * desenhar. Nada dava erro e nada mudava de aspecto — a linha simplesmente não fazia nada,
 * e só apareceu a tocar-lhe no emulador, na lista de alimentos que troca um item da AI.
 *
 * Um teste de estado não podia ver isto, e é por isso que este é de interface.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class LinhaDaListaUiTest {

    @Test
    fun `sem cartao, a linha continua a responder ao toque`() = runComposeUiTest {
        var toques = 0
        setContent {
            LinhaDaLista(titulo = "Arroz branco cozido", onClick = { toques++ }, emCartao = false)
        }

        onNodeWithText("Arroz branco cozido").performClick()

        assertEquals(1, toques, "a linha sem cartão engoliu o clique")
    }

    @Test
    fun `com cartao, tambem`() = runComposeUiTest {
        var toques = 0
        setContent {
            LinhaDaLista(titulo = "Arroz integral", onClick = { toques++ })
        }

        onNodeWithText("Arroz integral").performClick()

        assertEquals(1, toques)
    }

    /** Uma linha sem destino não finge que se toca — é o caso das linhas com interruptor. */
    @Test
    fun `sem onClick nao ha nada para tocar`() = runComposeUiTest {
        setContent { LinhaDaLista(titulo = "Notificações", subtitulo = "ligado", emCartao = false) }

        // Não rebenta: o que se protege é que a linha continua a desenhar-se sem destino.
        onNodeWithText("Notificações").performClick()
    }
}
