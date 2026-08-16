package pt.antares.app.feature.running.ui

import androidx.compose.foundation.clickable
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
import pt.antares.app.core.designsystem.components.FilterBar
import pt.antares.app.core.designsystem.components.FilterDropdownChip
import pt.antares.app.core.designsystem.components.FilterOption
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.mesLabel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.filter_month
import pt.antares.app.generated.resources.filter_no_match
import pt.antares.app.generated.resources.filter_type
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
        ListaAdaptavel(modifier = Modifier.fillMaxSize().padding(padding)) {
            linhaInteira {
                Text(stringResource(Res.string.run_totals_title), style = MaterialTheme.typography.titleMedium)
            }
            linhaInteira {
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
            linhaInteira {
                Text(stringResource(Res.string.run_pr_title), style = MaterialTheme.typography.titleMedium)
            }
            linhaInteira {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                    PrTile(stringResource(Res.string.run_pr_1k), state.pr1kMs?.let { RunFormat.clock(it) } ?: "--", Modifier.weight(1f))
                    PrTile(stringResource(Res.string.run_pr_5k), state.pr5kMs?.let { RunFormat.clock(it) } ?: "--", Modifier.weight(1f))
                    PrTile(stringResource(Res.string.run_pr_10k), state.pr10kMs?.let { RunFormat.clock(it) } ?: "--", Modifier.weight(1f))
                }
            }
            linhaInteira {
                FilterBar {
                    FilterDropdownChip(
                        label = stringResource(Res.string.filter_month),
                        selected = state.mes,
                        options = state.meses.map { FilterOption(it, mesLabel(it)) },
                        onSelect = viewModel::setMes,
                    )
                    FilterDropdownChip(
                        label = stringResource(Res.string.filter_type),
                        selected = state.tipo,
                        options = state.tipos.map { FilterOption(it, stringResource(activityLabel(it))) },
                        onSelect = viewModel::setTipo,
                    )
                }
            }

            // Filtrar até não sobrar nada não é o mesmo que não ter corrido nunca.
            if (state.visiveis.isEmpty()) {
                linhaInteira {
                    Text(
                        stringResource(Res.string.filter_no_match),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.lg),
                    )
                }
            }

            items(state.visiveis, key = { it.id }) { run -> RunRow(run, unidades, onRun) }
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
