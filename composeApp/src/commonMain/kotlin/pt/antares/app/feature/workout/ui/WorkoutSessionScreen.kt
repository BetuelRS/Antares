package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.CargaDoCorpo
import pt.antares.app.core.calc.PlateMath
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
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import pt.antares.app.feature.running.ui.RunFormat
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
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

    // A permissão pede-se quando o descanso arranca pela primeira vez, e não à entrada.
    //
    // Era `LaunchedEffect(Unit)`: a caixa do sistema aparecia por cima do ecrã antes de a
    // pessoa ter visto o treino, e a pergunta chegava sem nada que a explicasse. O
    // `estudo/areas/08-treino-sessao.md` nomeia-o (defeito 5) e diz qual é o padrão certo.
    // Agora há contexto: acabou de gravar uma série, há uma contagem a andar, e a
    // notificação serve para essa contagem chegar com a app fechada.
    val requestNotifications = rememberNotificationPermissionRequester()
    var pedidaPeloDescanso by remember { mutableStateOf(false) }
    LaunchedEffect(restRemaining != null) {
        if (restRemaining != null && !pedidaPeloDescanso) {
            pedidaPeloDescanso = true
            requestNotifications()
        }
    }

    LaunchedEffect(routineId) { viewModel.ensureStarted(routineId) }
    LaunchedEffect(exit.finishedSessionId) { exit.finishedSessionId?.let(onFinished) }
    LaunchedEffect(exit.discarded) { if (exit.discarded) onDiscarded() }

    var confirmDiscard by remember { mutableStateOf(false) }
    val apagar = rememberApagarComDesfazer()

    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                // O nome da rotina, e não a palavra «Treino»: quem está a meio de um treino
                // sabe que está num treino, e o título era a única linha do ecrã que não
                // dizia nada. Sem rotina — um treino livre — fica a palavra.
                title = state.routineName ?: stringResource(Res.string.session_title),
                onBack = { confirmDiscard = true },
                actions = {
                    state.startedAt?.let { RelogioDaSessao(it) }
                    TextButton(onClick = viewModel::finish) { Text(stringResource(Res.string.session_finish)) }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@AntaresScaffold
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
                        pesoDoCorpoKg = state.pesoDoCorpoKg,
                        ex = ex,
                        onLog = { w, r, warm, corpo -> viewModel.logSet(ex, w, r, warm, corpo) },
                        onDeleteSet = { id ->
                            apagar({ viewModel.deleteSet(id) }, { viewModel.restoreSet(id) })
                        },
                        onEditSet = { set, peso, reps -> viewModel.updateSet(set, peso, reps) },
                        onRpe = { set, valor -> viewModel.updateRpe(set, valor) },
                        onNote = { texto -> viewModel.saveNote(ex.exerciseId, texto) },
                        onPercentagem = { pct -> viewModel.savePercentagem(ex.exerciseId, pct) },
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
    pesoDoCorpoKg: Double?,
    onLog: (Double, Int, Boolean, Double?) -> Unit,
    onDeleteSet: (String) -> Unit,
    onEditSet: (WorkoutSetEntity, Double, Int) -> Unit,
    onRpe: (WorkoutSetEntity, Double?) -> Unit,
    onNote: (String) -> Unit,
    onPercentagem: (Int) -> Unit,
) {
    var warmup by remember(ex.exerciseId) { mutableStateOf(false) }
    val unidades = rememberUnitSystem()

    // Qual das séries já gravadas está aberta para corrigir. Nula quase sempre.
    var aCorrigir by remember(ex.exerciseId) { mutableStateOf<WorkoutSetEntity?>(null) }
    var rpeDe by remember(ex.exerciseId) { mutableStateOf<WorkoutSetEntity?>(null) }
    var aEscreverNota by remember(ex.exerciseId) { mutableStateOf(false) }

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ex.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2)
            if (ex.recordeHoje) EtiquetaDoRecorde()
            ex.supersetGroup?.let { EtiquetaDeSuperserie(it) }
        }
        LinhaDoAlvo(ex = ex, unidades = unidades, warmup = warmup, onWarmup = { warmup = !warmup })

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
                        // Dizer de onde veio a carga: sem isto, uma flexão aparecia como uma
                        // série de 78 kg e ninguém sabia que os 78 eram a própria pessoa.
                        (
                            if (set.bodyweightKg != null) {
                                " · ${stringResource(Res.string.session_set_bodyweight)}"
                            } else {
                                ""
                            }
                            ) +
                        (if (set.isWarmup) " · ${stringResource(Res.string.session_warmup)}" else ""),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                // O RPE só aparece quando existe. Era um campo permanente para um número que
                // a maioria não escreve, e roubava largura ao peso e às reps.
                set.rpe?.let {
                    Text(
                        "${stringResource(Res.string.session_rpe)} ${trimmedDecimal(it, comma = virgulaDecimal())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MenuDaSerie(
                    temRpe = set.rpe != null,
                    onRpe = { rpeDe = set },
                    onLimparRpe = { onRpe(set, null) },
                    onApagar = { onDeleteSet(set.id) },
                )
            }
        }

        SeriesFantasma(ex, unidades)

        if (ex.dePesoDoCorpo) {
            LinhaDoPesoDoCorpo(
                ex = ex,
                pesoDoCorpoKg = pesoDoCorpoKg,
                unidades = unidades,
                onLog = onLog,
                onPercentagem = onPercentagem,
            )
        } else {
            NewSetRow(
                prefill = ex.ghost.getOrNull(ex.setsDone),
                warmup = warmup,
                unidades = unidades,
                comBarra = ex.equipamento == EQUIPAMENTO_BARRA,
                onLog = { peso, reps, aquecimento -> onLog(peso, reps, aquecimento, null) },
            )
        }

        NotaDoExercicio(nota = ex.nota, onClick = { aEscreverNota = true })
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

    rpeDe?.let { set ->
        RpeDialog(
            atual = set.rpe,
            onDismiss = { rpeDe = null },
            onConfirm = { valor ->
                onRpe(set, valor)
                rpeDe = null
            },
        )
    }

    if (aEscreverNota) {
        NotaDialog(
            atual = ex.nota,
            onDismiss = { aEscreverNota = false },
            onConfirm = { texto ->
                onNote(texto)
                aEscreverNota = false
            },
        )
    }
}

