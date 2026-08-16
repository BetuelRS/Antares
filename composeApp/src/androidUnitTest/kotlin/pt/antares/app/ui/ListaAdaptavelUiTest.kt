package pt.antares.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
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
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.linhaInteira
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Uma lista de linhas curtas esticada por 1280 dp é meio ecrã de nada. A [ListaAdaptavel]
 * ganha colunas com a janela, e o que este teste guarda é o que se vê: itens lado a lado
 * numa janela larga, uns por baixo dos outros num telemóvel de pé.
 *
 * Compara-se o topo de cada item, e não o número de colunas: contar colunas seria repetir a
 * conta que o código já faz, e passaria na mesma se o grid as ignorasse.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ListaAdaptavelUiTest {

    private val itens = listOf("um", "dois", "tres", "quatro", "cinco", "seis")

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `numa janela larga os tres primeiros ficam na mesma linha`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                ListaAdaptavel(modifier = Modifier.fillMaxSize()) {
                    items(itens) { Text(it) }
                }
            }
        }

        val topo = onNodeWithText("um").getUnclippedBoundsInRoot().top
        assertEquals(topo, onNodeWithText("dois").getUnclippedBoundsInRoot().top)
        assertEquals(topo, onNodeWithText("tres").getUnclippedBoundsInRoot().top)

        // O quarto começa a linha seguinte: três colunas é o teto, e sem ele a lista virava
        // uma fila de seis colunas estreitas.
        assertTrue(onNodeWithText("quatro").getUnclippedBoundsInRoot().top > topo)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `num telemovel de pe cada item fica na sua linha`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                ListaAdaptavel(modifier = Modifier.fillMaxSize()) {
                    items(itens) { Text(it) }
                }
            }
        }

        val primeiro = onNodeWithText("um").getUnclippedBoundsInRoot().top
        assertTrue(onNodeWithText("dois").getUnclippedBoundsInRoot().top > primeiro)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `um cabecalho atravessa as colunas todas`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                ListaAdaptavel(modifier = Modifier.fillMaxSize()) {
                    linhaInteira { Text("cabecalho") }
                    items(itens) { Text(it) }
                }
            }
        }

        // Um `item` normal ocuparia uma célula, e o cabeçalho ficava encavalitado ao lado do
        // primeiro resultado — que é exatamente o que o `linhaInteira` existe para evitar.
        val cabecalho = onNodeWithText("cabecalho").getUnclippedBoundsInRoot()
        val primeiro = onNodeWithText("um").getUnclippedBoundsInRoot()
        assertTrue(primeiro.top > cabecalho.top, "o cabeçalho não empurrou os itens para baixo")
    }
}
