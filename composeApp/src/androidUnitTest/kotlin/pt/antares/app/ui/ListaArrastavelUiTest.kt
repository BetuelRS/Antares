package pt.antares.app.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.ListaArrastavel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A lista que se reordena a arrastar.
 *
 * **Este teste existe porque o gesto não se prova no emulador.** O `adb input draganddrop`
 * injecta um toque longo seguido de movimento e a lista não mexeu — não por estar partida,
 * mas porque a injecção não reproduz o tempo do toque longo como o dedo o faz. Aqui o gesto é
 * construído passo a passo, e o que se afirma é o que interessa: **a ordem que sai é a ordem
 * que o dedo desenhou, e grava-se uma vez só**.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = "w411dp-h891dp")
class ListaArrastavelUiTest {

    private val alturaDaLinha = 60.dp

    /** Folga sobre o tempo do toque longo, para o gesto não ficar na fronteira. */
    private val folgaMs = 200L

    /** Um pouco além de metade do vizinho, que é onde a troca acontece. */
    private val margemPx = 20f

    @Test
    fun `arrastar uma linha para cima poe-a acima da vizinha`() = runComposeUiTest {
        var ordemGravada: List<String>? = null
        var vezes = 0

        setContent {
            ListaArrastavel(
                itens = listOf("a", "b", "c"),
                chave = { it },
                espaco = Spacing.sm,
                aoLargar = { ordemGravada = it; vezes++ },
            ) { item, _ ->
                Text(item, modifier = Modifier.fillMaxWidth().height(alturaDaLinha))
            }
        }
        waitForIdle()

        // O «c» sobe uma posição: o gesto tem de passar de metade do vizinho, e por isso o
        // movimento é maior do que a altura de uma linha.
        onNodeWithText("c").performTouchInput {
            down(center)
            // O toque longo tem de acontecer **dentro do mesmo gesto**: é o que distingue
            // arrastar de percorrer a lista, e um `longClick()` à parte já levantou o dedo.
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + folgaMs)
            moveBy(androidx.compose.ui.geometry.Offset(0f, -(alturaDaLinha.toPx() + margemPx)))
            up()
        }
        waitForIdle()

        assertEquals(listOf("a", "c", "b"), ordemGravada, "a ordem que saiu não é a que o dedo desenhou")
        assertEquals(1, vezes, "gravou-se mais do que uma vez para um arrasto só")
    }

    @Test
    fun `largar sem sair do sitio nao grava nada`() = runComposeUiTest {
        var vezes = 0

        setContent {
            ListaArrastavel(
                itens = listOf("a", "b"),
                chave = { it },
                espaco = Spacing.sm,
                aoLargar = { vezes++ },
            ) { item, _ ->
                Text(item, modifier = Modifier.fillMaxWidth().height(alturaDaLinha))
            }
        }
        waitForIdle()

        onNodeWithText("a").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(0f, 4f))
            up()
        }
        waitForIdle()

        assertEquals(0, vezes, "um toque que não moveu nada escreveu na base")
    }
    /**
     * O mesmo gesto, mas **dentro de um `LazyColumn`** — que é onde a lista vive no editor de
     * rotinas. Uma lista que rola e um arrasto vertical disputam o mesmo dedo, e um arrastar
     * que só funciona fora dela não serve de nada.
     */
    @Test
    fun `arrastar funciona dentro de uma lista que rola`() = runComposeUiTest {
        var ordemGravada: List<String>? = null

        setContent {
            LazyColumn {
                item {
                    ListaArrastavel(
                        itens = listOf("a", "b", "c"),
                        chave = { it },
                        espaco = Spacing.sm,
                        aoLargar = { ordemGravada = it },
                    ) { item, _ ->
                        Text(item, modifier = Modifier.fillMaxWidth().height(alturaDaLinha))
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithText("a").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + folgaMs)
            moveBy(androidx.compose.ui.geometry.Offset(0f, alturaDaLinha.toPx() + margemPx))
            up()
        }
        waitForIdle()

        assertEquals(listOf("b", "a", "c"), ordemGravada, "a lista roubou o dedo ao arrasto")
    }
}
