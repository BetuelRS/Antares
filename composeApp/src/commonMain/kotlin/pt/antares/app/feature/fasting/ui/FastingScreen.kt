package pt.antares.app.feature.fasting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.ConfirmDialog
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.util.formatMinuteOfDay
import pt.antares.app.core.util.minuteOfDayAt
import pt.antares.app.feature.fasting.data.toSnapshot
import pt.antares.app.feature.fasting.domain.FastingMachine
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_cancel
import pt.antares.app.generated.resources.fasting_adjust_minus_15m
import pt.antares.app.generated.resources.fasting_adjust_minus_1h
import pt.antares.app.generated.resources.fasting_adjust_plus_15m
import pt.antares.app.generated.resources.fasting_adjust_plus_1h
import pt.antares.app.generated.resources.fasting_adjust_start
import pt.antares.app.generated.resources.fasting_break
import pt.antares.app.generated.resources.fasting_break_confirm_message
import pt.antares.app.generated.resources.fasting_break_confirm_title
import pt.antares.app.generated.resources.fasting_choose_protocol
import pt.antares.app.generated.resources.fasting_disclaimer
import pt.antares.app.generated.resources.fasting_elapsed
import pt.antares.app.generated.resources.fasting_finish
import pt.antares.app.generated.resources.fasting_first_meal
import pt.antares.app.generated.resources.fasting_goal_at
import pt.antares.app.generated.resources.fasting_goal_reached
import pt.antares.app.generated.resources.fasting_history_title
import pt.antares.app.generated.resources.fasting_over_goal
import pt.antares.app.generated.resources.fasting_remaining
import pt.antares.app.generated.resources.fasting_start
import pt.antares.app.generated.resources.fasting_started_at
import pt.antares.app.generated.resources.fasting_title

