package pt.antares.app.feature.diary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import pt.antares.app.core.model.Sex
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.clickable
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.portionUnitLabel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.util.UnitConversions
import pt.antares.app.feature.fooddata.paraCampo
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.mealSlotLabel
import pt.antares.app.core.util.dayShort
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import pt.antares.app.core.util.MINUTES_PER_HOUR
import pt.antares.app.core.util.formatMinuteOfDay

/**
 * Os diálogos do diário. Vivem à parte do ecrã porque nenhum deles precisa de saber que
 * o diário existe: recebem o que mostram e devolvem a resposta.
 */
@Composable
internal fun QuickAddDialog(
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var kcalText by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val defaultName = stringResource(Res.string.quick_add_default_name)
    val kcal = kcalText.toIntOrNull()?.takeIf { it in 1..10_000 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.quick_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = kcalText,
                    onValueChange = { kcalText = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text(stringResource(Res.string.quick_add_kcal)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(stringResource(Res.string.quick_add_name)) },
                    singleLine = true,
                )
                Text(
                    stringResource(Res.string.quick_add_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = { onConfirm(kcal!!, name.trim().ifBlank { defaultName }) },
                enabled = kcal != null,
            )
        },
        dismissButton = {
            SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss)
        },
    )
}

@Composable
internal fun CopyFromDayDialog(
    candidates: List<RepeatableMeal>?,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.diary_copy_from_day)) },
        text = {
            when {

                candidates == null -> Text(stringResource(Res.string.common_loading))
                candidates.isEmpty() -> Text(stringResource(Res.string.diary_copy_none))
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(candidates, key = { it.fromEpochDay }) { refeicao ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(refeicao.fromEpochDay) }
                                .padding(vertical = Spacing.sm),
                        ) {
                            Text(
                                "${dayShort(refeicao.fromEpochDay)} · ${refeicao.kcal} " +
                                    stringResource(Res.string.common_kcal),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                refeicao.names.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}

@Composable
internal fun SaveTemplateDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.templates_save_meal)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text(stringResource(Res.string.templates_name_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(Res.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditLogDialog(
    log: FoodLogEntity,
    onSave: (Double, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    // Escreve-se na unidade escolhida e grava-se sempre em gramas — a mesma volta e meia do
    // ecrã do alimento, e pela mesma razão: a base não muda com uma preferência.
    val unidades = rememberUnitSystem()
    var text by remember(unidades) {
        mutableStateOf(paraCampo(log.quantityGrams, unidades, log.isLiquid, log.densidade))
    }
    val parsed = text.replace(',', '.').toDoubleOrNull()
        ?.let { UnitConversions.portionToStored(it, unidades, log.isLiquid, log.densidade) }
        ?.takeIf { it in 1.0..5000.0 }

    val previewKcal = parsed?.let { (log.kcalSnapshot * it / log.quantityGrams).toInt() }

    var hora by remember { mutableStateOf(log.eatenAtMin) }
    var relogioAberto by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(log.nameSnapshot, maxLines = 2) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6) },
                    label = {
                        Text(
                            stringResource(
                                Res.string.food_quantity,
                                stringResource(portionUnitLabel(unidades, log.isLiquid)),
                            ),
                        )
                    },
                    singleLine = true,
                )
                if (previewKcal != null) {
                    Text(
                        "$previewKcal ${stringResource(Res.string.common_kcal)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        hora?.let {
                            "${stringResource(Res.string.diary_eaten_at)} ${formatMinuteOfDay(it)}"
                        } ?: stringResource(Res.string.diary_eaten_at_unknown),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row {
                        if (hora != null) {
                            TextButton(onClick = { hora = null }) {
                                Text(stringResource(Res.string.diary_eaten_at_clear))
                            }
                        }
                        TextButton(onClick = { relogioAberto = true }) {
                            Text(stringResource(Res.string.diary_eaten_at_set))
                        }
                    }
                }
                Text(
                    stringResource(Res.string.diary_eaten_at_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = { onSave(parsed!!, hora) },
                enabled = parsed != null,
            )
        },
        dismissButton = {
            SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss)
        },
    )

    if (relogioAberto) {
        // Abre na hora que o registo já tem; sem nenhuma, na hora a que a refeição costuma
        // acontecer, que é sempre melhor palpite do que a meia-noite.
        val inicial = hora ?: (log.mealSlot.typicalHours.first * MINUTES_PER_HOUR)
        val estado = rememberTimePickerState(
            initialHour = inicial / MINUTES_PER_HOUR,
            initialMinute = inicial % MINUTES_PER_HOUR,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { relogioAberto = false },
            title = { Text(stringResource(Res.string.diary_eaten_at)) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(Res.string.common_save),
                    onClick = {
                        hora = estado.hour * MINUTES_PER_HOUR + estado.minute
                        relogioAberto = false
                    },
                )
            },
            dismissButton = {
                SecondaryButton(
                    text = stringResource(Res.string.common_cancel),
                    onClick = { relogioAberto = false },
                )
            },
        )
    }
}

internal data class QuickLogPendente(
    val mode: pt.antares.app.feature.fooddata.AddMode,
    val query: String,
)

/**
 * Pergunta a refeição quando a hora do relógio não a pode dizer — num dia que não é hoje.
 *
 * As quatro pela ordem do dia, e nenhuma pré-escolhida: sugerir uma era voltar a assumir,
 * que é o que isto veio corrigir.
 */
