package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightUnitLabel
import pt.antares.app.core.designsystem.loadWithUnit
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import kotlin.math.roundToInt
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.rememberApagarComDesfazer
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.feature.workout.data.RoutineItemView
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun RoutineEditScreen(
    routineId: String,
    onAddExercise: (String) -> Unit,
    onStart: (String) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: RoutineEditViewModel = koinViewModel(),
) {
    val detail by viewModel.detail.collectAsState()
    val deleted by viewModel.deleted.collectAsState()

    LaunchedEffect(routineId) { viewModel.start(routineId) }
    LaunchedEffect(deleted) { if (deleted) onDeleted() }

    var editItem by remember { mutableStateOf<RoutineItemView?>(null) }
    val apagar = rememberApagarComDesfazer()

    Scaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(Res.string.routine_edit_title),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            apagar(
                                { viewModel.deleteRoutine() },
                                { viewModel.restoreRoutine(routineId) },
                            )
                        },
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.routine_delete))
                    }
                },
            )
        },
    ) { padding ->
        val d = detail
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).larguraDeLeitura().padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                var name by remember(d?.routine?.id) { mutableStateOf(d?.routine?.name ?: "") }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; viewModel.rename(it) },
                    label = { Text(stringResource(Res.string.routine_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                )
            }

            val itemsList = d?.items.orEmpty()
            if (itemsList.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.routine_empty_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(itemsList, key = { it.item.id }) { row ->
                RoutineItemCard(
                    row = row,
                    onEdit = { editItem = row },
                    onMoveUp = { viewModel.move(row.item.id, up = true) },
                    onMoveDown = { viewModel.move(row.item.id, up = false) },
                    onSuperset = { g -> viewModel.setSuperset(row.item.id, g) },
                    onDelete = {
                        apagar(
                            { viewModel.deleteItem(row.item.id) },
                            { viewModel.restoreItem(row.item.id) },
                        )
                    },
                )
            }

            item {
                SecondaryButton(
                    text = stringResource(Res.string.routine_add_exercise),
                    onClick = { onAddExercise(routineId) },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                )
            }
            if (itemsList.isNotEmpty()) {
                item {
                    pt.antares.app.core.designsystem.components.PrimaryButton(
                        text = stringResource(Res.string.routine_start),
                        onClick = { onStart(routineId) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
                    )
                }
            }
        }
    }

    editItem?.let { row ->
        TargetsDialog(
            row = row,
            onSave = { sets, min, max, weight, rest ->
                viewModel.updateTargets(row.item.id, sets, min, max, weight, rest)
                editItem = null
            },
            onDismiss = { editItem = null },
        )
    }
}

@Composable
private fun RoutineItemCard(
    row: RoutineItemView,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSuperset: (Int?) -> Unit,
    onDelete: () -> Unit,
) {
    val it = row.item
    val unidades = rememberUnitSystem()
    var menu by remember { mutableStateOf(false) }
    var ssMenu by remember { mutableStateOf(false) }

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.exerciseName, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                Text(
                    "${it.targetSets}×${it.targetRepsMin}-${it.targetRepsMax} · ${it.restSec}s" +
                        (it.targetWeightKg?.let { w -> " · " + loadWithUnit(w, unidades) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                it.supersetGroup?.let { g ->
                    AssistChip(
                        onClick = { ssMenu = true },
                        label = { Text(stringResource(Res.string.routine_superset_group, g)) },
                    )
                }
            }
            IconButton(onClick = onMoveUp) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(Res.string.routine_move_up)) }
            IconButton(onClick = onMoveDown) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(Res.string.routine_move_down)) }
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.cd_more_options)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.common_edit)) }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text(stringResource(Res.string.routine_superset)) }, onClick = { menu = false; ssMenu = true })
                DropdownMenuItem(text = { Text(stringResource(Res.string.common_delete)) }, onClick = { menu = false; onDelete() })
            }
            DropdownMenu(expanded = ssMenu, onDismissRequest = { ssMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.routine_superset_none)) },
                    onClick = { ssMenu = false; onSuperset(null) },
                )
                (1..3).forEach { g ->
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.routine_superset_group, g)) },
                        onClick = { ssMenu = false; onSuperset(g) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetsDialog(
    row: RoutineItemView,
    onSave: (Int, Int, Int, Double?, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val it = row.item
    var sets by remember { mutableStateOf(it.targetSets.toString()) }
    var min by remember { mutableStateOf(it.targetRepsMin.toString()) }
    var max by remember { mutableStateOf(it.targetRepsMax.toString()) }
    var rest by remember { mutableStateOf(it.restSec.toString()) }

    // O alvo é escrito e lido na unidade escolhida, e guardado sempre em quilos.
    val unidades = rememberUnitSystem()
    var weight by remember {
        mutableStateOf(
            it.targetWeightKg?.let { kg ->
                val v = UnitConversions.weightToDisplay(kg, unidades)
                ((v * 10).roundToInt() / 10.0).toString()
            } ?: "",
        )
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.exerciseName, maxLines = 2) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                NumField(sets, { sets = it }, Res.string.routine_sets)
                NumField(min, { min = it }, Res.string.routine_reps_min)
                NumField(max, { max = it }, Res.string.routine_reps_max)
                NumField(rest, { rest = it }, Res.string.routine_rest_sec)
                NumField(
                    value = weight,
                    onChange = { weight = it },
                    label = Res.string.routine_weight_optional,
                    labelArg = stringResource(weightUnitLabel(unidades)),
                    decimal = true,
                )
            }
        },
        confirmButton = {
            pt.antares.app.core.designsystem.components.PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = {
                    onSave(
                        sets.toIntOrNull() ?: it.targetSets,
                        min.toIntOrNull() ?: it.targetRepsMin,
                        max.toIntOrNull() ?: it.targetRepsMax,
                        weight.replace(',', '.').toDoubleOrNull()?.let { v ->
                            if (unidades == UnitSystem.IMPERIAL) UnitConversions.lbToKg(v) else v
                        },
                        rest.toIntOrNull() ?: it.restSec,
                    )
                },
            )
        },
        dismissButton = {
            SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss)
        },
    )
}

@Composable
private fun NumField(
    value: String,
    onChange: (String) -> Unit,
    label: org.jetbrains.compose.resources.StringResource,
    decimal: Boolean = false,
    labelArg: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(5)) },
        label = { Text(if (labelArg == null) stringResource(label) else stringResource(label, labelArg)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
