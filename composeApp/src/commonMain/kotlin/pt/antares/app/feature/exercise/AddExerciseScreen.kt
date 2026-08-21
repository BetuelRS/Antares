package pt.antares.app.feature.exercise

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.generated.resources.ai_describe
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.exercise.MetActivity
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun AddExerciseScreen(
    epochDay: Long,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddExerciseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    AntaresScaffold(
        topBar = {
            AntaresTopBar(title = stringResource(Res.string.exercise_add_title), onBack = onBack)
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@AntaresScaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text(stringResource(Res.string.exercise_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
            )

            SecondaryButton(
                text = "✨ " + stringResource(Res.string.ai_describe),
                onClick = viewModel::openAi,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )

            if (state.ai.open) {
                AiExerciseDialog(
                    state = state.ai,
                    onText = viewModel::setAiText,
                    onAnalyze = viewModel::analyzeAi,
                    onConfirm = { viewModel.confirmAi(epochDay) },
                    onDismiss = viewModel::closeAi,
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
            ) {
                item {
                    FilterChip(
                        selected = state.category == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text(stringResource(Res.string.exercise_cat_all)) },
                    )
                }
                items(state.categories, key = { it }) { cat ->
                    FilterChip(
                        selected = state.category == cat,
                        onClick = { viewModel.setCategory(cat) },
                        label = { Text(stringResource(categoryLabel(cat))) },
                    )
                }
            }

            ListaAdaptavel(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(),
                espaco = Spacing.xs,
            ) {
                if (state.results.isEmpty()) {
                    linhaInteira {
                        // O catálogo de METs é fixo e vem com a app: uma procura sem
                        // resultados é a palavra que não existe nele, e não uma falta de
                        // dados que se resolva registando qualquer coisa.
                        Text(
                            stringResource(Res.string.exercise_search_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.lg),
                        )
                    }
                }
                items(state.results, key = { it.id }) { activity ->
                    ActivityRow(
                        activity = activity,
                        selected = state.selected?.id == activity.id,
                        onClick = { viewModel.select(activity) },
                    )
                }
            }

            if (state.selected != null) {
                AntaresCard(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(Res.string.exercise_duration), style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.changeDuration(-5) }) {
                                Icon(Icons.Default.Remove, contentDescription = "-5")
                            }
                            Text(
                                "${state.durationMin} ${stringResource(Res.string.exercise_min)}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            IconButton(onClick = { viewModel.changeDuration(5) }) {
                                Icon(Icons.Default.Add, contentDescription = "+5")
                            }
                        }
                    }
                    Text(
                        "${stringResource(Res.string.exercise_burned)}: ${state.previewKcal} ${stringResource(Res.string.common_kcal)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.exercise_save),
                        onClick = { viewModel.save(epochDay) },
                        enabled = state.canSave,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: MetActivity, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            activity.namePt,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false).padding(end = Spacing.md),
        )
        Text(
            "MET ${activity.met}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun categoryLabel(category: String): StringResource = when (category) {
    "walking" -> Res.string.exercise_cat_walking
    "running" -> Res.string.exercise_cat_running
    "cycling" -> Res.string.exercise_cat_cycling
    "strength" -> Res.string.exercise_cat_strength
    "swimming" -> Res.string.exercise_cat_swimming
    "sports" -> Res.string.exercise_cat_sports
    "cardio" -> Res.string.exercise_cat_cardio
    "daily" -> Res.string.exercise_cat_daily
    else -> Res.string.exercise_cat_all
}
