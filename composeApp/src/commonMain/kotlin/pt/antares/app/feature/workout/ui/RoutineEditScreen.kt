package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.RadioButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.ProximoAlvo
import pt.antares.app.core.calc.SerieDaUltimaVez
import pt.antares.app.core.calc.UltimaVez
import pt.antares.app.core.calc.resumoDaUltimaVez
import pt.antares.app.core.model.RegraDeProgressao
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightUnitLabel
import pt.antares.app.core.designsystem.loadWithUnit
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import kotlin.math.roundToInt
import pt.antares.app.core.designsystem.components.AntaresCard
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import pt.antares.app.core.designsystem.components.CampoComPassos
import pt.antares.app.core.designsystem.components.ListaArrastavel
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.rememberApagarComDesfazer
import pt.antares.app.core.designsystem.components.rememberDesfazer
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
    /** A rotina duplicada abre-se: é nela que se vai mexer, e não na que se copiou. */
    onOpenRoutine: (String) -> Unit,
    viewModel: RoutineEditViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsState()
    val detail = estado?.detalhe
    val deleted by viewModel.deleted.collectAsState()

    LaunchedEffect(routineId) { viewModel.start(routineId) }
    LaunchedEffect(deleted) { if (deleted) onDeleted() }

    var editItem by remember { mutableStateOf<RoutineItemView?>(null) }
    var duplicar by remember { mutableStateOf(false) }
    var renomear by remember { mutableStateOf(false) }
    var progressao by remember { mutableStateOf(false) }
    val apagar = rememberApagarComDesfazer()
    val desfazer = rememberDesfazer()
    val reordenado = stringResource(Res.string.routine_reordered)

    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(Res.string.routine_edit_title),
                onBack = onBack,
                actions = {
                    MenuDaRotina(
                        onDuplicar = { duplicar = true },
                        onRenomear = { renomear = true },
                        onApagar = {
                            apagar(
                                { viewModel.deleteRoutine() },
                                { viewModel.restoreRoutine(routineId) },
                            )
                        },
                    )
                },
            )
        },
    ) { padding ->
        val d = detail
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).larguraDeLeitura().padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // O nome passa a ser um título e não um campo sempre aberto. Gravava-se **a cada
            // tecla** — vinte transações num nome de vinte letras, e era o único campo da app
            // sem espera. Mudar o nome é raro, e agora é uma escrita só, no ⋮.
            item {
                Text(
                    d?.routine?.name.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                )
            }

            // A regra vive por cima da lista, e não no ⋮: é ela que explica os alvos que as
            // linhas de baixo mostram, e uma decisão escondida num menu deixava a pessoa a
            // olhar para um número que subiu sem saber porquê.
            estado?.let { e ->
                item {
                    CartaoDaProgressao(estado = e, aoTocar = { progressao = true })
                }
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
            // Num `item` só, e não num `items`: o arrastar precisa de ver a lista inteira
            // para saber por que vizinho passou, e uma rotina tem meia dúzia de exercícios.
            item {
                ListaArrastavel(
                    itens = itemsList,
                    chave = { it.item.id },
                    espaco = Spacing.sm,
                    aoLargar = { nova ->
                        val antes = viewModel.ordemActual()
                        viewModel.reordenar(nova)
                        // Mover era a única acção do editor sem desfazer — e é mais fácil de
                        // fazer por engano do que apagar. **Não é o `apagar`:** esse traz a
                        // frase «apagado» colada, e diria à pessoa que o exercício se foi.
                        desfazer(reordenado) { viewModel.reordenar(antes) }
                    },
                    // O levantar do cartão enquanto o dedo o leva é do componente, e não do
                    // cartão: quem arrastar outra lista ganha-o de graça.
                ) { row, _ ->
                    RoutineItemCard(
                        row = row,
                        proposta = estado?.propostas?.get(row.item.id),
                        ultima = estado?.ultimas?.get(row.item.exerciseId).orEmpty(),
                        onEdit = { editItem = row },
                        onSuperset = { g -> viewModel.setSuperset(row.item.id, g) },
                        onDelete = {
                            apagar(
                                { viewModel.deleteItem(row.item.id) },
                                { viewModel.restoreItem(row.item.id) },
                            )
                        },
                    )
                }
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
                    PrimaryButton(
                        text = stringResource(Res.string.routine_start),
                        onClick = { onStart(routineId) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
                    )
                }
            }
        }
    }

    DialogosDoEditor(
        estado = estado,
        progressao = progressao,
        editItem = editItem,
        renomear = renomear,
        duplicar = duplicar,
        viewModel = viewModel,
        onFechar = { progressao = false; editItem = null; renomear = false; duplicar = false },
        onOpenRoutine = onOpenRoutine,
    )
}