private const val MIN_MS = 60_000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FastingScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDiary: () -> Unit,
    viewModel: FastingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val justEnded by viewModel.justEnded.collectAsState()

    var now by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(1_000)
        }
    }

    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(Res.string.fasting_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = stringResource(Res.string.fasting_history_title))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            val active = state.active
            if (active != null) {
                ActiveFasting(
                    snapshot = active.toSnapshot(),
                    totalHours = ((active.targetEndAt - active.startedAt) / 3_600_000L).toInt(),
                    now = now,
                    onFinish = viewModel::finish,
                    onBreak = viewModel::breakFast,
                    onShiftStart = viewModel::shiftStart,
                )
            } else {

                if (justEnded) {
                    AntaresCard(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.fasting_goal_reached), style = MaterialTheme.typography.titleMedium)
                        PrimaryButton(
                            stringResource(Res.string.fasting_first_meal),
                            onClick = { viewModel.dismissJustEnded(); onOpenDiary() },
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                        )
                    }
                }
                IdleFasting(
                    state = state,
                    onSelect = viewModel::selectProtocol,
                    onStart = { viewModel.selectedIdOrDefault()?.let(viewModel::start) },
                )
            }

            Text(
                stringResource(Res.string.fasting_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActiveFasting(
    snapshot: pt.antares.app.feature.fasting.domain.FastingSnapshot,
    totalHours: Int,
    now: Long,
    onFinish: () -> Unit,
    onBreak: () -> Unit,
    onShiftStart: (Long) -> Unit,
) {
    val progress = FastingMachine.progress(snapshot, now)
    // Passada a meta, o tempo que falta fica negativo de propósito, e é isso que aqui se
    // mostra: dizer só «objetivo atingido» deitava fora quanto tempo já ia para além dele.
    val subtitle = if (progress.reachedGoal) {
        stringResource(Res.string.fasting_over_goal, FastingFormat.hm(progress.remainingMs))
    } else {
        "${stringResource(Res.string.fasting_remaining)} ${FastingFormat.hm(progress.remainingMs)}"
    }
    val elapsedLabel = FastingFormat.hm(progress.elapsedMs)

    FastingTimerRing(
        fraction = progress.fraction,
        totalHours = totalHours,
        centerValue = elapsedLabel,
        centerSubtitle = subtitle,
        reachedGoal = progress.reachedGoal,
    )
    Text(
        stringResource(Res.string.fasting_elapsed),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    HorasDoJejum(snapshot = snapshot, onShiftStart = onShiftStart, now = now)

    // Um botão só, e o que ele diz sai do relógio. Havia dois — «terminar» e «terminar
    // cedo» — lado a lado desde o primeiro minuto, os dois disponíveis e nada a dizer qual
    // era o certo. Antes da meta só há uma coisa a fazer, e ela é interromper.
    var confirmBreak by remember { mutableStateOf(false) }
    if (progress.reachedGoal) {
        PrimaryButton(stringResource(Res.string.fasting_finish), onFinish, Modifier.fillMaxWidth())
    } else {
        PrimaryButton(stringResource(Res.string.fasting_break), { confirmBreak = true }, Modifier.fillMaxWidth())
    }
    if (confirmBreak) {
        ConfirmDialog(
            title = stringResource(Res.string.fasting_break_confirm_title),
            message = stringResource(Res.string.fasting_break_confirm_message),
            confirmLabel = stringResource(Res.string.fasting_break),
            dismissLabel = stringResource(Res.string.common_cancel),
            onConfirm = { confirmBreak = false; onBreak() },
            onDismiss = { confirmBreak = false },
        )
    }
}

/**
 * As horas do jejum e o acerto do início. O ecrã dizia há quanto tempo se estava em jejum e
 * mais nada — a que horas começou e a que horas acaba estavam só dentro do anel, em fração.
 *
 * O acerto era assimétrico: dava para recuar uma hora e não para avançar. Quem carregava a
 * mais tinha de sair e voltar a começar. Agora anda para os dois lados, e os botões que
 * poriam o início no futuro ficam apagados em vez de não fazer nada.
 */
@Composable
private fun HorasDoJejum(
    snapshot: pt.antares.app.feature.fasting.domain.FastingSnapshot,
    onShiftStart: (Long) -> Unit,
    now: Long,
) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HoraMarcada(Res.string.fasting_started_at, snapshot.startedAt)
            HoraMarcada(Res.string.fasting_goal_at, snapshot.targetEndAt)
        }

        Text(
            stringResource(Res.string.fasting_adjust_start),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = Spacing.md),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AJUSTES.forEach { (rotulo, minutos) ->
                val delta = minutos * MIN_MS
                SecondaryButton(
                    text = stringResource(rotulo),
                    onClick = { onShiftStart(delta) },
                    modifier = Modifier.weight(1f),
                    enabled = snapshot.startedAt + delta <= now,
                )
            }
        }
    }
}

@Composable
private fun HoraMarcada(rotulo: StringResource, instante: Long) {
    Column {
        Text(
            stringResource(rotulo),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(formatMinuteOfDay(minuteOfDayAt(instante)), style = MaterialTheme.typography.titleMedium)
    }
}

private val AJUSTES = listOf(
    Res.string.fasting_adjust_minus_1h to -60L,
    Res.string.fasting_adjust_minus_15m to -15L,
    Res.string.fasting_adjust_plus_15m to 15L,
    Res.string.fasting_adjust_plus_1h to 60L,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleFasting(
    state: FastingUiState,
    onSelect: (String) -> Unit,
    onStart: () -> Unit,
) {
    Text(
        stringResource(Res.string.fasting_choose_protocol),
        style = MaterialTheme.typography.titleMedium,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        val selected = state.selectedProtocol
        state.protocols.forEach { p ->
            FilterChip(
                selected = p.id == selected?.id,
                onClick = { onSelect(p.id) },
                label = { Text(p.name) },
            )
        }
    }
    PrimaryButton(
        stringResource(Res.string.fasting_start),
        onStart,
        Modifier.fillMaxWidth().padding(top = Spacing.md),
        enabled = state.protocols.isNotEmpty(),
    )
}
