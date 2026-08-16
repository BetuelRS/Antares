package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightUnitLabel
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun WorkoutDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = koinViewModel(),
) {
    val breakdown by viewModel.breakdown.collectAsState()
    val unidades = rememberUnitSystem()
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.workout_detail_title), onBack = onBack) },
    ) { padding ->
        val b = breakdown
        if (b == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${b.durationMin} min · ${weightWithUnit(b.volume, unidades)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            items(b.exercises, key = { it.id }) { ex ->
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(ex.name, style = MaterialTheme.typography.titleSmall)
                    ex.sets.forEachIndexed { i, set ->
                        Text(
                            "${i + 1}. ${weightWithUnit(set.weightKg, unidades)} × ${set.reps}" +
                                (if (set.isWarmup) " · ${stringResource(Res.string.session_warmup)}" else ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
