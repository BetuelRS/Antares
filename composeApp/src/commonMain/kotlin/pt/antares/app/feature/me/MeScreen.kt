package pt.antares.app.feature.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.feature.progress.ProgressSections
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun MeScreen(
    onSettingsMenu: () -> Unit,
    onProfileClick: () -> Unit,
    onWeightClick: () -> Unit,
    onPhotosClick: () -> Unit,
    onStatsClick: () -> Unit,
    onRichInClick: () -> Unit,
    onCoachClick: () -> Unit,
) {
    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(Res.string.nav_profile),
                actions = {
                    IconButton(onClick = onSettingsMenu) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(Res.string.settings_general_title),
                        )
                    }
                },
            )
        },
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

            ProgressSections(
                onWeightHistory = onWeightClick,
                onPhotos = onPhotosClick,
            )

            SectionHeader(title = stringResource(Res.string.more_group_body))
            MeItem(Res.string.more_profile_goals, Icons.Default.Person, onProfileClick)

            MeItem(Res.string.more_nutrition_stats, Icons.Default.BarChart, onStatsClick)
            MeItem(Res.string.rich_title, Icons.Default.Search, onRichInClick)
            MeItem(Res.string.coach_history_title, Icons.Default.AutoAwesome, onCoachClick)
        }
    }
}

@Composable
private fun MeItem(label: StringResource, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(label)) },
            // Decorativo: cada linha do menu tem o seu nome escrito ao lado.
            leadingContent = { Icon(icon, contentDescription = null) },
        )
    }
}