/**
 * O alvo do exercício, o 1RM estimado e o interruptor de aquecimento, numa linha só.
 *
 * O aquecimento partilha a linha em vez de ocupar uma para ele: era uma linha inteira por
 * exercício, num ecrã onde o que interessa é a série a seguir. E o 1RM fica ao lado do alvo,
 * e não num ecrã à parte, porque é o número com que se decide o peso da série seguinte — e
 * essa decisão toma-se aqui.
 */
@Composable
private fun LinhaDoAlvo(
    ex: SessionExerciseUi,
    unidades: UnitSystem,
    warmup: Boolean,
    onWarmup: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${ex.targetSets}×${ex.repsMin}-${ex.repsMax} · ${ex.restSec}s" +
                (if (ex.melhorOneRmKg != null) " · " else ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ex.melhorOneRmKg?.let {
            Text(
                stringResource(Res.string.session_one_rm, loadWithUnit(it, unidades)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } ?: Spacer(Modifier.weight(1f))

        // O aquecimento não aparece nos exercícios de peso do corpo: a linha deles não tem
        // campo de peso para aquecer com menos, e um interruptor que só muda uma marca no
        // histórico é ruído no ecrã mais apertado da app.
        if (!ex.dePesoDoCorpo) {
            FilterChip(
                selected = warmup,
                onClick = onWarmup,
                label = { Text(stringResource(Res.string.session_warmup)) },
            )
        }
    }
}

/**
 * O relógio do treino, na barra do topo.

 *
 * Ao segundo e não ao minuto: entre séries olha-se para ele a contar, e um número que muda de
 * minuto a minuto parece parado. A conta é feita a partir do instante de início gravado na
 * sessão, e por isso sobrevive a fechar a app — não é um cronómetro que arranca ao abrir o
 * ecrã, que era o que perdia o tempo de quem saiu para atender uma chamada.
 */
@Composable
private fun RelogioDaSessao(startedAt: Long) {
    var agora by remember(startedAt) { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(startedAt) {
        while (true) {
            delay(1000)
            agora = Clock.System.now().toEpochMilliseconds()
        }
    }
    val texto = RunFormat.clock((agora - startedAt).coerceAtLeast(0))
    val descricao = stringResource(Res.string.session_clock, texto)
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        // Sem isto o leitor de ecrã diz «trinta e oito, catorze», que não é nada.
        modifier = Modifier.semantics { contentDescription = descricao },
    )
}

/**
 * A supersérie é uma **etiqueta**, e não um chip.
 *
 * Era um `AssistChip(onClick = {})`: parecia tocável, era tocável, e não fazia nada. Um alvo
 * de toque que não responde é pior do que não haver alvo nenhum — a pessoa toca, não acontece
 * nada, e fica a achar que a app falhou.
 */
@Composable
private fun EtiquetaDeSuperserie(grupo: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            stringResource(Res.string.routine_superset_group, grupo),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

/** O recorde dito no momento em que acontece, e não só no resumo do fim. */
@Composable
private fun EtiquetaDoRecorde() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            stringResource(Res.string.session_pr_now),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

/** O que era raro na linha da série: o RPE e o apagar. */
@Composable
private fun MenuDaSerie(
    temRpe: Boolean,
    onRpe: () -> Unit,
    onLimparRpe: () -> Unit,
    onApagar: () -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { aberto = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.session_set_menu))
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.session_rpe)) },
                onClick = { aberto = false; onRpe() },
            )
            if (temRpe) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.session_rpe_clear)) },
                    onClick = { aberto = false; onLimparRpe() },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.common_delete)) },
                onClick = { aberto = false; onApagar() },
            )
        }
    }
}

