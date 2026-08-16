package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.database.entities.BodyMeasurementEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.Sparkline
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.feature.profile.data.BodyMeasurementRepository
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

class MeasurementHistoryViewModel(
    repository: BodyMeasurementRepository,
) : ViewModel() {

    val entries: StateFlow<List<BodyMeasurementEntity>> = repository.observeAll()
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeasurementHistoryScreen(
    onBack: () -> Unit,
    viewModel: MeasurementHistoryViewModel = koinViewModel(),
) {
    val entries by viewModel.entries.collectAsState()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.measure_history_title), onBack = onBack) },
    ) { padding ->
        if (entries.size < 2) {
            EmptyState(
                title = stringResource(Res.string.measure_history_title),
                subtitle = stringResource(Res.string.measure_history_empty),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        // Os cartões de medida ficam lado a lado numa janela larga, e não é só arrumação: as
        // quatro medidas comparam-se entre si, e empilhadas obrigavam a percorrer para o fazer.
        ListaAdaptavel(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
            espaco = Spacing.sm,
        ) {

            val fat = entries.mapNotNull { it.bodyFatPct }
            if (fat.size >= 2) {
                item {
                    AntaresCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(Res.string.profile_health_body_fat, fmtG(fat.last())),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Sparkline(
                            primary = fat,
                            modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = Spacing.sm),
                        )
                    }
                }
            }

            val series = listOf(
                Res.string.bodycomp_waist to entries.mapNotNull { it.waistCm },
                Res.string.bodycomp_arm to entries.mapNotNull { it.armCm },
                Res.string.bodycomp_thigh to entries.mapNotNull { it.thighCm },
                Res.string.bodycomp_chest to entries.mapNotNull { it.chestCm },
            )
            series.forEach { (label, valores) ->
                if (valores.size >= MIN_POINTS_FOR_LINE) {
                    item {
                        AntaresCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(label) + " ${fmtG(valores.last())}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Sparkline(
                                primary = valores,
                                modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = Spacing.sm),
                            )
                        }
                    }
                }
            }

            items(entries.reversed(), key = { it.id }) { entry ->
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        dayShortDated(entry.epochDay),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        entry.bodyFatPct?.let {
                            Text(
                                stringResource(Res.string.profile_health_body_fat, fmtG(it)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        val medidas = listOf(
                            Res.string.bodycomp_waist to entry.waistCm,
                            Res.string.bodycomp_arm to entry.armCm,
                            Res.string.bodycomp_thigh to entry.thighCm,
                            Res.string.bodycomp_chest to entry.chestCm,
                        )
                        medidas.forEach { (label, valor) ->
                            valor?.let {
                                Text(
                                    "${stringResource(label)} ${fmtG(it)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val MIN_POINTS_FOR_LINE = 2
