package pt.antares.app.ui

import android.content.ContentProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pt.antares.app.feature.running.domain.Split
import pt.antares.app.feature.running.ui.SplitsTable

/**
 * O título da tabela de parciais diz o que ela tem lá dentro.
 *
 * A 2.30.0 meteu as voltas marcadas à mão na mesma tabela dos quilómetros e deixou o
 * cabeçalho a dizer «Parciais por km» — com uma «Volta 1» logo por baixo. Nenhum dos testes
 * dessa versão desenhava a tabela, e foi a corrida no aparelho da 2.31.0 que o viu.
 *
 * Procura-se a palavra nas duas línguas, porque o que se guarda é a regra e não a tradução.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class TabelaDeParciaisUiTest {

    // O título vem de um recurso, e os recursos do Compose precisam do contexto que o
    // fornecedor deles arranca — sem isto o teste rebenta antes de desenhar, e rebentar
    // parecia a falha que ele guarda.
    @Before
    fun arrancaOsRecursos() {
        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()
    }

    private val menciona = hasText("voltas", substring = true, ignoreCase = true) or
        hasText("laps", substring = true, ignoreCase = true)

    private fun km(i: Int) = Split(index = i, distanceM = 1000.0, movingMs = 300_000, paceSecPerKm = 300, kcal = 60)

    @Test
    fun `com voltas, o titulo di-lo`() = runComposeUiTest {
        val volta = Split(index = 1, distanceM = 400.0, movingMs = 90_000, paceSecPerKm = 225, kcal = 25, manual = true)
        setContent { SplitsTable(listOf(km(1), volta, km(2))) }

        onNode(menciona).assertExists("o título diz «por km» e a tabela tem uma volta")
    }

    @Test
    fun `so com quilometros, o titulo nao fala de voltas`() = runComposeUiTest {
        setContent { SplitsTable(listOf(km(1), km(2))) }

        onNode(menciona).assertDoesNotExist()
    }
}