/** O RPE de uma série, escrito depois de ela estar gravada. */
@Composable
private fun RpeDialog(atual: Double?, onDismiss: () -> Unit, onConfirm: (Double?) -> Unit) {
    val virgula = virgulaDecimal()
    var texto by remember { mutableStateOf(atual?.let { trimmedDecimal(it, comma = virgula) } ?: "") }
    val valor = texto.replace(',', '.').toDoubleOrNull()
    val mau = texto.isNotBlank() && !SetLimits.isRpeValid(valor)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.session_rpe)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(4) },
                    label = { Text(stringResource(Res.string.session_rpe)) },
                    singleLine = true,
                    isError = mau,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (mau) {
                    Text(
                        stringResource(Res.string.session_rpe_out_of_range),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                enabled = !mau,
                onClick = { onConfirm(valor.takeIf { texto.isNotBlank() }) },
            )
        },
        dismissButton = { SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss) },
    )
}

/**
 * A nota deste exercício neste treino.
 *
 * Sem nota é um convite de uma linha, e não um cartão vazio: um espaço permanente a dizer que
 * não há nada escrito é o defeito que a área 01 nomeia nos cartões do «Hoje».
 */
@Composable
private fun NotaDoExercicio(nota: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.StickyNote2,
            // Decorativo: a linha inteira é o alvo, e o texto ao lado dela já diz o que faz
            // — «Acrescentar uma nota», ou a nota que estiver escrita.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            nota.ifBlank { stringResource(Res.string.session_note_add) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NotaDialog(atual: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var texto by remember { mutableStateOf(atual) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.session_note_label)) },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.take(MAX_NOTA) },
                label = { Text(stringResource(Res.string.session_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            PrimaryButton(text = stringResource(Res.string.common_save), onClick = { onConfirm(texto) })
        },
        dismissButton = { SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss) },
    )
}

