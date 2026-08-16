package pt.antares.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.designsystem.ProvedorDaJanela
import pt.antares.app.core.designsystem.components.PainelDeListaEDetalhe
import pt.antares.app.core.designsystem.components.cabeDetalheAoLado
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O terceiro modo do plano: lista à esquerda, detalhe à direita. Resolve o percurso mais
 * cansativo de um tablet — abrir um exercício, voltar atrás, abrir o seguinte — quando havia
 * espaço para os ver sem sair da lista.
 *
 * Numa janela compacta os dois não cabem, e o [cabeDetalheAoLado] tem de dizer que não: 360
 * dp partidos ao meio não servem nem para a lista nem para o detalhe.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ListaEDetalheUiTest {

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `numa janela larga a lista e o detalhe ficam lado a lado`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                PainelDeListaEDetalhe(
                    modifier = Modifier.fillMaxSize(),
                    lista = { Text("lista") },
                    detalhe = { Text("detalhe") },
                )
            }
        }

        val lista = onNodeWithText("lista").getUnclippedBoundsInRoot()
        val detalhe = onNodeWithText("detalhe").getUnclippedBoundsInRoot()

        onNodeWithText("lista").assertIsDisplayed()
        onNodeWithText("detalhe").assertIsDisplayed()
        assertTrue(detalhe.left > lista.left, "o detalhe não ficou à direita da lista")
        assertTrue(detalhe.top == lista.top, "o detalhe ficou por baixo em vez de ao lado")
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `sem nada escolhido o lado direito diz o que fazer`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                PainelDeListaEDetalhe(
                    modifier = Modifier.fillMaxSize(),
                    lista = { Text("lista") },
                    detalhe = null,
                    vazio = { Text("escolhe um da lista") },
                )
            }
        }

        // Um painel branco à direita parece a app avariada; o texto diz que falta escolher.
        onNodeWithText("escolhe um da lista").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `num telemovel o detalhe continua a ser um ecra a parte`() = runComposeUiTest {
        var cabe = true
        setContent {
            ProvedorDaJanela { cabe = cabeDetalheAoLado() }
        }

        assertFalse(cabe, "um telemóvel de 360 dp não tem espaço para os dois")
    }

    @Test
    @Config(qualifiers = "w840dp-h800dp")
    fun `a partir da janela media ja cabem os dois`() = runComposeUiTest {
        var cabe = false
        setContent {
            ProvedorDaJanela { cabe = cabeDetalheAoLado() }
        }

        assertTrue(cabe)
    }
}
