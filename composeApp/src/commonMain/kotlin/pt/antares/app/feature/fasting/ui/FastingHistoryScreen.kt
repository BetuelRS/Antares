package pt.antares.app.feature.fasting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.core.model.FastingStatus
import pt.antares.app.core.util.dayShort
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.fasting_avg_duration
import pt.antares.app.generated.resources.fasting_completion_rate
import pt.antares.app.generated.resources.fasting_days_unit
import pt.antares.app.generated.resources.fasting_history_empty
import pt.antares.app.generated.resources.fasting_history_title
import pt.antares.app.generated.resources.fasting_status_broken
import pt.antares.app.generated.resources.fasting_status_completed
import pt.antares.app.generated.resources.fasting_streak_best
import pt.antares.app.generated.resources.fasting_streak_current
import kotlin.math.roundToInt

@Composable
fun FastingHistoryScreen(
    onBack: () -> Unit,
    viewModel: FastingHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.fasting_history_title), onBack = onBack) },
    ) { padding ->
        if (state.sessions.isEmpty()) {
            EmptyState(
                title = stringResource(Res.string.fasting_history_empty),
                modifier = Modifier.padding(padding),
            )
            return@AntaresScaffold
        }

        val s = state.stats
        ListaAdaptavel(modifier = Modifier.fillMaxSize().padding(padding)) {
            linhaInteira {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                    StatTile(stringResource(Res.string.fasting_streak_current), "${s.currentStreak}${stringResource(Res.string.fasting_days_unit)}", Modifier.weight(1f))
                    StatTile(stringResource(Res.string.fasting_streak_best), "${s.longestStreak}${stringResource(Res.string.fasting_days_unit)}", Modifier.weight(1f))
                }
            }
            linhaInteira {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                    StatTile(stringResource(Res.string.fasting_completion_rate), "${(s.completionRate * 100).roundToInt()}%", Modifier.weight(1f))
                    StatTile(stringResource(Res.string.fasting_avg_duration), FastingFormat.hm(s.averageDurationMs), Modifier.weight(1f))
                }
            }
            items(state.sessions, key = { it.id }) { session -> SessionRow(session) }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    AntaresCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SessionRow(session: FastingSessionEntity) {
    val completed = session.status == FastingStatus.COMPLETED
    val duration = (session.endedAt ?: session.startedAt) - session.startedAt
    val date = dayShort(epochMillisToLocalDate(session.startedAt))
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f, fill = false).padding(end = Spacing.md)) {
                Text(date, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(if (completed) Res.string.fasting_status_completed else Res.string.fasting_status_broken),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(FastingFormat.hm(duration), style = MaterialTheme.typography.titleMedium)
        }
    }
}
