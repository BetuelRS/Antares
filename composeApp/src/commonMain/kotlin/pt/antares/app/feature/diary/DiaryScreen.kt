package pt.antares.app.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.DailyBudgetCalc
import pt.antares.app.core.database.entities.ExerciseLogEntity
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import pt.antares.app.core.model.Sex
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.designsystem.AntaresColors
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.success
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresGhostCard
import pt.antares.app.core.designsystem.components.MacroBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.StatRing
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.mealSlotLabel
import pt.antares.app.core.util.dayShort
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import pt.antares.app.core.util.MINUTES_PER_HOUR
import pt.antares.app.core.util.formatMinuteOfDay
import pt.antares.app.core.calc.Janela
import pt.antares.app.core.util.formatDurationMin

@Composable
fun DiaryScreen(
    onAddFood: (MealSlot, Long, pt.antares.app.feature.fooddata.AddMode) -> Unit,
    onAddExercise: (Long) -> Unit,

    onQuickLog: (MealSlot, Long, pt.antares.app.feature.fooddata.AddMode, String) -> Unit,
    viewModel: DiaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val repeatable by viewModel.repeatable.collectAsState()
    var editLog by remember { mutableStateOf<FoodLogEntity?>(null) }
    var detailLog by remember { mutableStateOf<FoodLogEntity?>(null) }
    var detailMeal by remember { mutableStateOf<MealSlot?>(null) }

    var saveTemplateSlot by remember { mutableStateOf<MealSlot?>(null) }

    var addSheetSlot by remember { mutableStateOf<MealSlot?>(null) }

    var quickLogPendente by remember { mutableStateOf<QuickLogPendente?>(null) }
    quickLogPendente?.let { pedido ->
        EscolherRefeicaoDialog(
            onEscolha = { slot ->
                quickLogPendente = null
                onQuickLog(slot, state.epochDay, pedido.mode, pedido.query)
            },
            onDismiss = { quickLogPendente = null },
        )
    }

    var quickAddSlot by remember { mutableStateOf<MealSlot?>(null) }

    var copyIntoSlot by remember { mutableStateOf<MealSlot?>(null) }

    var clearMealSlot by remember { mutableStateOf<MealSlot?>(null) }
    val copyCandidates by viewModel.copyCandidates.collectAsState()

    addSheetSlot?.let { slot ->
        pt.antares.app.feature.fooddata.AddEntrySheet(
            onPick = { mode ->
                addSheetSlot = null

                if (mode == pt.antares.app.feature.fooddata.AddMode.QUICK) {
                    quickAddSlot = slot
                } else {
                    onAddFood(slot, state.epochDay, mode)
                }
            },
            onDismiss = { addSheetSlot = null },
        )
    }

    quickAddSlot?.let { slot ->
        QuickAddDialog(
            onConfirm = { kcal, nome ->
                viewModel.quickAddCalories(kcal, nome, slot)
                quickAddSlot = null
            },
            onDismiss = { quickAddSlot = null },
        )
    }

    copyIntoSlot?.let { slot ->
        CopyFromDayDialog(
            candidates = copyCandidates,
            onPick = { dia -> viewModel.copyMealFrom(dia, slot); copyIntoSlot = null },
            onDismiss = { copyIntoSlot = null; viewModel.closeCopyCandidates() },
        )
    }

    clearMealSlot?.let { slot ->
        val nome = slotLabel(slot)
        AlertDialog(
            onDismissRequest = { clearMealSlot = null },
            title = { Text(stringResource(Res.string.diary_clear_meal)) },
            text = { Text(stringResource(Res.string.diary_clear_meal_body, nome)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMeal(slot); clearMealSlot = null }) {
                    Text(stringResource(Res.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { clearMealSlot = null }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
        )
    }

    saveTemplateSlot?.let { slot ->
        SaveTemplateDialog(
            onConfirm = { name ->
                viewModel.saveMealAsTemplate(name, slot)
                saveTemplateSlot = null
            },
            onDismiss = { saveTemplateSlot = null },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::previousDay) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_previous_day))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.isToday) {
                            stringResource(Res.string.diary_today)
                        } else {
                            dayShort(state.epochDay)
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (!state.isToday) {
                        IconButton(onClick = viewModel::goToToday) {
                            Icon(Icons.Default.CalendarToday, contentDescription = stringResource(Res.string.diary_today))
                        }
                    }
                }
                IconButton(onClick = viewModel::nextDay) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(Res.string.cd_next_day))
                }
            }
        }

        state.quebraDoJejum?.let { quebra ->
            item(key = "jejum-quebrado") { QuebraDoJejumCartao(quebra) }
        }

        item(key = "quick-log") {
            // Num dia que não é hoje, a hora do relógio não diz nada sobre a refeição: quem
            // regista o jantar de ontem de manhã levava-o para o pequeno-almoço. Aí
            // pergunta-se, e o pedido fica à espera de resposta.
            fun registar(mode: pt.antares.app.feature.fooddata.AddMode, q: String) {
                if (state.isToday) {
                    onQuickLog(
                        MealSlot.atHour(pt.antares.app.core.util.currentHour()),
                        state.epochDay,
                        mode,
                        q,
                    )
                } else {
                    quickLogPendente = QuickLogPendente(mode, q)
                }
            }
            pt.antares.app.feature.fooddata.QuickLogBar(
                onSubmit = { q -> registar(pt.antares.app.feature.fooddata.AddMode.SEARCH, q) },
                onPhoto = { registar(pt.antares.app.feature.fooddata.AddMode.PHOTO, "") },
                onScan = { registar(pt.antares.app.feature.fooddata.AddMode.SCAN, "") },
            )
        }

        item {
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    val budget = DailyBudgetCalc.compute(
                        target = state.targets?.kcal ?: 0,
                        consumed = state.totals.kcal,
                        exercise = state.exerciseKcal,
                    )
                    StatRing(
                        progress = if (budget.budget > 0) budget.consumed.toFloat() / budget.budget else 0f,
                        centerValue = "${budget.remaining}",
                        centerTitle = stringResource(Res.string.diary_remaining_kcal),
                        color = if (budget.remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "${budget.consumed} / ${budget.budget} ${stringResource(Res.string.common_kcal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.exerciseKcal > 0) {
                        Text(
                            "+${state.exerciseKcal} ${stringResource(Res.string.common_kcal)} · " +
                                stringResource(Res.string.exercise_addback_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(Spacing.md))
                    state.targets?.let { t ->
                        MacroBar(
                            label = stringResource(Res.string.onb_plan_protein),
                            grams = state.totals.proteinG,
                            targetGrams = t.proteinG,
                            color = AntaresColors.macroProtein,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        MacroBar(
                            label = stringResource(Res.string.onb_plan_carbs),
                            grams = state.totals.carbsG,
                            targetGrams = t.carbsG,
                            color = AntaresColors.macroCarbs,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        MacroBar(
                            label = stringResource(Res.string.onb_plan_fat),
                            grams = state.totals.fatG,
                            targetGrams = t.fatG,
                            color = AntaresColors.macroFat,
                        )
                    }

                    state.janela?.let { janela ->
                        Spacer(Modifier.height(Spacing.md))
                        JanelaAlimentarLinha(janela)
                    }
                }
            }
        }

        if (state.logsBySlot.isEmpty()) {
            item {
                SecondaryButton(
                    text = stringResource(Res.string.diary_copy_yesterday),
                    onClick = viewModel::copyYesterday,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        MealSlot.entries.forEach { slot ->
            val logs = state.logsBySlot[slot].orEmpty()
            item(key = "header-$slot") {
                MealHeader(
                    slot = slot,
                    totalKcal = logs.sumOf { it.kcalSnapshot },
                    hasLogs = logs.isNotEmpty(),
                    onOpenDetail = if (logs.isNotEmpty()) ({ detailMeal = slot }) else null,
                    onAdd = { addSheetSlot = slot },
                    onSaveAsTemplate = { saveTemplateSlot = slot },
                    onCopyFromDay = {
                        copyIntoSlot = slot
                        viewModel.loadCopyCandidates(slot)
                    },
                    onMoveMeal = { destino -> viewModel.moveMeal(slot, destino) },
                    onClearMeal = { clearMealSlot = slot },
                )
            }

            repeatable[slot]?.let { sugestao ->
                item(key = "again-$slot") {
                    RepeatMealCard(meal = sugestao, onClick = { viewModel.repeatMeal(slot) })
                }
            }
            items(logs, key = { it.id }) { log ->
                LogRow(
                    log = log,
                    onOpen = { detailLog = log },
                    onEdit = { editLog = log },
                    onDuplicate = { viewModel.duplicateLog(log.id) },
                    onMove = { newSlot -> viewModel.moveLog(log.id, newSlot) },
                    onDelete = { viewModel.deleteLog(log.id) },
                )
            }
        }

        item(key = "exercise-header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.exercise_section_title), style = MaterialTheme.typography.titleMedium)
                    if (state.exerciseKcal > 0) {
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            "${state.exerciseKcal} ${stringResource(Res.string.common_kcal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { onAddExercise(state.epochDay) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.exercise_add_cta))
                }
            }
        }
        if (state.exerciseEntries.isEmpty()) {
            item(key = "exercise-empty") {
                Text(
                    stringResource(Res.string.exercise_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.exerciseEntries, key = { "ex-${it.id}" }) { entry ->
            ExerciseRow(entry = entry, onDelete = { viewModel.deleteExercise(entry.id) })
        }

        item {
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(stringResource(Res.string.diary_water), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.waterMl} / ${state.waterGoalMl} ml",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.waterMl >= state.waterGoalMl) {
                                MaterialTheme.success
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Row {
                        TextButton(onClick = { viewModel.addWater(-250) }) { Text("−250") }
                        TextButton(onClick = { viewModel.addWater(+250) }) { Text("+250") }
                        TextButton(onClick = { viewModel.addWater(+500) }) { Text("+500") }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }

    detailMeal?.let { slot ->
        val ref by viewModel.nutritionRef.collectAsState()
        MealDetailSheet(
            slot = slot,
            slotName = slotLabel(slot),
            logs = state.logsBySlot[slot].orEmpty(),
            reference = ref?.reference,
            sex = ref?.sex ?: Sex.MALE,
            lifeStage = ref?.lifeStage,
            onDismiss = { detailMeal = null },
        )
    }

    detailLog?.let { log ->
        val ref by viewModel.nutritionRef.collectAsState()
        LogDetailSheet(
            log = log,
            reference = ref?.reference,
            sex = ref?.sex ?: Sex.MALE,
            lifeStage = ref?.lifeStage,
            onDismiss = { detailLog = null },
        )
    }

    editLog?.let { log ->
        EditLogDialog(
            log = log,
            onSave = { grams, hora ->
                viewModel.updateLogQuantity(log.id, grams)
                if (hora != log.eatenAtMin) viewModel.updateLogEatenAt(log.id, hora)
                editLog = null
            },
            onDismiss = { editLog = null },
        )
    }
}
