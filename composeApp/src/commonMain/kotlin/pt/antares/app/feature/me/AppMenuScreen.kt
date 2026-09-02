package pt.antares.app.feature.me

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresScreen
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.feature.backup.CartaoDaCopia
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

/**
 * O «Mais»: tudo o que não é um ecrã de todos os dias, num sítio só.
 *
 * Eram dois. O separador «Eu» tinha o progresso e cinco atalhos; o menu da app tinha outros
 * oito, atrás de uma engrenagem no canto do primeiro. O `estudo/areas/19` conta as definições
 * **espalhadas por três sítios, com uma delas repetida**, e pede exactamente isto: *«o
 * `AppMenuScreen` passa a ser o índice»*.
 *
 * O progresso saiu daqui para separador próprio, que era a outra metade da queixa.
 *
 * A ordem é por quantas vezes se abre, e não por assunto: o corpo e as metas primeiro, o que
 * se consulta a seguir, e o que se lê uma vez na vida no fim.
 */
@Composable
fun AppMenuScreen(
    corpo: DestinosDoCorpo,
    app: DestinosDaApp,
    sobre: DestinosDoSobre,
) {
    val sendFeedback = rememberFeedbackSender()
    AntaresScreen(
        topBar = { AntaresTopBar(title = stringResource(Res.string.nav_more)) },
        espaco = Spacing.sm,
        margem = PaddingValues(Spacing.lg),
    ) {
        // Em cima de tudo, e não como mais uma linha da lista: desde a 2.1.0 esta é a
        // única cópia que existe, e uma linha entre outras seis não diz a ninguém que
        // está há um mês sem cópia.
        CartaoDaCopia()

        SectionHeader(title = stringResource(Res.string.more_group_body))
        MenuItem(Res.string.more_profile_goals, Icons.Default.Person, corpo.perfil)
        MenuItem(Res.string.search_your_meals, Icons.Default.Restaurant, corpo.refeicoes)
        MenuItem(Res.string.more_nutrition_stats, Icons.Default.BarChart, corpo.estatisticas)
        MenuItem(Res.string.rich_title, Icons.Default.Search, corpo.ricoEm)
        MenuItem(Res.string.coach_history_title, Icons.Default.AutoAwesome, corpo.treinador)

        SectionHeader(title = stringResource(Res.string.more_group_app))
        MenuItem(Res.string.settings_general_title, Icons.Default.Settings, app.definicoes)
        MenuItem(Res.string.backup_title, Icons.Default.Save, app.copia)

        // A seguir à cópia e não na secção do «sobre»: as duas respondem à mesma
        // pergunta — onde é que os meus dados estão e para onde vão.
        MenuItem(Res.string.outgoing_title, Icons.Default.Public, app.destinos)
        MenuItem(Res.string.health_connect_title, Icons.Default.Favorite, app.saude)

        SectionHeader(title = stringResource(Res.string.more_group_about))
        MenuItem(Res.string.more_feedback, Icons.Default.Email, sendFeedback)
        MenuItem(Res.string.more_attributions, Icons.Default.Info, sobre.atribuicoes)
        MenuItem(Res.string.more_about, Icons.Default.History, sobre.sobre)
        MenuItem(Res.string.crash_title, Icons.Default.BugReport, sobre.falhas)
    }
}

@Composable
private fun MenuItem(label: StringResource, icon: ImageVector, onClick: () -> Unit) {
    LinhaDaLista(titulo = stringResource(label), icone = icon, onClick = onClick)
}

/**
 * Os destinos do «Mais», agrupados pelas mesmas três secções que o ecrã desenha.
 *
 * Doze lambdas soltas na assinatura eram doze coisas que o ecrã tinha de saber sobre o grafo,
 * e nada as prendia à secção onde aparecem. Assim a assinatura diz o que o ecrã é: o corpo, a
 * app, e o sobre.
 */
class DestinosDoCorpo(
    val perfil: () -> Unit,
    val refeicoes: () -> Unit,
    val estatisticas: () -> Unit,
    val ricoEm: () -> Unit,
    val treinador: () -> Unit,
)

class DestinosDaApp(
    val definicoes: () -> Unit,
    val copia: () -> Unit,
    val destinos: () -> Unit,
    val saude: () -> Unit,
)

class DestinosDoSobre(
    val atribuicoes: () -> Unit,
    val sobre: () -> Unit,
    val falhas: () -> Unit,
)
