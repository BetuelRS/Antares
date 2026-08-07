package pt.antares.app.feature.running.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_acquiring_gps
import pt.antares.app.generated.resources.run_acquiring_hint
import pt.antares.app.generated.resources.run_goal_reached
import pt.antares.app.generated.resources.run_live_distance
import pt.antares.app.generated.resources.run_live_finish
import pt.antares.app.generated.resources.run_live_kcal
import pt.antares.app.generated.resources.run_live_lock
import pt.antares.app.generated.resources.run_live_pace_avg
import pt.antares.app.generated.resources.run_live_pace_cur
import pt.antares.app.generated.resources.run_live_paused
import pt.antares.app.generated.resources.run_live_time
import pt.antares.app.generated.resources.run_live_unlock
import pt.antares.app.generated.resources.run_unit_km

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RunLiveScreen(
    onFinish: () -> Unit,
    viewModel: RunViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val m = state.metrics
    val goalType by viewModel.goalType.collectAsState()
    val goalValue by viewModel.goalValue.collectAsState()
    var locked by remember { mutableStateOf(false) }

    val goalFraction: Float? = when (goalType) {
        RunGoalType.DISTANCE -> if (goalValue > 0) (m.distanceM / goalValue).toFloat().coerceIn(0f, 1f) else null
        RunGoalType.TIME -> if (goalValue > 0) (m.movingMs / (goalValue * 1000.0)).toFloat().coerceIn(0f, 1f) else null
        RunGoalType.NONE -> null
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            RunMap(
                path = state.path,
                modifier = Modifier.fillMaxSize(),
                follow = true,
            )
            if (state.active && !state.hasFix) {
                AcquiringGpsOverlay()
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            if (m.paused) {
                Text(
                    stringResource(Res.string.run_live_paused),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                "${RunFormat.km(m.distanceM)} ${stringResource(Res.string.run_unit_km)}",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.run_live_distance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            goalFraction?.let { frac ->
                Spacer(Modifier.height(Spacing.sm))
                LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
                if (frac >= 1f) {
                    Text(
                        stringResource(Res.string.run_goal_reached),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Metric(RunFormat.clock(m.movingMs), stringResource(Res.string.run_live_time))
                Metric(RunFormat.pace(m.avgPaceSecPerKm), stringResource(Res.string.run_live_pace_avg))
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Metric(RunFormat.paceFromSpeed(m.curSpeedMps), stringResource(Res.string.run_live_pace_cur))
                Metric("${m.kcal}", stringResource(Res.string.run_live_kcal))
            }
            Spacer(Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                IconButton(onClick = { locked = !locked }) {
                    Icon(
                        if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = stringResource(if (locked) Res.string.run_live_unlock else Res.string.run_live_lock),
                    )
                }

                Surface(
                    color = if (locked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .combinedClickable(
                            enabled = !locked,
                            onClick = {},
                            onLongClick = { viewModel.finish(); onFinish() },
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(Res.string.run_live_finish),
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AcquiringGpsOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.xl),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            Text(
                stringResource(Res.string.run_acquiring_gps),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.run_acquiring_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
