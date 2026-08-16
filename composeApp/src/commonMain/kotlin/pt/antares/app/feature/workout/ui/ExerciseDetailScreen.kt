package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    Scaffold(
        topBar = {
            AntaresTopBar(
                title = state.exercise?.displayName ?: "",
                onBack = onBack,
                actions = {
                    if (state.exercise?.isCustom == true) {
                        IconButton(onClick = viewModel::deleteCustom) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.excustom_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val ex = state.exercise
        if (state.loading || ex == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (ex.imageUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(ex.imageUrls, key = { it }) { url ->
                        ExerciseImage(
                            url = url,
                            exerciseName = ex.displayName,
                            modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1.4f).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            }

            val meta = listOfNotNull(
                stringResource(categoryLabel(ex.category)),
                ex.equipment?.let { stringResource(equipmentLabel(it)) },
                stringResource(levelLabel(ex.level)),
            ).joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (ex.primaryMuscles.isNotEmpty()) {
                MuscleRow(Res.string.exdetail_primary, ex.primaryMuscles)
            }
            if (ex.secondaryMuscles.isNotEmpty()) {
                MuscleRow(Res.string.exdetail_secondary, ex.secondaryMuscles)
            }

            if (state.progress.size >= 2) {
                Text(stringResource(Res.string.exdetail_progress), style = MaterialTheme.typography.titleMedium)
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    pt.antares.app.core.designsystem.components.Sparkline(values = state.progress)
                }
            }

            Text(stringResource(Res.string.exdetail_instructions), style = MaterialTheme.typography.titleMedium)
            if (ex.instructionsUntranslated) {
                Text(
                    stringResource(Res.string.exdetail_untranslated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (ex.instructions.isEmpty()) {
                Text(stringResource(Res.string.exdetail_no_instructions), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                ex.instructions.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MuscleRow(label: org.jetbrains.compose.resources.StringResource, muscles: List<String>) {

    val names = muscles.map { stringResource(muscleLabel(it)) }.joinToString(", ")
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(names, style = MaterialTheme.typography.bodyLarge)
    }
}