@Composable
internal fun EscolherRefeicaoDialog(onEscolha: (MealSlot) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.diary_which_meal_title)) },
        text = { Text(stringResource(Res.string.diary_which_meal_body)) },
        confirmButton = {
            Column {
                MealSlot.entries.forEach { slot ->
                    TextButton(onClick = { onEscolha(slot) }) {
                        Text(pt.antares.app.core.model.mealSlotLabel(slot))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}

/**
 * Os nove diálogos e folhas do diário, e o estado de cada um.
 *
 * Vivem juntos porque partilham a mesma regra: só um está aberto de cada vez, e todos se
 * fecham pondo o campo a nulo. Estavam declarados um a um no corpo do ecrã, e eram cento e
 * poucas linhas antes de a lista do dia começar.
 */
@Stable
internal class DiarySheets {
    var editLog by mutableStateOf<FoodLogEntity?>(null)
    var detailLog by mutableStateOf<FoodLogEntity?>(null)
    var detailMeal by mutableStateOf<MealSlot?>(null)
    var saveTemplateSlot by mutableStateOf<MealSlot?>(null)
    var addSheetSlot by mutableStateOf<MealSlot?>(null)
    var quickLogPendente by mutableStateOf<QuickLogPendente?>(null)
    var quickAddSlot by mutableStateOf<MealSlot?>(null)
    var copyIntoSlot by mutableStateOf<MealSlot?>(null)
    var clearMealSlot by mutableStateOf<MealSlot?>(null)

    // Não abre folha nenhuma: leva à pesquisa, no separador das refeições. Vive aqui na
    // mesma porque é o sítio onde os pedidos de uma secção do diário se juntam.
    var aplicarModeloSlot by mutableStateOf<MealSlot?>(null)
}

@Composable
internal fun DiaryDialogHost(
    folhas: DiarySheets,
    viewModel: DiaryViewModel,
    epochDay: Long,
    logsBySlot: Map<MealSlot, List<FoodLogEntity>>,
    onAddFood: (MealSlot, Long, pt.antares.app.feature.fooddata.AddMode) -> Unit,
    onQuickLog: (MealSlot, Long, pt.antares.app.feature.fooddata.AddMode, String) -> Unit,
    onOpenFood: (String, MealSlot, Long) -> Unit,
) {
    folhas.quickLogPendente?.let { pedido ->
        EscolherRefeicaoDialog(
            onEscolha = { slot ->
                folhas.quickLogPendente = null
                onQuickLog(slot, epochDay, pedido.mode, pedido.query)
            },
            onDismiss = { folhas.quickLogPendente = null },
        )
    }

    folhas.aplicarModeloSlot?.let { slot ->
        LaunchedEffect(slot) {
            folhas.aplicarModeloSlot = null
            onAddFood(slot, epochDay, pt.antares.app.feature.fooddata.AddMode.MEALS)
        }
    }

    folhas.addSheetSlot?.let { slot ->
        pt.antares.app.feature.fooddata.AddEntrySheet(
            onPick = { mode ->
                folhas.addSheetSlot = null
                if (mode == pt.antares.app.feature.fooddata.AddMode.QUICK) {
                    folhas.quickAddSlot = slot
                } else {
                    onAddFood(slot, epochDay, mode)
                }
            },
            onDismiss = { folhas.addSheetSlot = null },
        )
    }

    folhas.quickAddSlot?.let { slot ->
        QuickAddDialog(
            onConfirm = { kcal, nome ->
                viewModel.quickAddCalories(kcal, nome, slot)
                folhas.quickAddSlot = null
            },
            onDismiss = { folhas.quickAddSlot = null },
        )
    }

    folhas.copyIntoSlot?.let { slot ->
        val copyCandidates by viewModel.copyCandidates.collectAsState()
        CopyFromDayDialog(
            candidates = copyCandidates,
            onPick = { dia -> viewModel.copyMealFrom(dia, slot); folhas.copyIntoSlot = null },
            onDismiss = { folhas.copyIntoSlot = null; viewModel.closeCopyCandidates() },
        )
    }

    folhas.clearMealSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { folhas.clearMealSlot = null },
            title = { Text(stringResource(Res.string.diary_clear_meal)) },
            text = { Text(stringResource(Res.string.diary_clear_meal_body, slotLabel(slot))) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMeal(slot); folhas.clearMealSlot = null }) {
                    Text(stringResource(Res.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { folhas.clearMealSlot = null }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
        )
    }

    folhas.saveTemplateSlot?.let { slot ->
        SaveTemplateDialog(
            onConfirm = { name ->
                viewModel.saveMealAsTemplate(name, slot)
                folhas.saveTemplateSlot = null
            },
            onDismiss = { folhas.saveTemplateSlot = null },
        )
    }

    folhas.detailMeal?.let { slot ->
        val ref by viewModel.nutritionRef.collectAsState()
        MealDetailSheet(
            slot = slot,
            slotName = slotLabel(slot),
            logs = logsBySlot[slot].orEmpty(),
            reference = ref?.reference,
            sex = ref?.sex ?: Sex.MALE,
            lifeStage = ref?.lifeStage,
            onDismiss = { folhas.detailMeal = null },
        )
    }

    folhas.detailLog?.let { log ->
        val ref by viewModel.nutritionRef.collectAsState()
        LogDetailSheet(
            log = log,
            reference = ref?.reference,
            sex = ref?.sex ?: Sex.MALE,
            lifeStage = ref?.lifeStage,
            // Nulo quando o alimento já não existe: o registo sobrevive-lhe.
            onOpenFood = log.foodId?.let { id ->
                { onOpenFood(id, log.mealSlot, log.epochDay) }
            },
            onDismiss = { folhas.detailLog = null },
        )
    }

    folhas.editLog?.let { log ->
        EditLogDialog(
            log = log,
            onSave = { grams, hora ->
                viewModel.updateLogQuantity(log.id, grams)
                if (hora != log.eatenAtMin) viewModel.updateLogEatenAt(log.id, hora)
                folhas.editLog = null
            },
            onDismiss = { folhas.editLog = null },
        )
    }
}
