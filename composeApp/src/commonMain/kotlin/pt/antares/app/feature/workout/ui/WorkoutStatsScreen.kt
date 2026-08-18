package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightUnitLabel
import pt.antares.app.core.designsystem.loadWithUnit
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LabeledBar
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun WorkoutStatsScreen(
    onBack: () -> Unit,
    viewModel: WorkoutStatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.workout_stats_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {

            Text(stringResource(Res.string.workout_stats_volume_week), style = MaterialTheme.typography.titleMedium)
            if (state.muscleVolume.isEmpty()) {
                Text(stringResource(Res.string.workout_stats_no_volume), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    val maxV = state.muscleVolume.maxOf { it.volume }.toFloat()
                    state.muscleVolume.forEach { mv ->
                        LabeledBar(
                            label = stringResource(muscleLabel(mv.muscle)),
                            value = mv.volume.toFloat(),
                            maxValue = maxV,
                            valueText = weightWithUnit(mv.volume, unidades),
                        )
                    }
                }
            }

            Text(stringResource(Res.string.workout_stats_records), style = MaterialTheme.typography.titleMedium)
            if (state.records.isEmpty()) {
                Text(stringResource(Res.string.workout_stats_no_records), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    state.records.forEach { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {

                            Text(r.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false).padding(end = Spacing.md))
                            Text(
                                loadWithUnit(r.oneRm, unidades),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
