package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import pt.antares.app.core.calc.ChartScale
import pt.antares.app.core.calc.TimeAxis
import pt.antares.app.core.database.entities.WeightLogEntity
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresChart
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.ConfirmDialog
import pt.antares.app.core.designsystem.components.rememberApagarComDesfazer
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.axisDate
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.core.util.UnitConversions
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_cancel
import pt.antares.app.generated.resources.common_delete
import pt.antares.app.generated.resources.common_kg
import pt.antares.app.generated.resources.common_lb
import pt.antares.app.generated.resources.common_save
import pt.antares.app.generated.resources.goal_change_ack
import pt.antares.app.generated.resources.weight_confirm_body
import pt.antares.app.generated.resources.weight_confirm_save
import pt.antares.app.generated.resources.weight_confirm_title
import pt.antares.app.generated.resources.weight_delete_confirm_msg
import pt.antares.app.generated.resources.weight_add
import pt.antares.app.generated.resources.weight_delta_since
import pt.antares.app.generated.resources.weight_dialog_date
import pt.antares.app.generated.resources.weight_dialog_replaces
import pt.antares.app.generated.resources.weight_how_to_body
import pt.antares.app.generated.resources.weight_how_to_title
import pt.antares.app.generated.resources.weight_next_day
import pt.antares.app.generated.resources.weight_prev_day
import pt.antares.app.generated.resources.weight_second_average
import pt.antares.app.generated.resources.weight_second_body
import pt.antares.app.generated.resources.weight_second_keep
import pt.antares.app.generated.resources.weight_second_replace
import pt.antares.app.generated.resources.weight_second_title
import pt.antares.app.generated.resources.weight_delete_confirm_title
import pt.antares.app.generated.resources.weight_dialog_title
import pt.antares.app.generated.resources.weight_empty_subtitle
import pt.antares.app.generated.resources.weight_empty_title
import pt.antares.app.generated.resources.weight_note_hint
import pt.antares.app.generated.resources.weight_title
import pt.antares.app.generated.resources.weight_trend_label
import pt.antares.app.generated.resources.weight_true_weight
import kotlin.math.abs

