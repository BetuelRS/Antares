package pt.antares.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.designsystem.LARGURA_DE_LEITURA_DP
import pt.antares.app.core.designsystem.larguraDeLeitura
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Um campo de texto com 1200 dp de largura é impossível de ler e ridículo de preencher. Os
 * formulários e os ecrãs de detalhe param na largura de leitura e ficam ao meio.
 *
 * O centrado verifica-se pelas margens dos dois lados, e não pelo centro do bloco: um bloco
 * encostado à esquerda com o dobro da largura teria o mesmo centro que um centrado estreito.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class LarguraDeLeituraUiTest {

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `numa janela larga o formulario para na largura de leitura e fica ao meio`() =
        runComposeUiTest {
            setContent {
                Box(Modifier.fillMaxSize()) {
                    // O texto enche a coluna de propósito: é o que ele ocupa que revela onde
                    // a coluna parou, e um texto curto media-se a si próprio.
                    Box(Modifier.larguraDeLeitura().background(Color.Red)) {
                        Text("campo", modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            val bloco = onNodeWithText("campo").getUnclippedBoundsInRoot()
            val esquerda = bloco.left.value
            val direita = 1280f - bloco.right.value

            assertTrue(
                bloco.width.value <= LARGURA_DE_LEITURA_DP.toFloat(),
                "o bloco passou a largura de leitura: ${bloco.width}",
            )
            assertTrue(abs(esquerda - direita) < 1f, "ficou encostado a um lado: $esquerda / $direita")
        }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `num telemovel nao aperta nada`() = runComposeUiTest {
        setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.larguraDeLeitura()) {
                    Text("campo", modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // 360 dp nunca chega ao teto, e o campo tem de continuar a ocupar o ecrã todo — um
        // formulário centrado com margens num telemóvel seria pior do que não fazer nada.
        val bloco = onNodeWithText("campo").getUnclippedBoundsInRoot()
        assertTrue(bloco.left.value < 1f, "apareceu margem à esquerda num telemóvel: ${bloco.left}")
        assertTrue(bloco.width.value > 300.dp.value, "o campo encolheu no telemóvel: ${bloco.width}")
    }
}
