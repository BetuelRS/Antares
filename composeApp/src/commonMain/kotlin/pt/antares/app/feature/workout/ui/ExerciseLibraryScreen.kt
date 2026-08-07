package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.feature.workout.model.Exercise
import pt.antares.app.feature.workout.model.WorkoutTaxonomy
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun ExerciseLibraryScreen(
    pickMode: Boolean,
    onExercise: (String) -> Unit,
    onCreateCustom: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExerciseLibraryViewModel = koinViewModel(),
) {
    val filters by viewModel.filters.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(if (pickMode) Res.string.exlib_pick_title else Res.string.exlib_title),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            if (!pickMode) {
                ExtendedFloatingActionButton(
                    onClick = onCreateCustom,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.exlib_create_custom)) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg)) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setQuery,
                label = { Text(stringResource(Res.string.exlib_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FilterDropdown(
                    label = Res.string.exlib_filter_muscle,
                    selected = filters.muscle,
                    options = WorkoutTaxonomy.muscles,
                    optionLabel = ::muscleLabel,
                    onSelect = viewModel::setMuscle,
                    modifier = Modifier.weight(1f),
                )
                FilterDropdown(
                    label = Res.string.exlib_filter_equipment,
                    selected = filters.equipment,
                    options = WorkoutTaxonomy.equipment,
                    optionLabel = ::equipmentLabel,
                    onSelect = viewModel::setEquipment,
                    modifier = Modifier.weight(1f),
                )
                FilterDropdown(
                    label = Res.string.exlib_filter_level,
                    selected = filters.level,
                    options = WorkoutTaxonomy.levels,
                    optionLabel = ::levelLabel,
                    onSelect = viewModel::setLevel,
                    modifier = Modifier.weight(1f),
                )
            }

            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Res.string.exlib_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    items(results, key = { it.id }) { ex ->
                        ExerciseListItem(ex, onClick = { onExercise(ex.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: StringResource,
    selected: String?,
    options: List<String>,
    optionLabel: (String) -> StringResource,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FilterChip(
            selected = selected != null,
            onClick = { open = true },
            label = {
                Text(
                    selected?.let { stringResource(optionLabel(it)) } ?: stringResource(label),
                    maxLines = 1,
                )
            },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.exlib_filter_all)) },
                onClick = { onSelect(null); open = false },
            )
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(stringResource(optionLabel(opt))) },
                    onClick = { onSelect(opt); open = false },
                )
            }
        }
    }
}

@Composable
private fun ExerciseListItem(ex: Exercise, onClick: () -> Unit) {

    val muscleText = ex.primaryMuscles.firstOrNull()?.let { stringResource(muscleLabel(it)) }
    val equipText = ex.equipment?.let { stringResource(equipmentLabel(it)) }
    val subtitle = listOfNotNull(muscleText, equipText).joinToString(" · ")

    AntaresCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AsyncImage(
                model = ex.imageUrls.firstOrNull(),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(ex.displayName, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
