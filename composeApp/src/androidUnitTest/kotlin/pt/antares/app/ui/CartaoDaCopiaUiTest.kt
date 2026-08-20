package pt.antares.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.feature.backup.AvisoDeCopiaAtrasada
import pt.antares.app.feature.backup.CartaoDaCopia
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.copia_agora
import pt.antares.app.generated.resources.copia_hoje
import pt.antares.app.generated.resources.copia_nunca
import pt.antares.app.generated.resources.copia_nunca_hoje
import pt.antares.app.generated.resources.copia_titulo
import pt.antares.app.testing.FluxoUiHarness
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

/**
 * O cartão da cópia é a única coisa que diz a alguém que está desprotegido. Se ele mentir —
 * ou não aparecer — a cópia automática volta a ser invisível, que é exatamente o defeito da
 * cópia da Google que a 2.1.0 desligou.
 *
 * O aviso do Hoje tem a regra inversa: **só** pode aparecer quando há mesmo atraso. Um cartão
 * de alarme que aparece todos os dias deixa de ser lido em duas semanas.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, qualifiers = "w411dp-h891dp")
class CartaoDaCopiaUiTest : FluxoUiHarness() {

    private fun copiaHa(dias: Int) = runBlocking {
        val quando = Clock.System.now() - dias.days
        prefs.setLastBackup(quando.toEpochMilliseconds(), "antares-copia-antiga.zip")
    }

    @Test
    fun `sem cópia nenhuma o cartão di-lo`() = runComposeUiTest {
        arrancaKoin()

        val textos = Textos()
        setContent {
            textos.ler(Res.string.copia_titulo, Res.string.copia_nunca)
            CartaoDaCopia()
        }

        waitUntil("o cartão da cópia nunca chegou ao ecrã", ESPERA_MS) {
            onAllNodesWithText(textos[Res.string.copia_titulo]).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(
            1,
            onAllNodesWithText(textos[Res.string.copia_nunca]).fetchSemanticsNodes().size,
            "o cartão não diz que nunca houve cópia — que é o pior estado possível",
        )
    }

    @Test
    fun `uma cópia de hoje lê-se como hoje`() = runComposeUiTest {
        arrancaKoin()
        copiaHa(dias = 0)

        val textos = Textos()
        setContent {
            textos.ler(Res.string.copia_titulo, Res.string.copia_hoje)
            CartaoDaCopia()
        }

        waitUntil("o cartão nunca leu o estado da cópia", ESPERA_MS) {
            onAllNodesWithText(textos[Res.string.copia_hoje]).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `o aviso do Hoje cala-se quando a cópia está em dia`() = runComposeUiTest {
        arrancaKoin()
        copiaHa(dias = 1)

        val textos = Textos()
        setContent {
            textos.ler(Res.string.copia_agora)
            AvisoDeCopiaAtrasada()
        }

        // Pelo botão e não pela frase: com a cópia de ontem a frase que apareceria era
        // outra — «a tua última cópia tem 1 dias» —, e procurar a frase do «nunca» dava um
        // teste que passava na mesma com o cartão inteiro no ecrã. O botão está no cartão e
        // em mais lado nenhum desta composição.
        waitForIdle()
        assertEquals(
            0,
            onAllNodesWithText(textos[Res.string.copia_agora]).fetchSemanticsNodes().size,
            "o aviso de cópia em atraso apareceu com a cópia feita ontem",
        )
    }

    @Test
    fun `o aviso do Hoje aparece quando nunca houve cópia`() = runComposeUiTest {
        arrancaKoin()

        val textos = Textos()
        setContent {
            textos.ler(Res.string.copia_nunca_hoje)
            AvisoDeCopiaAtrasada()
        }

        waitUntil("o aviso nunca apareceu, e não há cópia nenhuma", ESPERA_MS) {
            onAllNodesWithText(textos[Res.string.copia_nunca_hoje]).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val ESPERA_MS = 5_000L
    }
}
