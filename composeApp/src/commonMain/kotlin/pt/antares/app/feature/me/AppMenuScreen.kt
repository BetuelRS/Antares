package pt.antares.app.feature.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.feature.backup.CartaoDaCopia
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun AppMenuScreen(
    onSettingsClick: () -> Unit,
    onHealthClick: () -> Unit,
    onAttributionsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onBackupClick: () -> Unit,
    onDestinosClick: () -> Unit,
    onCrashClick: () -> Unit,
    onBack: () -> Unit,
) {
    val sendFeedback = rememberFeedbackSender()
    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.more_group_app), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Em cima de tudo, e não como mais uma linha da lista: desde a 2.1.0 esta é a
            // única cópia que existe, e uma linha entre outras seis não diz a ninguém que
            // está há um mês sem cópia.
            CartaoDaCopia()

            MenuItem(Res.string.settings_general_title, Icons.Default.Settings, onSettingsClick)

            MenuItem(Res.string.backup_title, Icons.Default.Save, onBackupClick)

            // A seguir à cópia e não na secção do «sobre»: as duas respondem à mesma
            // pergunta — onde é que os meus dados estão e para onde vão.
            MenuItem(Res.string.outgoing_title, Icons.Default.Public, onDestinosClick)
            MenuItem(Res.string.health_connect_title, Icons.Default.Favorite, onHealthClick)

            SectionHeader(title = stringResource(Res.string.more_group_about))
            MenuItem(Res.string.more_feedback, Icons.Default.Email, sendFeedback)
            MenuItem(Res.string.more_attributions, Icons.Default.Info, onAttributionsClick)
            MenuItem(Res.string.more_about, Icons.Default.History, onAboutClick)

            MenuItem(Res.string.crash_title, Icons.Default.BugReport, onCrashClick)
        }
    }
}

@Composable
private fun MenuItem(label: StringResource, icon: ImageVector, onClick: () -> Unit) {
    LinhaDaLista(titulo = stringResource(label), icone = icon, onClick = onClick)
}
