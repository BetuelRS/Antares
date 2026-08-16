package pt.antares.app.feature.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.feature.workout.ui.WorkoutHubViewModel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun WorkoutScreen(
    onLibrary: () -> Unit,
    onRoutine: (String) -> Unit,
    onStartEmpty: () -> Unit,
    onResume: () -> Unit,
    onHistory: () -> Unit,
    onStats: () -> Unit,
    onSchedule: () -> Unit,
    viewModel: WorkoutHubViewModel = koinViewModel(),
) {
    val routines by viewModel.routines.collectAsState()
    val hasActive by viewModel.hasActiveSession.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().larguraDeLeitura().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Text(stringResource(Res.string.workout_hub_title), style = MaterialTheme.typography.headlineSmall)
        }
        if (hasActive) {
            item {
                PrimaryButton(
                    text = stringResource(Res.string.workout_hub_resume),
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            SecondaryButton(
                text = stringResource(Res.string.workout_hub_start_empty),
                onClick = onStartEmpty,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SecondaryButton(
                text = stringResource(Res.string.workout_hub_library),
                onClick = onLibrary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SecondaryButton(
                text = stringResource(Res.string.workout_history_title),
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SecondaryButton(
                text = stringResource(Res.string.workout_stats_title),
                onClick = onStats,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SecondaryButton(
                text = stringResource(Res.string.schedule_title),
                onClick = onSchedule,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(stringResource(Res.string.workout_hub_routines), style = MaterialTheme.typography.titleMedium)
        }
        if (routines.isEmpty()) {
            item {
                Text(
                    stringResource(Res.string.workout_hub_no_routines),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(routines, key = { it.id }) { routine ->
            AntaresCard(modifier = Modifier.fillMaxWidth().clickable { onRoutine(routine.id) }) {
                Text(routine.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            val defaultName = stringResource(Res.string.workout_hub_new_routine)
            PrimaryButton(
                text = stringResource(Res.string.workout_hub_new_routine),
                onClick = { viewModel.createRoutine(defaultName, onCreated = onRoutine) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