@Composable
fun WeightHistoryScreen(
    onBack: () -> Unit,
    viewModel: WeightViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val recalc by viewModel.recalc.collectAsState()
    val pendingTypo by viewModel.pendingTypo.collectAsState()

    pendingTypo?.let { p ->
        ConfirmDialog(
            title = stringResource(Res.string.weight_confirm_title),
            message = stringResource(
                Res.string.weight_confirm_body,
                fmtG(p.weightKg),
                fmtG(p.referenceKg),
            ),
            confirmLabel = stringResource(Res.string.weight_confirm_save),
            dismissLabel = stringResource(Res.string.common_cancel),
            onConfirm = viewModel::confirmPending,
            onDismiss = viewModel::dismissPending,
        )
    }
    val segundaPesagem by viewModel.segundaPesagem.collectAsState()
    segundaPesagem?.let { p ->
        SegundaPesagemDialog(
            anterior = fmtG(p.anteriorKg),
            novo = fmtG(p.novoKg),
            onEscolha = viewModel::resolverSegundaPesagem,
            onDismiss = viewModel::dispensarSegundaPesagem,
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WeightLogEntity?>(null) }
    val apagar = rememberApagarComDesfazer()

    val imperial = state.unitSystem == UnitSystem.IMPERIAL
    val unitLabel = stringResource(if (imperial) Res.string.common_lb else Res.string.common_kg)

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.weight_title), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {

                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.weight_add))
            }
        },
    ) { padding ->
        if (state.entries.isEmpty() && !state.loading) {
            EmptyState(
                title = stringResource(Res.string.weight_empty_title),
                subtitle = stringResource(Res.string.weight_empty_subtitle),
                modifier = Modifier.padding(padding),
            )
        } else {
            ListaAdaptavel(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                espaco = Spacing.sm,
            ) {

                recalc?.let { r ->
                    linhaInteira {
                        Column {
                            WeightRecalcNotice(r.oldKcal, r.newKcal, r.deltaWeightKg)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = viewModel::consumeRecalc) {
                                    Text(stringResource(Res.string.goal_change_ack))
                                }
                            }
                        }
                    }
                }

                linhaInteira {
                    AntaresCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(Res.string.weight_how_to_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(Res.string.weight_how_to_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Text(
                            stringResource(Res.string.weight_true_weight),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.sm),
                        )
                    }
                }
                if (state.series.size >= MIN_POINTS_FOR_CHART) {
                    // O gráfico atravessa a linha toda mesmo em três colunas: espremido a um
                    // terço da largura, os pontos ficam por cima uns dos outros.
                    linhaInteira {
                        AntaresCard(modifier = Modifier.fillMaxWidth()) {
                            AntaresChart(
                                points = state.series.display(imperial),
                                trend = state.trendSeries.display(imperial),
                                targetValue = state.goalWeightKg?.display(imperial),
                                modifier = Modifier.fillMaxWidth(),
                                labels = { escala, eixo -> ChartAxisLabels(escala, eixo) },
                            )
                            state.trend?.let { trend ->
                                Text(
                                    "${stringResource(Res.string.weight_trend_label)}: " +
                                        "${fmtG(trend.display(imperial))} $unitLabel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = Spacing.sm),
                                )
                            }
                        }
                    }
                }

                itemsIndexed(state.entries) { index, entry ->
                    WeightRow(
                        entry = entry,
                        previousKg = state.entries.getOrNull(index + 1)?.weightKg,
                        imperial = imperial,
                        unitLabel = unitLabel,
                        onDelete = { pendingDelete = entry },
                    )
                }
                linhaInteira { Spacer(Modifier.height(FAB_SPACE_DP.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddWeightDialog(
            imperial = imperial,
            unitLabel = unitLabel,
            daysWithEntry = state.daysWithEntry,
            onSave = { epochDay, weightInput, note ->

                val kg = if (imperial) UnitConversions.lbToKg(weightInput) else weightInput
                viewModel.submit(epochDay = epochDay, weightKg = kg, note = note)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = stringResource(Res.string.weight_delete_confirm_title),
            message = stringResource(Res.string.weight_delete_confirm_msg),
            confirmLabel = stringResource(Res.string.common_delete),
            dismissLabel = stringResource(Res.string.common_cancel),
            onConfirm = {
                apagar({ viewModel.delete(entry.id) }, { viewModel.restore(entry.id) })
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun WeightRow(
    entry: WeightLogEntity,
    previousKg: Double?,
    imperial: Boolean,
    unitLabel: String,
    onDelete: () -> Unit,
) {
    val display = entry.weightKg.display(imperial)
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("${fmtG(display)} $unitLabel", style = MaterialTheme.typography.titleLarge)
                Text(

                    dayShortDated(entry.epochDay),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                previousKg?.let { anterior ->
                    val delta = entry.weightKg - anterior
                    if (abs(delta) >= DELTA_EPSILON_KG) {
                        val sinal = if (delta > 0) "+" else "−"
                        Text(
                            stringResource(
                                Res.string.weight_delta_since,
                                "$sinal${fmtG(abs(delta).display(imperial))} $unitLabel",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                entry.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.common_delete))
            }
        }
    }
}

@Composable
private fun ChartAxisLabels(scale: ChartScale, eixo: TimeAxis) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            fmtG(scale.min),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            fmtG(scale.max),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val marcas = eixo.tickDays()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        marcas.forEachIndexed { i, dia ->
            Text(
                axisDate(dia, eixo.spanDays),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = when (i) {
                    0 -> TextAlign.Start
                    marcas.lastIndex -> TextAlign.End
                    else -> TextAlign.Center
                },
            )
        }
    }
}

@Composable
private fun AddWeightDialog(
    imperial: Boolean,
    unitLabel: String,
    daysWithEntry: Set<Long>,
    onSave: (Long, Double, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val hoje = remember { todayEpochDay() }
    var dia by remember { mutableStateOf(hoje) }
    var weightText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    val parsed = weightText.replace(',', '.').toDoubleOrNull()
    val validRange = if (imperial) IMPERIAL_RANGE else METRIC_RANGE
    val valid = parsed != null && parsed in validRange

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.weight_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    stringResource(Res.string.weight_dialog_date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { dia -= 1 }) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = stringResource(Res.string.weight_prev_day),
                        )
                    }
                    Text(dayShortDated(dia, hoje), style = MaterialTheme.typography.bodyLarge)
                    IconButton(
                        onClick = { dia += 1 },

                        enabled = dia < hoje,
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = stringResource(Res.string.weight_next_day),
                        )
                    }
                }
                if (dia in daysWithEntry) {

                    Text(
                        stringResource(Res.string.weight_dialog_replaces),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6) },
                    label = { Text(unitLabel) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it.take(NOTE_MAX_CHARS) },
                    label = { Text(stringResource(Res.string.weight_note_hint)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = { onSave(dia, parsed!!, noteText.ifBlank { null }) },
                enabled = valid,
            )
        },
        dismissButton = {
            SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss)
        },
    )
}

private fun Double.display(imperial: Boolean): Double =
    UnitConversions.weightToDisplay(this, if (imperial) UnitSystem.IMPERIAL else UnitSystem.METRIC)

private fun List<Pair<Long, Double>>.display(imperial: Boolean): List<Pair<Long, Double>> =
    if (imperial) map { it.first to UnitConversions.kgToLb(it.second) } else this

private val METRIC_RANGE = 30.0..300.0
private val IMPERIAL_RANGE = 66.0..660.0
private const val NOTE_MAX_CHARS = 100
private const val FAB_SPACE_DP = 80
private const val MIN_POINTS_FOR_CHART = 2

private const val DELTA_EPSILON_KG = 0.05

/**
 * Pergunta o que fazer com a segunda pesagem do dia, com os dois valores à vista.
 *
 * Três botões e não dois: o `ConfirmDialog` do sistema de desenho tem confirmar e cancelar,
 * e aqui nenhuma das três respostas é um cancelamento — manter a anterior é uma escolha
 * tão deliberada como as outras.
 */
@Composable
private fun SegundaPesagemDialog(
    anterior: String,
    novo: String,
    onEscolha: (EscolhaDaSegundaPesagem) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.weight_second_title)) },
        text = { Text(stringResource(Res.string.weight_second_body, anterior, novo)) },
        confirmButton = {
            Column {
                TextButton(onClick = { onEscolha(EscolhaDaSegundaPesagem.SUBSTITUIR) }) {
                    Text(stringResource(Res.string.weight_second_replace, novo))
                }
                TextButton(onClick = { onEscolha(EscolhaDaSegundaPesagem.MEDIA) }) {
                    Text(stringResource(Res.string.weight_second_average))
                }
                TextButton(onClick = { onEscolha(EscolhaDaSegundaPesagem.MANTER_A_ANTERIOR) }) {
                    Text(stringResource(Res.string.weight_second_keep, anterior))
                }
            }
        },
    )
}
