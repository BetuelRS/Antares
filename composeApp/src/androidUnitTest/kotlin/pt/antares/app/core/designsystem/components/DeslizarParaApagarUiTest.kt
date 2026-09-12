package pt.antares.app.core.designsystem.components

import android.content.ContentProvider
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
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

    /**
     * O desfazer, dentro de uma lista com chave, que é onde o diário o usa.
     *
     * A lista guarda o estado de cada item pela chave, mesmo depois de ele sair — é o que faz um
     * item voltar como estava quando se rola para trás. Com o estado do gesto guardável, a linha
     * reposta pelo desfazer voltava **deslizada**: fora do ecrã, e a chamar o apagar outra vez.
     */
    @Test
    fun `a linha reposta pelo desfazer volta inteira e nao se apaga outra vez`() = runComposeUiTest {
        var vezes = 0
        val linhas = mutableStateListOf("a", "b")
        setContent {
            LazyColumn {
                items(linhas, key = { it }) { linha ->
                    DeslizarParaApagar(onApagar = { vezes++; linhas.remove(linha) }) {
                        Text(linha, modifier = Modifier.fillMaxWidth().height(60.dp))
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithText("a").performTouchInput { swipeLeft() }
        waitForIdle()
        onNodeWithText("a").assertDoesNotExist()

        // O desfazer: a mesma linha, com a mesma chave, volta à lista.
        linhas.add(0, "a")
        waitForIdle()

        assertEquals(1, vezes, "a linha reposta voltou a chamar o apagar")
        onNodeWithText("a").assertIsDisplayed()
    }

    /**
     * O fundo do gesto está sempre composto, por baixo da linha. Com descrição, o leitor de ecrã
     * parava nele em cada registo e anunciava um «Apagar» que não se toca — quem usa o leitor
     * apaga pelo menu da linha.
     */
    @Test
    fun `o fundo do gesto nao se anuncia ao leitor de ecra`() = runComposeUiTest {
        setContent {
            DeslizarParaApagar(onApagar = {}) {
                Text("linha", modifier = Modifier.fillMaxWidth().height(60.dp))
            }
        }
        waitForIdle()

        val comDescricao = onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertEquals(0, comDescricao.size, "o fundo do gesto tem descrição, e não faz nada")
    }
}
