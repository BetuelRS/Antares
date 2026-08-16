package pt.antares.app.ui

import android.content.ContentProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.stringResource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.core.designsystem.LocalAlturaDaJanela
import pt.antares.app.core.designsystem.LocalLarguraDaJanela
import pt.antares.app.core.designsystem.LocalModoDeEsquema
import pt.antares.app.core.designsystem.ProvedorDaJanela
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.nav_profile
import pt.antares.app.generated.resources.nav_today
import pt.antares.app.navigation.AntaresNavigationRail

/**
 * A app esteve trancada em retrato desde sempre. Ao destrancar, a primeira coisa que tem de
 * se aguentar é o casco: a janela ser medida onde deve, e a navegação mudar de sítio sem
 * perder separadores pelo caminho.
 *
 * O tamanho vem dos qualificadores do Robolectric — `w1280dp-h800dp` — e não de um
 * `Modifier.size`: uma caixa maior do que a janela do teste desenha-se fora dela, e o
 * `assertIsDisplayed` recusa o que está fora do ecrã mesmo existindo na árvore.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class JanelaLargaUiTest {

    @Before
    fun arrancaOsRecursos() {
        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `uma janela de tablet da a classe larga e o modo de duas colunas`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                Text(
                    "${LocalLarguraDaJanela.current} ${LocalModoDeEsquema.current} " +
                        "${LocalAlturaDaJanela.current}",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        onNodeWithText("LARGA DUAS_COLUNAS NORMAL").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `um telemovel de pe continua compacto e numa coluna`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                Text(
                    "${LocalLarguraDaJanela.current} ${LocalModoDeEsquema.current} " +
                        "${LocalAlturaDaJanela.current}",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        onNodeWithText("COMPACTA UMA_COLUNA NORMAL").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp")
    fun `o telemovel deitado e baixo, e a navegacao tem de sair de baixo`() = runComposeUiTest {
        setContent {
            ProvedorDaJanela {
                Text("${LocalAlturaDaJanela.current}", modifier = Modifier.fillMaxSize())
            }
        }

        onNodeWithText("BAIXA").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun `o riel mostra os mesmos separadores que a barra`() = runComposeUiTest {
        lateinit var hoje: String
        lateinit var perfil: String

        setContent {
            hoje = stringResource(Res.string.nav_today)
            perfil = stringResource(Res.string.nav_profile)
            AntaresNavigationRail(rememberNavController())
        }

        // O primeiro e o último da lista: se o riel tivesse ficado com uma lista própria, é
        // nas pontas que a diferença aparecia.
        onNodeWithText(hoje).assertIsDisplayed()
        onNodeWithText(perfil).assertIsDisplayed()
    }
}
