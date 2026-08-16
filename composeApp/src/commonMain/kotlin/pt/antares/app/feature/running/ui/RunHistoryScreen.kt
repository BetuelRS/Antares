package pt.antares.app.feature.running.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.designsystem.paceUnitLabel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_history_empty
import pt.antares.app.generated.resources.run_history_title
import pt.antares.app.generated.resources.run_pr_10k
import pt.antares.app.generated.resources.run_pr_1k
import pt.antares.app.generated.resources.run_pr_5k
import pt.antares.app.generated.resources.run_pr_title
import pt.antares.app.generated.resources.run_totals_distance
import pt.antares.app.generated.resources.run_totals_runs
import pt.antares.app.generated.resources.run_totals_time
import pt.antares.app.generated.resources.run_totals_title

@Composable
fun RunHistoryScreen(
    onRun: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RunHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.run_history_title), onBack = onBack) },
    ) { padding ->
        if (state.runs.isEmpty()) {
            EmptyState(title = stringResource(Res.string.run_history_empty), modifier = Modifier.padding(padding))
            return@AntaresScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text(stringResource(Res.string.run_totals_title), style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                    PrTile(stringResource(Res.string.run_totals_runs), "${state.totalRuns}", Modifier.weight(1f))
                    PrTile(
                        stringResource(Res.string.run_totals_distance),
                        "${RunFormat.distance(state.totalDistanceM, unidades)} " +
                            stringResource(distanceUnitLabel(unidades)),
                        Modifier.weight(1f),
                    )
                    PrTile(stringResource(Res.string.run_totals_time), RunFormat.clock(state.totalMovingS * 1000), Modifier.weight(1f))
                }
            }
            item {
                Text(stringResource(Res.string.run_pr_title), style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                    PrTile(stringResource(Res.string.run_pr_1k), state.pr1kMs?.let { RunFormat.clock(it) } ?: "--", Modifier.weight(1f))
                    PrTile(stringResource(Res.string.run_pr_5k), state.pr5kMs?.let { RunFormat.clock(it) } ?: "--", Modifier.weight(1f))
                    PrTile(stringResource(Res.string.run_pr_10k), state.pr10kMs?.let { RunFormat.clock(it) } ?: "--", Modifier.weight(1f))
                }
            }
            items(state.runs, key = { it.id }) { run -> RunRow(run, unidades, onRun) }
        }
    }
}

@Composable
private fun RunRow(run: RunEntity, unidades: UnitSystem, onRun: (String) -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth().clickable { onRun(run.id) }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

            Column(Modifier.weight(1f, fill = false).padding(end = Spacing.md)) {

                Text(
                    run.name.ifBlank { stringResource(activityLabel(run.type)) },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "${epochMillisToLocalDate(run.startedAt)} · ${RunFormat.clock(run.movingS * 1000)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${RunFormat.distance(run.distanceM, unidades)} ${stringResource(distanceUnitLabel(unidades))}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
