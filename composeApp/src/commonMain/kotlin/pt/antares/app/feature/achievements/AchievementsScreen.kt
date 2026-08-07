package pt.antares.app.feature.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.ach_cat_body_fat
import pt.antares.app.generated.resources.ach_cat_waist
import pt.antares.app.generated.resources.ach_cat_fasts
import pt.antares.app.generated.resources.ach_cat_run
import pt.antares.app.generated.resources.ach_cat_weighins
import pt.antares.app.generated.resources.ach_cat_workouts
import pt.antares.app.generated.resources.ach_summary
import pt.antares.app.generated.resources.ach_title

private data class CatVisual(val emoji: String, val label: StringResource)

private fun visual(cat: AchievementCategory): CatVisual = when (cat) {
    AchievementCategory.WORKOUTS -> CatVisual("💪", Res.string.ach_cat_workouts)
    AchievementCategory.RUN_KM -> CatVisual("🏃", Res.string.ach_cat_run)
    AchievementCategory.FASTS -> CatVisual("⏱️", Res.string.ach_cat_fasts)
    AchievementCategory.WEIGHINS -> CatVisual("⚖️", Res.string.ach_cat_weighins)

    AchievementCategory.WAIST_CM -> CatVisual("📏", Res.string.ach_cat_waist)
    AchievementCategory.BODY_FAT_PCT -> CatVisual("🔥", Res.string.ach_cat_body_fat)
}

@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.ach_title), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text(
                    stringResource(Res.string.ach_summary, state.unlocked, state.total),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            items(state.achievements, key = { "${it.category}-${it.target}" }) { ach ->
                AchievementRow(ach)
            }
        }
    }
}

@Composable
private fun AchievementRow(ach: Achievement) {
    val v = visual(ach.category)

    val alpha = if (ach.unlocked) 1f else 0.45f
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(v.emoji, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.alpha(alpha))
            Column(Modifier.weight(1f)) {
                Text(
                    "${stringResource(v.label)} · ${ach.target}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (ach.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { ach.fraction },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                Text(
                    "${ach.current.coerceAtMost(ach.target)} / ${ach.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