private const val MAX_NOTA = 280

/** O valor que o catálogo de exercícios usa para a barra. Ver `WorkoutTaxonomy`. */
private const val EQUIPAMENTO_BARRA = "barbell"

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
                        // Fixa, e **não aguenta letra grande** — o mesmo caso do `NewSetRow`
                        // aqui em baixo. Fica para a 2.21.0, que é quem mexe nesta sessão.
                        modifier = Modifier.width(112.dp),
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(Res.string.session_reps)) },
                        singleLine = true,
                        isError = repsMau,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        // Idem: par do campo de cima, e cai com ele.
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
    comBarra: Boolean,
    onLog: (Double, Int, Boolean) -> Unit,
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

    val noDisplay = weight.replace(',', '.').toDoubleOrNull()
    val w = noDisplay?.let(paraKg)
    val r = reps.toIntOrNull()

    // Só marcamos erro no que a pessoa já escreveu: um campo ainda vazio não é
    // um erro, é um campo por preencher.
    val pesoMau = weight.isNotBlank() && (w == null || !SetLimits.isWeightValid(w))
    val repsMau = reps.isNotBlank() && (r == null || !SetLimits.isRepsValid(r))
    val podeGravar = SetLimits.isSetValid(w, r, null)

    val teclado = LocalSoftwareKeyboardController.current
    val repsFocus = remember { FocusRequester() }

    // Dois campos a dividir a linha, e não três larguras fixas de 96, 80 e 72 dp: era o caso
    // que o `transversal/03-acessibilidade.md` §3.1 nomeia com estes números — «a 200 % os
    // rótulos não cabem». A saída do RPE devolveu um terço da linha aos dois que sobram.
    //
    // **`Row` e não `FlowRow`:** num `FlowRow`, o `weight` manda o item encher o que resta da
    // linha, e por isso o primeiro campo ocupava-a toda e o segundo caía para baixo — visto no
    // aparelho. Com dois campos a meias, a conta é a mesma em qualquer largura.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
            label = { Text(stringResource(weightUnitLabel(unidades))) },
            singleLine = true,
            isError = pesoMau,
            // Peso → reps → gravar, sem tirar a mão do teclado. Eram dois toques por série só
            // para mudar de campo, e numa sessão de vinte séries são quarenta toques.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { repsFocus.requestFocus() }),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = reps,
            onValueChange = { reps = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(Res.string.session_reps)) },
            singleLine = true,
            isError = repsMau,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (podeGravar) {
                        teclado?.hide()
                        onLog(w!!, r!!, warmup)
                    }
                },
            ),
            modifier = Modifier.weight(1f).focusRequester(repsFocus),
        )
    }

    if (comBarra) LinhaDosDiscos(pesoNoEcra = noDisplay, unidades = unidades)

    // Um botão com o nome da acção e a linha toda, e não um ✓ de 40 dp ao lado dos campos: é
    // o alvo mais tocado do ecrã e era o mais pequeno, e a palavra diz o que acontece a quem
    // nunca cá esteve.
    PrimaryButton(
        text = stringResource(Res.string.session_save_set),
        enabled = podeGravar,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            teclado?.hide()
            onLog(w!!, r!!, warmup)
        },
    )

    ErroDaSerie(pesoMau = pesoMau, repsMau = repsMau, rpeMau = false, unidades = unidades)
}

