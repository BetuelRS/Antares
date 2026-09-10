package pt.antares.app.feature.running.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.FilterBar
import pt.antares.app.core.designsystem.components.FilterDropdownChip
import pt.antares.app.core.designsystem.components.FilterOption
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.core.util.mesLabel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.filter_month
import pt.antares.app.generated.resources.filter_no_match
import pt.antares.app.generated.resources.filter_type
import pt.antares.app.generated.resources.run_history_empty
import pt.antares.app.generated.resources.run_history_title

@Composable
fun RunHistoryScreen(
    onRun: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RunHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()
    val hoje = remember { todayEpochDay() }

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.run_history_title), onBack = onBack) },
    ) { padding ->
        if (state.runs.isEmpty()) {
            EmptyState(title = stringResource(Res.string.run_history_empty), modifier = Modifier.padding(padding))
            return@AntaresScaffold
        }
        ListaAdaptavel(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Os totais e os recordes viviam aqui, e passaram ao hub na 2.31.0: um facto num
            // sítio só, por decisão do dono. O histórico ficou a ser a lista, com os filtros.
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

            items(state.visiveis, key = { it.id }) { run -> CorridaNaLista(run, unidades, hoje, onRun) }
        }
    }
}
