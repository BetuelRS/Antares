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
            slot = slot,
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

@Composable
private fun MealHeader(
    slot: MealSlot,
    totalKcal: Int,
    hasLogs: Boolean,

    onOpenDetail: (() -> Unit)?,
    onAdd: () -> Unit,
    onSaveAsTemplate: () -> Unit,
    onCopyFromDay: () -> Unit,
    onMoveMeal: (MealSlot) -> Unit,
    onClearMeal: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var moveOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(slotLabel(slot), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (totalKcal > 0) {

                val abrirLabel = stringResource(Res.string.meal_detail_open)
                Text(
                    "$totalKcal ${stringResource(Res.string.common_kcal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (onOpenDetail != null) {
                        Modifier
                            .clickable(role = Role.Button, onClick = onOpenDetail)
                            .semantics { contentDescription = abrirLabel }
                    } else {
                        Modifier
                    },
                )
                Spacer(Modifier.width(Spacing.sm))
            }

            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.diary_add_food))
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.common_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.diary_copy_from_day)) },
                        onClick = { menuOpen = false; onCopyFromDay() },
                    )

                    if (hasLogs) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.templates_save_meal)) },
                            onClick = { menuOpen = false; onSaveAsTemplate() },
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.diary_move_meal)) },
                            onClick = { menuOpen = false; moveOpen = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.diary_clear_meal)) },
                            onClick = { menuOpen = false; onClearMeal() },
                        )
                    }
                }
                DropdownMenu(expanded = moveOpen, onDismissRequest = { moveOpen = false }) {
                    MealSlot.entries.filter { it != slot }.forEach { destino ->
                        DropdownMenuItem(
                            text = { Text(slotLabel(destino)) },
                            onClick = { moveOpen = false; onMoveMeal(destino) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepeatMealCard(meal: RepeatableMeal, onClick: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.diary_again, dayShort(meal.fromEpochDay)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    meal.names.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Text(
                    "${meal.kcal} ${stringResource(Res.string.common_kcal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun QuickAddDialog(
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
private fun CopyFromDayDialog(
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
private fun SaveTemplateDialog(
    slot: MealSlot,
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

@Composable
private fun LogRow(
    log: FoodLogEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onMove: (MealSlot) -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var moveOpen by remember { mutableStateOf(false) }
    val openLabel = stringResource(Res.string.log_detail_open)

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
                    .semantics { contentDescription = openLabel },
            ) {
                Text(log.nameSnapshot, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                // A hora só aparece quando existe: um registo de um dia passado não a tem,
                // e escrever «sem hora» em cada linha do histórico seria ruído.
                val hora = log.eatenAtMin?.let { " · ${formatMinuteOfDay(it)}" }.orEmpty()
                Text(
                    "${log.quantityGrams.toInt()} ${stringResource(if (log.isLiquid) Res.string.common_ml else Res.string.common_grams_short)}" +
                        " · ${log.kcalSnapshot} ${stringResource(Res.string.common_kcal)}$hora",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.cd_more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.common_edit)) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.diary_duplicate)) },
                        onClick = { menuOpen = false; onDuplicate() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.diary_move)) },
                        onClick = { menuOpen = false; moveOpen = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.common_delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
                DropdownMenu(expanded = moveOpen, onDismissRequest = { moveOpen = false }) {
                    MealSlot.entries.filter { it != log.mealSlot }.forEach { slot ->
                        DropdownMenuItem(
                            text = { Text(slotLabel(slot)) },
                            onClick = { moveOpen = false; onMove(slot) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(entry: ExerciseLogEntity, onDelete: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {

                val label = entry.label.ifBlank {
                    if (entry.origin == ExerciseOrigin.WORKOUT) stringResource(Res.string.exercise_workout_label) else ""
                }
                Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                Text(
                    "${entry.durationMin} ${stringResource(Res.string.exercise_min)} · ${entry.kcal} ${stringResource(Res.string.common_kcal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.common_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLogDialog(
    log: FoodLogEntity,
    onSave: (Double, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(log.quantityGrams.toInt().toString()) }
    val parsed = text.replace(',', '.').toDoubleOrNull()?.takeIf { it in 1.0..5000.0 }

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
                        Text(stringResource(if (log.isLiquid) Res.string.food_quantity_ml else Res.string.food_quantity_g))
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

@Composable
internal fun slotLabel(slot: MealSlot): String = mealSlotLabel(slot)

/**
 * A janela alimentar do dia, com o jejum que sobra dela.
 *
 * É o número que o botão do jejum não sabe dar: mede o que foi registado ter-se comido, e
 * não o que alguém se lembrou de declarar. Quando faltam horas a registos do dia, isso vem
 * dito ao lado — uma janela feita sobre metade do dia não é a janela do dia.
 */
@Composable
private fun JanelaAlimentarLinha(janela: Janela) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.diary_window),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(
                Res.string.diary_window_value,
                formatMinuteOfDay(janela.primeiraMin),
                formatMinuteOfDay(janela.ultimaMin),
                formatDurationMin(janela.duracaoMin),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(Res.string.diary_window_fasting, formatDurationMin(janela.jejumMin)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (janela.semHora > 0) {
            Text(
                stringResource(Res.string.diary_window_partial, janela.semHora),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Diz que se comeu com o jejum a correr.
 *
 * Não julga e não propõe nada: o contador do jejum continua a subir, e a app limita-se a
 * pôr as duas coisas na mesma frase para a pessoa decidir. Quem quiser terminar o jejum
 * tem o botão no ecrã dele — repeti-lo aqui era transformar um facto numa ordem.
 */
@Composable
private fun QuebraDoJejumCartao(quebra: QuebraDoJejum) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.diary_fast_clash_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(
                Res.string.diary_fast_clash_body,
                formatMinuteOfDay(quebra.inicioMin),
                quebra.registos,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// O registo rápido à espera de saber a que refeição pertence. Guarda o pedido inteiro para
// a resposta não obrigar a pessoa a escrever tudo outra vez.
private data class QuickLogPendente(
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
private fun EscolherRefeicaoDialog(onEscolha: (MealSlot) -> Unit, onDismiss: () -> Unit) {
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
