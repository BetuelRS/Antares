package pt.antares.app.feature.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.util.dayShort
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.adaptive_accept
import pt.antares.app.generated.resources.adaptive_body
import pt.antares.app.generated.resources.adaptive_disclaimer
import pt.antares.app.generated.resources.adaptive_keep
import pt.antares.app.generated.resources.adaptive_title
import pt.antares.app.generated.resources.ai_disclaimer
import pt.antares.app.generated.resources.coach_adjustments
import pt.antares.app.generated.resources.coach_avg_kcal
import pt.antares.app.generated.resources.coach_disclaimer
import pt.antares.app.generated.resources.coach_row_summary
import pt.antares.app.generated.resources.coach_error
import pt.antares.app.generated.resources.coach_focus
import pt.antares.app.generated.resources.coach_generate
import pt.antares.app.generated.resources.coach_generating
import pt.antares.app.generated.resources.coach_history_empty
import pt.antares.app.generated.resources.coach_history_title
import pt.antares.app.generated.resources.coach_logged_days
import pt.antares.app.generated.resources.coach_observations
import pt.antares.app.generated.resources.coach_on_target
import pt.antares.app.generated.resources.coach_report_title
import pt.antares.app.generated.resources.coach_sparse
import pt.antares.app.generated.resources.coach_teaser_cta
import pt.antares.app.generated.resources.coach_teaser_title
import pt.antares.app.generated.resources.coach_week_numbers
import pt.antares.app.generated.resources.coach_weight_trend
import pt.antares.app.generated.resources.coach_wins

@Composable
private fun weekLabel(weekStartEpochDay: Long): String =
    dayShort(weekStartEpochDay)

@Composable
fun CoachReportScreen(
    reportId: String?,
    onBack: () -> Unit,
    viewModel: CoachViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val report = reportId?.let { id -> state.reports.firstOrNull { it.id == id } } ?: state.latest

    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                title = report?.let {
                    stringResource(Res.string.coach_report_title, weekLabel(it.weekStartEpochDay))
                } ?: stringResource(Res.string.coach_history_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        when {
            loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(Res.string.coach_generating),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }

            report == null -> EmptyState(
                title = stringResource(Res.string.coach_history_empty),
                subtitle = error?.let { stringResource(Res.string.coach_error) },
                modifier = Modifier.padding(padding),
                action = {
                    PrimaryButton(
                        text = stringResource(Res.string.coach_generate),
                        onClick = viewModel::generate,
                    )
                },
            )

            else -> {

                val winsTitle = stringResource(Res.string.coach_wins)
                val obsTitle = stringResource(Res.string.coach_observations)
                val adjTitle = stringResource(Res.string.coach_adjustments)

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {

                    if (report.hasOpenProposal) {
                        item {
                            AdaptiveProposalCard(
                                report = report,
                                onAccept = { viewModel.acceptProposal(report) },
                                onKeep = { viewModel.dismissProposal(report) },
                            )
                        }
                    }

                    if (report.focus.isNotBlank()) item { FocusCard(report.focus) }

                    if (report.aggregate?.isSparse == true) {
                        item {
                            Text(
                                text = stringResource(Res.string.coach_sparse),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    bulletSection(winsTitle, report.wins)
                    bulletSection(obsTitle, report.observations)
                    bulletSection(adjTitle, report.adjustments)

                    if (report.aggregate != null) {
                        item { NumbersCard(report) }
                    }

                    item {

                        // Não é `ai_disclaimer`: este relatório é aritmética local, e o
                        // AdaptiveTargetsOfflineTest rebenta se a AI voltar a este caminho.
                        Text(
                            text = stringResource(Res.string.coach_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoachHistoryScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: CoachViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    AntaresScaffold(
        topBar = {
            AntaresTopBar(title = stringResource(Res.string.coach_history_title), onBack = onBack)
        },
    ) { padding ->
        if (loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(Res.string.coach_generating),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
            return@AntaresScaffold
        }
        if (state.reports.isEmpty()) {

            EmptyState(
                title = stringResource(Res.string.coach_history_empty),
                subtitle = error?.let { stringResource(Res.string.coach_error) },
                modifier = Modifier.padding(padding),
                action = {
                    PrimaryButton(
                        text = stringResource(Res.string.coach_generate),
                        onClick = viewModel::generate,
                    )
                },
            )
            return@AntaresScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(state.reports, key = { it.id }) { report ->
                AntaresCard(modifier = Modifier.fillMaxWidth().clickable { onOpen(report.id) }) {
                    Text(
                        text = stringResource(
                            Res.string.coach_report_title,
                            weekLabel(report.weekStartEpochDay),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    // O `focus` é sempre vazio enquanto o relatório for determinístico
                    // (AdaptiveTargetsOfflineTest exige-o), por isso a linha cai nos
                    // números da semana — sem isto o cartão fica só com a data.
                    val resumo = report.focus.takeIf { it.isNotBlank() }
                        ?: report.aggregate?.let { agg ->
                            stringResource(
                                Res.string.coach_row_summary,
                                agg.loggedDays,
                                agg.avgKcal,
                            )
                        }
                    if (resumo != null) {
                        Text(
                            text = resumo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoachTeaserCard(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoachViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val report = state.latest ?: return
    if (!report.isFresh(todayEpochDay())) return

    AntaresCard(modifier = modifier.fillMaxWidth().clickable { onOpen() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.coach_teaser_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = report.focus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            Text(
                text = stringResource(Res.string.coach_teaser_cta),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Spacing.md),
            )
        }
    }
}

@Composable
private fun FocusCard(focus: String) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.coach_focus),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = focus,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bulletSection(
    title: String,
    lines: List<String>,
) {
    if (lines.isEmpty()) return
    item {
        AntaresCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            lines.forEach { line ->
                Row(modifier = Modifier.padding(top = Spacing.sm)) {
                    Text(text = "·", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumbersCard(report: CoachReportUi) {
    val agg = report.aggregate ?: return
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.coach_week_numbers),
            style = MaterialTheme.typography.labelLarge,
        )
        Column(
            modifier = Modifier.padding(top = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.coach_logged_days, agg.loggedDays),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(Res.string.coach_avg_kcal, agg.avgKcal, agg.targetKcal),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(Res.string.coach_on_target, agg.daysOnTarget),
                style = MaterialTheme.typography.bodyMedium,
            )
            agg.weightTrendDeltaKg?.let { delta ->

                val signed = if (delta > 0) "+$delta" else delta.toString()
                Text(
                    text = stringResource(Res.string.coach_weight_trend, signed),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun AdaptiveProposalCard(
    report: CoachReportUi,
    onAccept: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val proposed = report.proposedKcal ?: return
    val previous = report.previousKcal ?: return
    val observed = report.observedTdee ?: return

    AntaresCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.adaptive_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.adaptive_body, observed, previous, proposed),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PrimaryButton(
                text = stringResource(Res.string.adaptive_accept),
                onClick = onAccept,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = stringResource(Res.string.adaptive_keep),
                onClick = onKeep,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(Res.string.adaptive_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}
