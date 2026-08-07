package pt.antares.app.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.components.SplitRow
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.nutrition.MicroCoverage
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun NutritionStatsScreen(
    onBack: () -> Unit,
    viewModel: NutritionStatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.stat_title), onBack = onBack) },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    FilterChip(
                        selected = state.period == StatsPeriod.DAY,
                        onClick = { viewModel.setPeriod(StatsPeriod.DAY) },
                        label = { Text(stringResource(Res.string.stat_period_day)) },
                    )
                    FilterChip(
                        selected = state.period == StatsPeriod.WEEK,
                        onClick = { viewModel.setPeriod(StatsPeriod.WEEK) },
                        label = { Text(stringResource(Res.string.stat_period_week)) },
                    )
                }
            }

            if (!state.hasAnyData) {
                item {
                    Text(
                        stringResource(Res.string.stat_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
                    )
                }
            } else {

                if (state.includesOldCatalogue) {
                    item {
                        Text(
                            stringResource(Res.string.stat_old_catalogue),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )
                    }
                }
                if (state.measuredAnyPct < 100) {
                    item {
                        Text(
                            stringResource(Res.string.stat_micro_base, state.measuredAnyPct),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )
                    }
                }

                for ((titleRes, rows) in state.groups) {
                    if (rows.isEmpty()) continue
                    item(key = "h-$titleRes") {
                        Text(
                            stringResource(titleRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = Spacing.sm),
                        )
                    }
                    items(rows, key = { it.key }) {
                        CoverageRow(it, dayMeasuredPct = state.measuredAnyPct)
                    }
                }
            }

            item {
                Text(
                    stringResource(Res.string.stat_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.lg),
                )
            }
        }
    }
}

@Composable
private fun CoverageRow(c: MicroCoverage, dayMeasuredPct: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {

        SplitRow(
            leading = {
                Text(stringResource(microLabel(c.key)), style = MaterialTheme.typography.bodyMedium)
            },
            trailing = {
            if (c.hasData) {
                Text(
                    "${c.coveragePct}%  ·  ${fmt(c.intake)}/${fmt(c.drv)} ${c.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(Res.string.stat_micro_no_data),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            },
        )
        val fraction = if (c.hasData) (c.coveragePct / 100f).coerceIn(0f, 1f) else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(
                        if (c.coveragePct >= 100) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(4.dp),
                    ),
            )
        }

        if (c.isPartial && c.measuredPct < dayMeasuredPct - 15) {
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(Res.string.stat_micro_partial, c.measuredPct),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun fmt(v: Double): String {
    val r = (v * 10).roundToInt() / 10.0
    val i = r.toInt()
    return if (r == i.toDouble()) i.toString() else r.toString()
}

private fun microLabel(key: String): StringResource = pt.antares.app.core.nutrition.microLabelRes(key)
