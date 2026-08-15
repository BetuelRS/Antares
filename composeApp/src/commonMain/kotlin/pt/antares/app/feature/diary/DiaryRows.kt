package pt.antares.app.feature.diary

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.designsystem.Spacing
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
