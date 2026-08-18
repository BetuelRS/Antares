package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.SetLimits
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.trimmedDecimal
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightUnitLabel
import pt.antares.app.core.designsystem.loadWithUnit
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.rememberApagarComDesfazer
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
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
    val apagar = rememberApagarComDesfazer()

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
            modifier = Modifier.fillMaxSize().larguraDeLeitura().padding(horizontal = Spacing.lg),
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
                if (ex.exerciseId == state.currentExerciseId) {
                    ExerciseBlock(
                        ex = ex,
                        onLog = { w, r, rpe, warm -> viewModel.logSet(ex, w, r, rpe, warm) },
                        onDeleteSet = { id ->
                            apagar({ viewModel.deleteSet(id) }, { viewModel.restoreSet(id) })
                        },
                        onEditSet = { set, peso, reps -> viewModel.updateSet(set, peso, reps) },
                    )
                } else {
                    ExerciseRecolhido(ex = ex, onSelect = { viewModel.select(ex.exerciseId) })
                }
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

/**
 * O exercício que está a ser feito. Ocupa o ecrã inteiro porque durante um treino a pessoa
 * está num exercício, e não nos quatro do plano ao mesmo tempo.
 */
@Composable
private fun ExerciseBlock(
    ex: SessionExerciseUi,
    onLog: (Double, Int, Double?, Boolean) -> Unit,
    onDeleteSet: (String) -> Unit,
    onEditSet: (WorkoutSetEntity, Double, Int) -> Unit,
) {
    var warmup by remember(ex.exerciseId) { mutableStateOf(false) }
    val unidades = rememberUnitSystem()

    // Qual das séries já gravadas está aberta para corrigir. Nula quase sempre.
    var aCorrigir by remember(ex.exerciseId) { mutableStateOf<WorkoutSetEntity?>(null) }

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ex.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2)
            ex.supersetGroup?.let { g ->
                AssistChip(onClick = {}, label = { Text(stringResource(Res.string.routine_superset_group, g)) })
            }
        }
        // O aquecimento partilha a linha do alvo em vez de ocupar uma só para ele: era uma
        // linha inteira por exercício, num ecrã onde o que interessa é a série a seguir.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${ex.targetSets}×${ex.repsMin}-${ex.repsMax} · ${ex.restSec}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = warmup,
                onClick = { warmup = !warmup },
                label = { Text(stringResource(Res.string.session_warmup)) },
            )
        }

        ex.sets.forEachIndexed { i, set ->
            Row(
                // A linha inteira abre a correção. Uma série gravada com o peso errado só
                // se resolvia apagando e refazendo, e a fila de descanso recomeçava.
                modifier = Modifier.clickable(role = Role.Button) { aCorrigir = set },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text("${i + 1}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${loadWithUnit(set.weightKg, unidades)} × ${set.reps} " +
                        stringResource(Res.string.session_reps) +
                        (if (set.isWarmup) " · ${stringResource(Res.string.session_warmup)}" else ""),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = { onDeleteSet(set.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.common_delete))
                }
            }
        }

        SeriesFantasma(ex, unidades)

        NewSetRow(
            prefill = ex.ghost.getOrNull(ex.setsDone),
            warmup = warmup,
            unidades = unidades,
            onLog = onLog,
        )
    }

    aCorrigir?.let { set ->
        CorrigirSerieDialog(
            set = set,
            unidades = unidades,
            onDismiss = { aCorrigir = null },
            onConfirm = { peso, reps ->
                onEditSet(set, peso, reps)
                aCorrigir = null
            },
        )
    }
}

/**
 * Corrigir uma série já gravada. Mexe no peso e nas repetições e mais nada: o RPE e o
 * aquecimento são o que se sentiu na altura, e mudá-los depois seria reescrever a memória
 * do treino em vez de corrigir um erro de digitação.
 */
