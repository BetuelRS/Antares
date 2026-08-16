package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightUnitLabel
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.FilterBar
import pt.antares.app.core.designsystem.components.FilterDropdownChip
import pt.antares.app.core.designsystem.components.FilterOption
import pt.antares.app.core.util.dayShort
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.mesLabel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun WorkoutHistoryScreen(
    onSession: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: WorkoutHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.workout_history_title), onBack = onBack) },
    ) { padding ->
        if (state.todos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.workout_history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                FilterBar {
                    FilterDropdownChip(
                        label = stringResource(Res.string.filter_month),
                        selected = state.mes,
                        options = state.meses.map { FilterOption(it, mesLabel(it)) },
                        onSelect = viewModel::setMes,
                    )
                    FilterDropdownChip(
                        label = stringResource(Res.string.filter_exercise),
                        selected = state.exercicioId,
                        options = state.exercicios.map { FilterOption(it.id, it.name) },
                        onSelect = viewModel::setExercicio,
                    )
                }
            }

            // Uma lista vazia por causa do filtro não é um histórico vazio, e dizer-lhe o
            // mesmo era mandar a pessoa procurar treinos que ela tem.
            if (state.visiveis.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.filter_no_match),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.lg),
                    )
                }
            }

            items(state.visiveis, key = { it.id }) { s ->
                AntaresCard(modifier = Modifier.fillMaxWidth().clickable { onSession(s.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(dayShort(epochMillisToLocalDate(s.startedAt)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false).padding(end = Spacing.md))
                        Text(
                            weightWithUnit(s.volume, unidades),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
