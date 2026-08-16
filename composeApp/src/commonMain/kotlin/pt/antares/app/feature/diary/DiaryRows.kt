package pt.antares.app.feature.diary

import androidx.compose.foundation.layout.height
import pt.antares.app.core.calc.DailyBudgetCalc
import pt.antares.app.core.designsystem.AntaresColors
import pt.antares.app.core.designsystem.components.MacroBar
import pt.antares.app.core.designsystem.components.StatRing
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.database.entities.ExerciseLogEntity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.porcaoComUnidade
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresGhostCard
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.mealSlotLabel
import pt.antares.app.core.util.dayShort
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import pt.antares.app.core.util.formatMinuteOfDay

/**
 * As linhas do diário: o cabeçalho de cada refeição, um registo, um exercício, e a
 * sugestão de repetir.
 *
 * A sugestão é a única que não tem forma de cartão sólido, e é de propósito — ver o
 * `RepeatMealCard`.
 */
@Composable
internal fun MealHeader(
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

/**
 * Uma refeição que se pode repetir. **Não é um registo**, e por isso não tem a forma de um.
 *
 * Antes tinha: cartão sólido igual aos dos registos, com «Outra vez, seg» primeiro e a cor de
 * destaque, e o nome do alimento a cinzento por baixo. Um dia sem nada registado parecia
 * cheio, e o anel por cima dizia `0 / 1832` — as duas coisas ao mesmo tempo, na mesma vista.
 *
 * Agora: contorno tracejado, sem fundo, mais baixo, o alimento primeiro e o «outra vez» como
 * etiqueta pequena. O que se comeu é o que interessa; de onde a sugestão veio é a nota.
 */
@Composable
internal fun RepeatMealCard(meal: RepeatableMeal, onClick: () -> Unit) {
    AntaresGhostCard(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    meal.names.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
                Text(
                    stringResource(Res.string.diary_again, dayShort(meal.fromEpochDay)) +
                        " · ${meal.kcal} ${stringResource(Res.string.common_kcal)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun LogRow(
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
                    "${porcaoComUnidade(log.quantityGrams, log.isLiquid)}" +
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
internal fun ExerciseRow(entry: ExerciseLogEntity, onDelete: () -> Unit) {
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

@Composable
internal fun slotLabel(slot: MealSlot): String = mealSlotLabel(slot)

/**
 * A janela alimentar do dia, com o jejum que sobra dela.
 *
 * É o número que o botão do jejum não sabe dar: mede o que foi registado ter-se comido, e
 * não o que alguém se lembrou de declarar. Quando faltam horas a registos do dia, isso vem
 * dito ao lado — uma janela feita sobre metade do dia não é a janela do dia.
 */

/**
 * Uma refeição inteira na lista do dia: o cabeçalho, a sugestão de repetir, e os registos.
 *
 * É uma extensão do `LazyListScope` e não um `@Composable` porque os registos têm de ser
 * itens da lista para se poderem reciclar. Enfiá-los num `Column` dentro de um item fazia o
 * dia inteiro compor-se de uma vez.
 */
internal fun LazyListScope.mealSection(
    slot: MealSlot,
    logs: List<FoodLogEntity>,
    sugestao: RepeatableMeal?,
    viewModel: DiaryViewModel,
    // As folhas em vez de seis funções: todas faziam a mesma coisa — pôr um campo a este
    // valor — e passá-las uma a uma era escrever o mesmo seis vezes.
    folhas: DiarySheets,
) {
    item(key = "header-$slot") {
        MealHeader(
            slot = slot,
            totalKcal = logs.sumOf { it.kcalSnapshot },
            hasLogs = logs.isNotEmpty(),
            onOpenDetail = if (logs.isNotEmpty()) ({ folhas.detailMeal = slot }) else null,
            onAdd = { folhas.addSheetSlot = slot },
            onSaveAsTemplate = { folhas.saveTemplateSlot = slot },
            onCopyFromDay = { folhas.copyIntoSlot = slot; viewModel.loadCopyCandidates(slot) },
            onMoveMeal = { destino -> viewModel.moveMeal(slot, destino) },
            onClearMeal = { folhas.clearMealSlot = slot },
        )
    }

    sugestao?.let {
        item(key = "again-$slot") {
            RepeatMealCard(meal = it, onClick = { viewModel.repeatMeal(slot) })
        }
    }

    items(logs, key = { it.id }) { log ->
        LogRow(
            log = log,
            onOpen = { folhas.detailLog = log },
            onEdit = { folhas.editLog = log },
            onDuplicate = { viewModel.duplicateLog(log.id) },
            onMove = { newSlot -> viewModel.moveLog(log.id, newSlot) },
            onDelete = { viewModel.deleteLog(log.id) },
        )
    }
}

/**
 * A barra do dia: setas para trás e para a frente, e o dia ao meio.
 *
 * O botão de voltar a hoje só aparece quando não se está em hoje — é o que impede a barra de
 * ter um botão que não faz nada em quatro dias de cada cinco.
 */
@Composable
internal fun DayHeader(
    isToday: Boolean,
    epochDay: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.cd_previous_day),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isToday) stringResource(Res.string.diary_today) else dayShort(epochDay),
                style = MaterialTheme.typography.titleLarge,
            )
            if (!isToday) {
                IconButton(onClick = onToday) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = stringResource(Res.string.diary_today),
                    )
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(Res.string.cd_next_day),
            )
        }
    }
}

/**
 * O resumo do dia: o anel das calorias, as três barras de macros, e a janela alimentar.
 *
 * As barras mantêm a cor do macro em qualquer valor — a cor aqui é categoria e não estado, e
 * o excesso vê-se pela forma. Ver o `MacroBar`.
 */
@Composable
internal fun DaySummaryCard(state: DiaryState) {
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
                color = if (budget.remaining < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
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