/**
 * Que discos pôr na barra, por baixo do campo do peso.
 *
 * **Só em exercícios de barra.** Um press de halteres a 30 kg não tem barra nenhuma, e dizer-lhe
 * «5 por lado · barra de 20 kg» era inventar material que ali não existe. O material vem do
 * catálogo de exercícios, que sempre o soube.
 *
 * **Só em exercícios de barra.** Um press de halteres a 30 kg não tem barra nenhuma, e dizer-lhe
 * «5 por lado · barra de 20 kg» era inventar material que ali não existe. O material vem do
 * catálogo de exercícios, que sempre o soube.
 *
 * Só aparece quando há um peso escrito: sem número não há conta nenhuma para mostrar, e uma
 * linha permanente a dizer «só a barra» ocupava espaço para não dizer nada. Quando o peso não
 * se consegue montar com os discos do conjunto, **diz quanto falta** em vez de arredondar em
 * silêncio — é o mesmo princípio da validação, que já diz porquê em vez de ficar cinzenta.
 */
@Composable
private fun LinhaDosDiscos(pesoNoEcra: Double?, unidades: UnitSystem) {
    val peso = pesoNoEcra ?: return
    if (peso <= 0.0) return
    val virgula = virgulaDecimal()
    val unidade = stringResource(weightUnitLabel(unidades))
    val carga = PlateMath.paraOPeso(peso, unidades)

    val texto = when {
        carga == null -> stringResource(Res.string.session_plates_too_light)
        carga.porLado.isEmpty() -> stringResource(Res.string.session_plates_bar_only)
        carga.sobra > 0.0 -> stringResource(
            Res.string.session_plates_approx,
            carga.porLado.joinToString(" + ") { trimmedDecimal(it, places = 2, comma = virgula) },
            "${trimmedDecimal(carga.barra, comma = virgula)} $unidade",
            "${trimmedDecimal(carga.sobra * 2, places = 2, comma = virgula)} $unidade",
        )
        else -> stringResource(
            Res.string.session_plates,
            carga.porLado.joinToString(" + ") { trimmedDecimal(it, places = 2, comma = virgula) },
            "${trimmedDecimal(carga.barra, comma = virgula)} $unidade",
        )
    }

    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A linha de registo de um exercício de **peso do corpo**.
 *
 * Cento e onze exercícios do catálogo não se conseguiam registar: a validação exige um peso
 * maior do que zero, e uma flexão não tem peso para escrever. Aqui o campo do peso passa a
 * ser a **carga extra** — o que está no cinto, ou nada — e o corpo entra por baixo.
 *
 * Sem peso registado no perfil não se inventa nenhum: diz-se que falta, e o botão não grava.
 * Um `70` de recurso seria um número que a pessoa nunca escreveu a entrar no volume, no 1RM e
 * nos recordes dela.
 */
@Composable
private fun LinhaDoPesoDoCorpo(
    ex: SessionExerciseUi,
    pesoDoCorpoKg: Double?,
    unidades: UnitSystem,
    onLog: (Double, Int, Boolean, Double?) -> Unit,
    onPercentagem: (Int) -> Unit,
) {
    val virgula = virgulaDecimal()
    val paraKg = { valor: Double ->
        if (unidades == UnitSystem.IMPERIAL) UnitConversions.lbToKg(valor) else valor
    }

    var extra by remember(ex.exerciseId) { mutableStateOf("") }
    var reps by remember(ex.exerciseId) { mutableStateOf("") }
    var aEscolherPercentagem by remember(ex.exerciseId) { mutableStateOf(false) }

    val extraKg = if (extra.isBlank()) 0.0 else extra.replace(',', '.').toDoubleOrNull()?.let(paraKg)
    val r = reps.toIntOrNull()
    val carga = CargaDoCorpo.calcular(pesoDoCorpoKg, ex.percentagemDoCorpo, extraKg ?: 0.0)

    val extraMau = extra.isNotBlank() && (extraKg == null || !SetLimits.isWeightValid(extraKg))
    val repsMau = reps.isNotBlank() && (r == null || !SetLimits.isRepsValid(r))
    val podeGravar = carga != null && extraKg != null && !extraMau &&
        SetLimits.isSetValid(carga.totalKg, r, null)

    val teclado = LocalSoftwareKeyboardController.current
    val repsFocus = remember { FocusRequester() }

    ContaDoCorpo(
        carga = carga,
        percentagem = ex.percentagemDoCorpo,
        unidades = unidades,
        onEditar = { aEscolherPercentagem = true },
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = extra,
            onValueChange = { extra = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
            label = { Text(stringResource(Res.string.session_added_load)) },
            singleLine = true,
            isError = extraMau,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { repsFocus.requestFocus() }),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = reps,
            onValueChange = { reps = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(Res.string.session_reps)) },
            singleLine = true,
            isError = repsMau,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (podeGravar) {
                        teclado?.hide()
                        onLog(carga!!.totalKg, r!!, false, carga.doCorpoKg)
                        extra = ""
                    }
                },
            ),
            modifier = Modifier.weight(1f).focusRequester(repsFocus),
        )
    }

    TotalDoCorpo(carga = carga, extraKg = extraKg ?: 0.0, unidades = unidades)

    PrimaryButton(
        text = stringResource(Res.string.session_save_set),
        enabled = podeGravar,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            teclado?.hide()
            onLog(carga!!.totalKg, r!!, false, carga.doCorpoKg)
            extra = ""
        },
    )

    ErroDaSerie(pesoMau = extraMau, repsMau = repsMau, rpeMau = false, unidades = unidades)

    if (aEscolherPercentagem) {
        PercentagemDialog(
            atual = ex.percentagemDoCorpo,
            onDismiss = { aEscolherPercentagem = false },
            onConfirm = {
                onPercentagem(it)
                aEscolherPercentagem = false
            },
        )
    }
}