@Composable
private fun CorrigirSerieDialog(
    set: WorkoutSetEntity,
    unidades: UnitSystem,
    onDismiss: () -> Unit,
    onConfirm: (Double, Int) -> Unit,
) {
    val virgula = virgulaDecimal()
    var peso by remember(set.id) {
        mutableStateOf(trimmedDecimal(UnitConversions.weightToDisplay(set.weightKg, unidades), comma = virgula))
    }
    var reps by remember(set.id) { mutableStateOf(set.reps.toString()) }

    val emKg = peso.replace(',', '.').toDoubleOrNull()?.let {
        if (unidades == UnitSystem.IMPERIAL) UnitConversions.lbToKg(it) else it
    }
    val r = reps.toIntOrNull()
    val pesoMau = peso.isNotBlank() && (emKg == null || !SetLimits.isWeightValid(emKg))
    val repsMau = reps.isNotBlank() && (r == null || !SetLimits.isRepsValid(r))

    // O RPE gravado passa tal e qual pela validação: é ele que fica, e recusar a correção
    // por causa de um valor que não se está a tocar não faria sentido nenhum.
    val podeGravar = SetLimits.isSetValid(emKg, r, set.rpe)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.session_edit_set)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { peso = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
                        label = { Text(stringResource(weightUnitLabel(unidades))) },
                        singleLine = true,
                        isError = pesoMau,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(112.dp),
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(Res.string.session_reps)) },
                        singleLine = true,
                        isError = repsMau,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(96.dp),
                    )
                }
                ErroDaSerie(pesoMau = pesoMau, repsMau = repsMau, rpeMau = false, unidades = unidades)
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = { if (podeGravar) onConfirm(emKg!!, r!!) },
                enabled = podeGravar,
            )
        },
        dismissButton = {
            SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss)
        },
    )
}

/**
 * As séries que faltam ao plano, a cinzento e por cima da linha de gravar: o que a pessoa fez
 * da última vez, no sítio onde vai escrever o que faz hoje. Sem histórico fica o alvo da
 * rotina, que é a única outra coisa que se sabe sobre uma série que ainda não aconteceu.
 */
@Composable
private fun SeriesFantasma(ex: SessionExerciseUi, unidades: UnitSystem) {
    val emFalta = ex.targetSets - ex.setsDone
    if (emFalta <= 0) return

    val reps = stringResource(Res.string.session_reps)
    repeat(emFalta) { k ->
        val fantasma = ex.ghost.getOrNull(ex.setsDone + k)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                "${ex.sets.size + k + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (fantasma != null) {
                    "${loadWithUnit(fantasma.weightKg, unidades)} × ${fantasma.reps} $reps"
                } else {
                    "${ex.repsMin}-${ex.repsMax} $reps"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Os exercícios que não estão a ser feitos: nome e progresso, e um toque para trocar. */
@Composable
private fun ExerciseRecolhido(ex: SessionExerciseUi, onSelect: () -> Unit) {
    AntaresCard(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onSelect),
        contentPadding = PaddingValues(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ex.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                stringResource(Res.string.session_sets_progress, ex.setsDone, ex.targetSets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ex.isComplete) {
                Icon(
                    Icons.Default.Check,
                    // O visto é o que separa «3/3 feitas» de «3/3 e acabou»: o número
                    // sozinho não diz que o exercício está fechado.
                    contentDescription = stringResource(Res.string.session_exercise_done),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun NewSetRow(
    prefill: WorkoutSetEntity?,
    warmup: Boolean,
    unidades: UnitSystem,
    onLog: (Double, Int, Double?, Boolean) -> Unit,
) {
    // O que se escreve está na unidade escolhida; o que se grava está sempre em quilos. A
    // volta e meia acontece aqui e em mais lado nenhum — a base nunca vê libras.
    val paraKg = { valor: Double ->
        if (unidades == UnitSystem.IMPERIAL) UnitConversions.lbToKg(valor) else valor
    }

    // O peso da série anterior volta como estava, com decimal e tudo: arredondá-lo a
    // inteiro fazia a app mudar em silêncio um número que a pessoa registou, e 62,5 kg
    // reapareciam como 63.
    val virgula = virgulaDecimal()
    var weight by remember(prefill, unidades, virgula) {
        mutableStateOf(
            prefill?.weightKg?.let {
                trimmedDecimal(UnitConversions.weightToDisplay(it, unidades), comma = virgula)
            } ?: "",
        )
    }
    var reps by remember(prefill) { mutableStateOf(prefill?.reps?.toString() ?: "") }
    var rpe by remember { mutableStateOf("") }

    val w = weight.replace(',', '.').toDoubleOrNull()?.let(paraKg)
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
            label = { Text(stringResource(weightUnitLabel(unidades))) },
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

    ErroDaSerie(pesoMau = pesoMau, repsMau = repsMau, rpeMau = rpeMau, unidades = unidades)
}

/** Dizer porquê, em vez de deixar o botão cinzento sem explicação. */
@Composable
private fun ErroDaSerie(pesoMau: Boolean, repsMau: Boolean, rpeMau: Boolean, unidades: UnitSystem) {
    if (!pesoMau && !repsMau && !rpeMau) return
    Text(
        text = when {
            // O teto é em quilos; dizê-lo em quilos a quem escreve em libras seria pedir uma
            // conta de cabeça para perceber o próprio erro.
            pesoMau -> stringResource(
                Res.string.session_weight_out_of_range,
                loadWithUnit(SetLimits.MAX_WEIGHT_KG, unidades),
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
