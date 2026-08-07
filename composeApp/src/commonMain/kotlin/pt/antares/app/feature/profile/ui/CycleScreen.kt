package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.CycleCalc
import pt.antares.app.core.database.entities.CycleEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.SplitRow
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.CycleRepository
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

data class CycleState(

    val entries: List<CycleEntity> = emptyList(),
) {
    private val inicios: List<Long> get() = entries.map { it.startEpochDay }

    val averageCycleDays: Int? get() = CycleCalc.averageCycleDays(inicios)
    val dayOfCycle: Int? get() = CycleCalc.dayOfCycle(inicios, todayEpochDay())
    val predictedNextStart: Long? get() = CycleCalc.predictedNextStart(inicios)
    val retentionLikely: Boolean get() = CycleCalc.retentionLikely(inicios, todayEpochDay())

    val openPeriod: Boolean
        get() = entries.lastOrNull()?.endEpochDay == null && entries.isNotEmpty()
}

class CycleViewModel(
    private val repository: CycleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CycleState())
    val state: StateFlow<CycleState> = _state

    init {
        repository.observeAll()
            .onEach { linhas -> _state.update { it.copy(entries = linhas) } }
            .launchIn(viewModelScope)
    }

    fun start() = viewModelScope.launch { repository.recordStart() }
    fun end() = viewModelScope.launch { repository.recordEnd() }
    fun delete(id: String) = viewModelScope.launch { repository.delete(id) }
}

@Composable
fun CycleScreen(
    onBack: () -> Unit,
    viewModel: CycleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.cycle_title), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Text(
                    stringResource(Res.string.cycle_why),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                Text(
                    stringResource(Res.string.cycle_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            item { StatusCard(state) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    PrimaryButton(
                        text = stringResource(Res.string.cycle_start_today),
                        onClick = viewModel::start,
                        modifier = Modifier.weight(1f),
                    )

                    if (state.openPeriod) {
                        SecondaryButton(
                            text = stringResource(Res.string.cycle_end_today),
                            onClick = viewModel::end,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            items(state.entries.reversed(), key = { it.id }) { linha ->
                CycleRow(linha, onDelete = { viewModel.delete(linha.id) })
            }
        }
    }
}

@Composable
private fun StatusCard(state: CycleState) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        val dia = state.dayOfCycle
        if (dia == null) {
            Text(
                stringResource(Res.string.cycle_no_data),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@AntaresCard
        }
        Text(
            stringResource(Res.string.cycle_day, dia),
            style = MaterialTheme.typography.titleMedium,
        )

        state.averageCycleDays?.let {
            Text(
                stringResource(Res.string.cycle_average, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.predictedNextStart?.let {
            Text(
                stringResource(Res.string.cycle_next, dayShortDated(it)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.retentionLikely) {

            Text(
                stringResource(Res.string.cycle_retention),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun CycleRow(linha: CycleEntity, onDelete: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        SplitRow(
            leading = {
                Column {
                    Text(
                        dayShortDated(linha.startEpochDay),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val duracao = CycleCalc.periodLengthDays(linha.startEpochDay, linha.endEpochDay)
                    Text(

                        if (duracao == null) {
                            stringResource(Res.string.cycle_open)
                        } else {
                            stringResource(Res.string.cycle_days, duracao)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            trailing = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.common_delete),
                    )
                }
            },
        )
    }
}