/**
 * A conta à vista: o peso, a percentagem quando não é 100 %, e o resultado.
 *
 * É o mesmo princípio da meta do dia — a app mostra a soma em vez de a apresentar feita. Sem
 * esta linha, a série de uma flexão aparecia com um número que ninguém escreveu.
 */
@Composable
private fun ContaDoCorpo(
    carga: CargaDoCorpo.Carga?,
    percentagem: Int,
    unidades: UnitSystem,
    onEditar: () -> Unit,
) {
    if (carga == null) {
        Text(
            stringResource(Res.string.session_bodyweight_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onEditar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (percentagem == CargaDoCorpo.PERCENTAGEM_POR_OMISSAO) {
                stringResource(Res.string.session_bodyweight_line, loadWithUnit(carga.doCorpoKg, unidades))
            } else {
                stringResource(
                    Res.string.session_bodyweight_pct,
                    percentagem,
                    loadWithUnit(carga.doCorpoKg, unidades),
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Default.Tune,
            // Decorativo: a linha inteira é o alvo e o texto ao lado diz o que ela abre.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** O total só se diz quando há alguma coisa a somar: sem carga extra, já foi dito acima. */
@Composable
private fun TotalDoCorpo(carga: CargaDoCorpo.Carga?, extraKg: Double, unidades: UnitSystem) {
    if (carga == null || extraKg <= 0.0) return
    Text(
        stringResource(Res.string.session_bodyweight_total, loadWithUnit(carga.totalKg, unidades)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Quanto do peso do corpo conta neste exercício.

 *
 * **A app não propõe um valor.** O `motor/05` fala de «cerca de 65 % numa flexão», e é uma
 * aproximação sem fonte no repositório — escrevê-la aqui era dá-la por medida. O que fica é
 * a explicação de para que serve o número, e quem o escreve é quem faz o movimento.
 */
@Composable
private fun PercentagemDialog(atual: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var texto by remember { mutableStateOf(atual.toString()) }
    val valor = texto.toIntOrNull()
    val mau = valor == null || valor !in 1..CargaDoCorpo.PERCENTAGEM_MAXIMA

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.session_bodyweight_percent)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    stringResource(Res.string.session_bodyweight_percent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.filter(Char::isDigit).take(3) },
                    label = { Text("%") },
                    singleLine = true,
                    isError = mau,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                enabled = !mau,
                onClick = { onConfirm(valor!!) },
            )
        },
        dismissButton = { SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss) },
    )
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