/**
 * Os cinco diálogos do editor, num sítio só.
 *
 * Estavam soltos no fim do ecrã e faziam-no passar do limite de comprimento — que é o aviso
 * a dizer que já não se lê de um relance. Aqui, o que o ecrã guarda é o que está aberto; o
 * que cada um faz é problema desta função.
 */
@Composable
private fun DialogosDoEditor(
    estado: RotinaNoEditor?,
    progressao: Boolean,
    editItem: RoutineItemView?,
    renomear: Boolean,
    duplicar: Boolean,
    viewModel: RoutineEditViewModel,
    onFechar: () -> Unit,
    onOpenRoutine: (String) -> Unit,
) {
    if (progressao && estado != null) {
        DialogoDeProgressao(
            estado = estado,
            onGravar = { regra, degrau ->
                viewModel.setProgressao(regra, degrau)
                onFechar()
            },
            onDismiss = onFechar,
        )
    }

    editItem?.let { row ->
        TargetsDialog(
            row = row,
            onSave = { sets, min, max, weight, rest ->
                viewModel.updateTargets(row.item.id, sets, min, max, weight, rest)
                onFechar()
            },
            onDismiss = onFechar,
        )
    }

    DialogosDaRotina(
        nome = estado?.detalhe?.routine?.name.orEmpty(),
        renomear = renomear,
        duplicar = duplicar,
        onFechar = onFechar,
        onRenomear = { viewModel.rename(it); onFechar() },
        onDuplicar = { nome ->
            onFechar()
            viewModel.duplicar(nome) { novoId -> onOpenRoutine(novoId) }
        },
    )
}

/** Os dois que perguntam a mesma coisa, fora do ecrã para ele caber num relance. */
@Composable
private fun DialogosDaRotina(
    nome: String,
    renomear: Boolean,
    duplicar: Boolean,
    onFechar: () -> Unit,
    onRenomear: (String) -> Unit,
    onDuplicar: (String) -> Unit,
) {
    if (renomear) {
        NomeDaRotinaDialog(
            atual = nome,
            titulo = Res.string.routine_rename,
            onDismiss = onFechar,
            onConfirm = onRenomear,
        )
    }
    if (duplicar) {
        NomeDaRotinaDialog(
            atual = stringResource(Res.string.routine_duplicate_suffix, nome),
            titulo = Res.string.routine_duplicate,
            onDismiss = onFechar,
            onConfirm = onDuplicar,
        )
    }
}

/** O que se faz à rotina inteira, e não a um exercício dela. */
@Composable
private fun MenuDaRotina(onDuplicar: () -> Unit, onRenomear: () -> Unit, onApagar: () -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { aberto = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.cd_more_options))
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.routine_rename)) },
                onClick = { aberto = false; onRenomear() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.routine_duplicate)) },
                onClick = { aberto = false; onDuplicar() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.routine_delete)) },
                onClick = { aberto = false; onApagar() },
            )
        }
    }
}

