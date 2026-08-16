package pt.antares.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.designsystem.ProvedorDaJanela
import pt.antares.app.core.designsystem.components.GrelhaDeCartoes
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O Hoje é uma pilha de cartões. Num tablet, essa pilha ao meio do ecrã deixa metade da
 * janela vazia e obriga a percorrer o que cabia à vista.
 *
 * A grelha alterna os cartões — o primeiro à esquerda, o segundo à direita — e é isso que
 * este teste guarda: com duas colunas, o primeiro e o segundo ficam à mesma altura, e o
 * terceiro volta ao lado do primeiro.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class GrelhaDeCartoesUiTest {

    private val cabecalhoTexto = "cabecalho"

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `numa janela larga os cartoes alternam entre duas colunas`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                GrelhaDeCartoes(
                    modifier = Modifier.fillMaxSize(),
                    cabecalho = { Text(cabecalhoTexto) },
                ) {
                    cartao { Text("um") }
                    cartao { Text("dois") }
                    cartao { Text("tres") }
                }
            }
        }

        val um = onNodeWithText("um").getUnclippedBoundsInRoot()
        val dois = onNodeWithText("dois").getUnclippedBoundsInRoot()
        val tres = onNodeWithText("tres").getUnclippedBoundsInRoot()

        assertEquals(um.top, dois.top, "o segundo cartão não subiu para a segunda coluna")
        assertTrue(dois.left > um.left, "o segundo cartão ficou na mesma coluna do primeiro")
        assertEquals(um.left, tres.left, "o terceiro não voltou à primeira coluna")
        assertTrue(tres.top > um.top, "o terceiro ficou ao lado do primeiro em vez de por baixo")

        // O cabeçalho fica por cima dos dois: é a barra de registo rápido, e numa coluna de
        // metade do ecrã ficava com meio campo de texto.
        val cabecalho = onNodeWithText(cabecalhoTexto).getUnclippedBoundsInRoot()
        assertTrue(cabecalho.top < um.top, "o cabeçalho não ficou por cima dos cartões")
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `num telemovel os cartoes ficam empilhados`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                GrelhaDeCartoes(modifier = Modifier.fillMaxSize()) {
                    cartao { Text("um") }
                    cartao { Text("dois") }
                }
            }
        }

        val um = onNodeWithText("um").getUnclippedBoundsInRoot()
        val dois = onNodeWithText("dois").getUnclippedBoundsInRoot()
        assertEquals(um.left, dois.left)
        assertTrue(dois.top > um.top)
    }
}
