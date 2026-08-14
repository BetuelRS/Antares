package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.SetLimits
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun WorkoutSessionScreen(
    routineId: String?,
    onAddExercise: () -> Unit,
    onFinished: (String) -> Unit,
    onDiscarded: () -> Unit,
    viewModel: WorkoutSessionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val exit by viewModel.exit.collectAsState()
    val restRemaining by viewModel.restRemaining.collectAsState()

    val requestNotifications = rememberNotificationPermissionRequester()
    LaunchedEffect(Unit) { requestNotifications() }

    LaunchedEffect(routineId) { viewModel.ensureStarted(routineId) }
    LaunchedEffect(exit.finishedSessionId) { exit.finishedSessionId?.let(onFinished) }
    LaunchedEffect(exit.discarded) { if (exit.discarded) onDiscarded() }

    var confirmDiscard by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(Res.string.session_title),
                onBack = { confirmDiscard = true },
                actions = {
                    TextButton(onClick = viewModel::finish) { Text(stringResource(Res.string.session_finish)) }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {

            restRemaining?.let { secs ->
                AntaresCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.session_rest_running, secs),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::skipRest) {
                            Text(stringResource(Res.string.session_rest_skip))
                        }
                    }
                }
            }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.exercises.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.session_no_exercises),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.exercises, key = { it.exerciseId }) { ex ->
                ExerciseBlock(
                    ex = ex,
                    onLog = { w, r, rpe, warm -> viewModel.logSet(ex, w, r, rpe, warm) },
                    onDeleteSet = viewModel::deleteSet,
                )
            }
            item {
                SecondaryButton(
                    text = stringResource(Res.string.session_add_exercise),
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                PrimaryButton(
                    text = stringResource(Res.string.session_finish),
                    onClick = viewModel::finish,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl),
                )
            }
        }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(Res.string.session_discard)) },
            text = { Text(stringResource(Res.string.session_discard_confirm)) },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(Res.string.session_discard),
                    onClick = { confirmDiscard = false; viewModel.discard() },
                )
            },
            dismissButton = {
                SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = { confirmDiscard = false })
            },
        )
    }
}

@Composable
private fun ExerciseBlock(
    ex: SessionExerciseUi,
    onLog: (Double, Int, Double?, Boolean) -> Unit,
    onDeleteSet: (String) -> Unit,
) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ex.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2)
            ex.supersetGroup?.let { g ->
                AssistChip(onClick = {}, label = { Text(stringResource(Res.string.routine_superset_group, g)) })
            }
        }
        Text(
            "${ex.targetSets}×${ex.repsMin}-${ex.repsMax} · ${ex.restSec}s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (ex.ghost.isNotEmpty()) {
            val g = ex.ghost.joinToString("  ") { "${it.weightKg.toInt()}×${it.reps}" }
            Text(
                stringResource(Res.string.session_ghost, g),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ex.sets.forEachIndexed { i, set ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("${i + 1}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${set.weightKg.toInt()} ${stringResource(Res.string.session_weight)} × ${set.reps} ${stringResource(Res.string.session_reps)}" +
                        (if (set.isWarmup) " · ${stringResource(Res.string.session_warmup)}" else ""),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = { onDeleteSet(set.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.common_delete))
                }
            }
        }

        NewSetRow(prefill = ex.ghost.getOrNull(ex.sets.size), onLog = onLog)
    }
}

@Composable
private fun NewSetRow(prefill: WorkoutSetEntity?, onLog: (Double, Int, Double?, Boolean) -> Unit) {
    var weight by remember(prefill) { mutableStateOf(prefill?.weightKg?.let { it.toInt().toString() } ?: "") }
    var reps by remember(prefill) { mutableStateOf(prefill?.reps?.toString() ?: "") }
    var rpe by remember { mutableStateOf("") }
    var warmup by remember { mutableStateOf(false) }

    val w = weight.replace(',', '.').toDoubleOrNull()
    val r = reps.toIntOrNull()
    val rp = rpe.replace(',', '.').toDoubleOrNull()

    // Só marcamos erro no que a pessoa já escreveu: um campo ainda vazio não é
    // um erro, é um campo por preencher.
    val pesoMau = weight.isNotBlank() && (w == null || !SetLimits.isWeightValid(w))
    val repsMau = reps.isNotBlank() && (r == null || !SetLimits.isRepsValid(r))
    val rpeMau = rpe.isNotBlank() && !SetLimits.isRpeValid(rp)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
            label = { Text(stringResource(Res.string.session_weight)) },
            singleLine = true,
            isError = pesoMau,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(96.dp),
        )
        OutlinedTextField(
            value = reps,
            onValueChange = { reps = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(Res.string.session_reps)) },
            singleLine = true,
            isError = repsMau,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(80.dp),
        )
        OutlinedTextField(
            value = rpe,
            onValueChange = { rpe = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(4) },
            label = { Text(stringResource(Res.string.session_rpe)) },
            singleLine = true,
            isError = rpeMau,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(72.dp),
        )
        val podeGravar = SetLimits.isSetValid(w, r, rp)
        IconButton(
            onClick = {
                if (podeGravar) {
                    onLog(w!!, r!!, rp, warmup)
                    rpe = ""
                }
            },
            enabled = podeGravar,
        ) {
            Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.session_add_set))
        }
    }

    // Dizer porquê, em vez de deixar o botão cinzento sem explicação.
    if (pesoMau || repsMau || rpeMau) {
        Text(
            text = when {
                pesoMau -> stringResource(
                    Res.string.session_weight_out_of_range,
                    SetLimits.MAX_WEIGHT_KG.toInt().toString(),
                )
                repsMau -> stringResource(
                    Res.string.session_reps_out_of_range,
                    SetLimits.MAX_REPS.toString(),
                )
                else -> stringResource(Res.string.session_rpe_out_of_range)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    FilterChip(
        selected = warmup,
        onClick = { warmup = !warmup },
        label = { Text(stringResource(Res.string.session_warmup)) },
    )
}