/**
 * O nome de uma rotina, escrito de uma vez.
 *
 * Serve o renomear e o duplicar porque a pergunta é a mesma. O duplicar chega com «(cópia)»
 * já escrito: quem quiser outro nome apaga-o, e quem não quiser não escreve nada.
 */
@Composable
private fun NomeDaRotinaDialog(
    atual: String,
    titulo: org.jetbrains.compose.resources.StringResource,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var nome by remember { mutableStateOf(atual) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titulo)) },
        text = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it.take(MAX_NOME) },
                label = { Text(stringResource(Res.string.routine_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                enabled = nome.isNotBlank(),
                onClick = { onConfirm(nome.trim()) },
            )
        },
        dismissButton = { SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss) },
    )
}

private const val MAX_NOME = 60
private const val MAX_SERIES = 20
private const val MAX_REPS_ALVO = 50
private const val PASSO_DESCANSO = 15
private const val MAX_DESCANSO_S = 600

@Composable
private fun RoutineItemCard(
    row: RoutineItemView,
    proposta: ProximoAlvo?,
    ultima: List<SerieDaUltimaVez>,
    onEdit: () -> Unit,
    onSuperset: (Int?) -> Unit,
    onDelete: () -> Unit,
) {
    val it = row.item
    val unidades = rememberUnitSystem()
    var menu by remember { mutableStateOf(false) }
    var ssMenu by remember { mutableStateOf(false) }

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        // A etiqueta da supersérie aparece em **todas** as linhas do grupo, e não só na
        // primeira. Um cabeçalho só no topo diz onde o grupo começa e não diz onde acaba:
        // no aparelho, o segundo exercício do grupo ficava indistinguível de um solto. É a
        // forma que o `estudo/esbocos/07-treino-rotinas.html` desenha, e tem razão.
        //
        // Continua a não ser um chip: era um `AssistChip` tocável que abria o menu, e o menu
        // está no ⋮. Um alvo de toque a duplicar outro é o que a área 03 condena.
        row.item.supersetGroup?.let { g ->
            Text(
                stringResource(Res.string.routine_superset_of, g),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.exerciseName, style = MaterialTheme.typography.bodyLarge, maxLines = 2)

                // Com regra, o peso da linha é o **proposto**, e o escrito à mão fica onde
                // estava — na base e no diálogo dos alvos. Mostrar os dois ao lado um do
                // outro era pôr a pessoa a escolher entre dois números na linha errada.
                val peso = when {
                    proposta != null -> stringResource(
                        Res.string.routine_prog_alvo,
                        loadWithUnit(proposta.pesoKg, unidades),
                    )
                    it.targetWeightKg != null -> loadWithUnit(it.targetWeightKg, unidades)
                    else -> null
                }
                Text(
                    "${it.targetSets}×${it.targetRepsMin}-${it.targetRepsMax} · ${it.restSec}s" +
                        (peso?.let { p -> " · $p" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinhaDaUltimaVez(ultima, proposta, unidades)
            }
            // A pega, e não duas setas: as setas moviam uma posição de cada vez e estavam
            // coladas ao menu. O `contentDescription` diz o gesto, que não se vê.
            Icon(
                Icons.Default.DragHandle,
                contentDescription = stringResource(Res.string.routine_drag_handle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    var sets by remember { mutableStateOf(it.targetSets) }
    var min by remember { mutableStateOf(it.targetRepsMin) }
    var max by remember { mutableStateOf(it.targetRepsMax) }
    var rest by remember { mutableStateOf(it.restSec) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.exerciseName, maxLines = 2) },
        text = {
            // Cinco campos de texto empilhados eram cinco aberturas de teclado para pôr
            // 4×6-8 com 180 s. Os quatro que são contagens acertam-se com `−` e `+`, e os
            // atalhos cobrem os valores que se repetem. O peso continua a escrever-se: não
            // há passo que sirva a quem sobe de 2,5 em 2,5 e a quem sobe de 10 em 10.
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                CampoComPassos(
                    rotulo = stringResource(Res.string.routine_sets),
                    valor = sets,
                    passo = 1,
                    intervalo = 1..MAX_SERIES,
                    onValor = { sets = it },
                    atalhos = listOf(3, 4, 5),
                )
                CampoComPassos(
                    rotulo = stringResource(Res.string.routine_reps_min),
                    valor = min,
                    passo = 1,
                    intervalo = 1..MAX_REPS_ALVO,
                    onValor = { min = it },
                )
                CampoComPassos(
                    rotulo = stringResource(Res.string.routine_reps_max),
                    valor = max,
                    passo = 1,
                    intervalo = 1..MAX_REPS_ALVO,
                    onValor = { max = it },
                )
                CampoComPassos(
                    rotulo = stringResource(Res.string.routine_rest_sec),
                    valor = rest,
                    passo = PASSO_DESCANSO,
                    intervalo = 0..MAX_DESCANSO_S,
                    onValor = { rest = it },
                    atalhos = listOf(60, 90, 120, 180),
                )
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
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = {
                    onSave(
                        sets,
                        min,
                        // O máximo nunca fica abaixo do mínimo: «8-6» não é um intervalo, e
                        // ninguém repara nele até a sessão o mostrar ao contrário.
                        maxOf(min, max),
                        weight.replace(',', '.').toDoubleOrNull()?.let { v ->
                            if (unidades == UnitSystem.IMPERIAL) UnitConversions.lbToKg(v) else v
                        },
                        rest,
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

/**
 * A regra da rotina, em cima e à vista.
 *
 * Diz o nome **e o que ele faz** — «Dupla · sobe as repetições até ao máximo, depois +2,5 kg e
 * volta ao mínimo». Só o nome era um rótulo que obrigava a abrir o diálogo para o perceber, e
 * quem escolheu a regra há três semanas já não se lembra qual delas era.
 */
@Composable
private fun CartaoDaProgressao(estado: RotinaNoEditor, aoTocar: () -> Unit) {
    val regra = estado.detalhe.routine.progressao
    val degrau = loadWithUnit(estado.incrementoKg, estado.unidades)

    AntaresCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = aoTocar)) {
        Text(
            stringResource(Res.string.routine_progressao),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            when (regra) {
                RegraDeProgressao.NENHUMA -> stringResource(Res.string.routine_prog_nenhuma_longa)
                RegraDeProgressao.LINEAR -> stringResource(Res.string.routine_prog_linear_longa, degrau)
                RegraDeProgressao.DUPLA -> stringResource(Res.string.routine_prog_dupla_longa, degrau)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * O que se fez da última vez, por baixo do alvo.
 *
 * **Só aparece com regra.** Sem progressão o alvo é o que a pessoa escreveu, e o histórico do
 * exercício já tem ecrã próprio — a linha aqui seria ruído em todas as rotinas da app para
 * servir as que ainda não escolheram nada.
 */
@Composable
private fun LinhaDaUltimaVez(
    ultima: List<SerieDaUltimaVez>,
    proposta: ProximoAlvo?,
    unidades: UnitSystem,
) {
    if (proposta == null) return
    val resumo = resumoDaUltimaVez(ultima) ?: return

    val texto = when (resumo) {
        is UltimaVez.Uniforme -> stringResource(
            Res.string.routine_prog_ultima_uniforme,
            resumo.series,
            resumo.reps,
            loadWithUnit(resumo.pesoKg, unidades),
        )
        is UltimaVez.MesmoPeso -> stringResource(
            Res.string.routine_prog_ultima_reps,
            resumo.reps.joinToString(", "),
            loadWithUnit(resumo.pesoKg, unidades),
        )
        is UltimaVez.Mista -> {
            // Num ciclo e não num `joinToString`: o `loadWithUnit` é `@Composable`, e chamá-lo
            // de dentro da lambda não compila. É o mesmo padrão do `MuscleRow` da 2.27.0.
            val partes = mutableListOf<String>()
            for (s in resumo.series) partes += "${loadWithUnit(s.weightKg, unidades)}×${s.reps}"
            stringResource(Res.string.routine_prog_ultima_mista, partes.joinToString(" · "))
        }
    }

    Text(
        // A seta é só para quem subiu: pô-la sempre fazia dela decoração, e repetir o peso
        // também é uma resposta da regra — a de que ainda não se completou o intervalo.
        if (proposta.subiu) "↑ $texto" else texto,
        style = MaterialTheme.typography.bodySmall,
        color = if (proposta.subiu) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/**
 * A escolha da regra, e de quanto ela sobe.
 *
 * O campo do degrau abre com o valor **em uso** — o escolhido, ou o da unidade —, e deixá-lo
 * como está grava nulo em vez desse número: gravar 2,5 congelava o degrau métrico na rotina de
 * quem mudasse para libras no dia seguinte. Ver a [RoutineEntity].
 */
@Composable
private fun DialogoDeProgressao(
    estado: RotinaNoEditor,
    onGravar: (RegraDeProgressao, Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var regra by remember { mutableStateOf(estado.detalhe.routine.progressao) }
    val unidades = estado.unidades
    val porOmissao = ((UnitConversions.weightToDisplay(estado.incrementoKg, unidades) * 100).roundToInt() / 100.0)
    var degrau by remember {
        mutableStateOf(
            estado.detalhe.routine.incrementoKg?.let {
                ((UnitConversions.weightToDisplay(it, unidades) * 100).roundToInt() / 100.0).toString()
            } ?: "",
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.routine_progressao)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                RegraDeProgressao.entries.forEach { r ->
                    OpcaoDeRegra(
                        regra = r,
                        escolhida = r == regra,
                        degrau = loadWithUnit(estado.incrementoKg, unidades),
                        onEscolher = { regra = r },
                    )
                }
                if (regra != RegraDeProgressao.NENHUMA) {
                    NumField(
                        value = degrau,
                        onChange = { degrau = it },
                        label = Res.string.routine_prog_degrau,
                        labelArg = stringResource(weightUnitLabel(unidades)),
                        decimal = true,
                    )
                    Text(
                        stringResource(Res.string.routine_prog_degrau_ajuda, porOmissao.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = {
                    val escrito = degrau.replace(',', '.').toDoubleOrNull()
                    onGravar(
                        regra,
                        // Vazio ou zero voltam ao degrau da unidade: um incremento de zero
                        // fazia a regra nunca subir, sem nada no ecrã a dizer porquê.
                        escrito?.takeIf { it > 0.0 }?.let { v ->
                            if (unidades == UnitSystem.IMPERIAL) UnitConversions.lbToKg(v) else v
                        },
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
private fun OpcaoDeRegra(
    regra: RegraDeProgressao,
    escolhida: Boolean,
    degrau: String,
    onEscolher: () -> Unit,
) {
    Row(
        // O papel é o do botão de rádio e não o de um botão qualquer: a linha inteira faz o
        // que o círculo à esquerda faz, e o TalkBack tem de dizer qual está escolhido.
        modifier = Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onEscolher),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = escolhida, onClick = onEscolher)
        Column(Modifier.weight(1f).padding(top = Spacing.sm)) {
            Text(
                when (regra) {
                    RegraDeProgressao.NENHUMA -> stringResource(Res.string.routine_prog_nenhuma)
                    RegraDeProgressao.LINEAR -> stringResource(Res.string.routine_prog_linear)
                    RegraDeProgressao.DUPLA -> stringResource(Res.string.routine_prog_dupla)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                when (regra) {
                    RegraDeProgressao.NENHUMA -> stringResource(Res.string.routine_prog_nenhuma_longa)
                    RegraDeProgressao.LINEAR -> stringResource(Res.string.routine_prog_linear_longa, degrau)
                    RegraDeProgressao.DUPLA -> stringResource(Res.string.routine_prog_dupla_longa, degrau)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
