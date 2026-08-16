package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightUnitLabel
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun WorkoutSummaryScreen(
    sessionId: String,
    onDone: () -> Unit,
    viewModel: WorkoutSummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.workout_summary_title)) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Stat(stringResource(Res.string.workout_summary_duration), "${state.durationMin} min")
                Stat(stringResource(Res.string.workout_summary_volume), weightWithUnit(state.volume, unidades))
                Stat(stringResource(Res.string.workout_summary_sets), "${state.setCount}")
            }

            Text(stringResource(Res.string.workout_summary_prs), style = MaterialTheme.typography.titleMedium)
            if (state.prLabels.isEmpty()) {
                Text(
                    stringResource(Res.string.workout_summary_no_prs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.prLabels.forEach { label ->
                    Text("🌟 ${stringResource(Res.string.workout_summary_pr_line, label)}")
                }
            }

            PrimaryButton(
                text = stringResource(Res.string.workout_summary_done),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
