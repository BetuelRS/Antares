package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_cancel
import pt.antares.app.generated.resources.schedule_clear
import pt.antares.app.generated.resources.schedule_confirm_cta
import pt.antares.app.generated.resources.schedule_confirm_title
import pt.antares.app.generated.resources.schedule_day_1
import pt.antares.app.generated.resources.schedule_day_2
import pt.antares.app.generated.resources.schedule_day_3
import pt.antares.app.generated.resources.schedule_day_4
import pt.antares.app.generated.resources.schedule_day_5
import pt.antares.app.generated.resources.schedule_day_6
import pt.antares.app.generated.resources.schedule_day_7
import pt.antares.app.generated.resources.schedule_intro
import pt.antares.app.generated.resources.schedule_pick_title
import pt.antares.app.generated.resources.schedule_rest
import pt.antares.app.generated.resources.schedule_title

private val dayNames: List<StringResource> = listOf(
    Res.string.schedule_day_1, Res.string.schedule_day_2, Res.string.schedule_day_3,
    Res.string.schedule_day_4, Res.string.schedule_day_5, Res.string.schedule_day_6,
    Res.string.schedule_day_7,
)

@Composable
fun WeeklyScheduleScreen(
    onBack: () -> Unit,
    viewModel: WeeklyScheduleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val picking by viewModel.picking.collectAsState()
    val confirm by viewModel.confirm.collectAsState()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.schedule_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(Res.string.schedule_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.days.forEach { day ->
                AntaresCard(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.openPicker(day.dayOfWeek) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(dayNames[day.dayOfWeek - 1]), style = MaterialTheme.typography.bodyLarge)

                        Text(
                            modifier = Modifier.weight(1f, fill = false).padding(start = Spacing.md),
                            text = day.routineName ?: stringResource(Res.string.schedule_rest),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (day.routineName != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    val pickingDay = picking
    if (pickingDay != null) {
        AlertDialog(
            onDismissRequest = viewModel::closePicker,
            title = { Text(stringResource(Res.string.schedule_pick_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {

                    Text(
                        stringResource(Res.string.schedule_clear),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { viewModel.clearDay(pickingDay) }
                            .padding(vertical = Spacing.sm),
                    )
                    state.routines.forEach { routine ->
                        Text(
                            routine.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.chooseRoutine(pickingDay, routine.id) }
                                .padding(vertical = Spacing.sm),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::closePicker) { Text(stringResource(Res.string.common_cancel)) }
            },
        )
    }

    val c = confirm
    if (c != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelConfirm,
            title = { Text(stringResource(Res.string.schedule_confirm_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        "${c.detail.routine.name} · ${stringResource(dayNames[c.dayOfWeek - 1])}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    c.detail.items.forEach { item ->
                        Text(
                            "• ${item.exerciseName} — ${item.item.targetSets}×${item.item.targetRepsMin}-${item.item.targetRepsMax}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (c.detail.items.isEmpty()) {
                        Text(
                            stringResource(Res.string.schedule_rest),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                PrimaryButton(text = stringResource(Res.string.schedule_confirm_cta), onClick = viewModel::confirmSchedule)
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelConfirm) { Text(stringResource(Res.string.common_cancel)) }
            },
        )
    }
}
