package pt.antares.app.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
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
                .larguraDeLeitura()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                // Em linha corrida e não numa `Row`: quatro períodos não cabem na largura
                // de um telemóvel estreito, e a quarta ficava cortada em vez de descer.
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    StatsPeriod.entries.forEach { periodo ->
                        FilterChip(
                            selected = state.period == periodo,
                            onClick = { viewModel.setPeriod(periodo) },
                            label = { Text(stringResource(periodLabel(periodo))) },
                        )
                    }
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
        // Uma barra curta pode ser carência ou pode ser falta de análise, e a cor não sabia
        // distinguir as duas. Sem cobertura que chegue, a barra fica cinzenta e tracejada:
        // é uma medida em que não se pode confiar, e a forma passa a dizê-lo.
        MicroBar(fraction = fraction, incerta = c.isPartial)

        if (c.isPartial && c.measuredPct < dayMeasuredPct - COBERTURA_NOTAVEL_ABAIXO) {
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

/**
 * A barra de um micronutriente, com a certeza embutida na forma.
 *
 * Cheia e sólida quando a medida é de confiança. Tracejada e cinzenta quando falta análise a
 * demasiada comida do período — aí o comprimento continua a dizer alguma coisa, mas não é uma
 * afirmação sobre o que se comeu.
 *
 * Uma cor por estado voltava a misturar as duas leituras: era assim que uma barra vermelha
 * podia significar carência **ou** falta de dados.
 */
@Composable
private fun MicroBar(fraction: Float, incerta: Boolean) {
    val traco = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MICRO_BAR_HEIGHT_DP.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(MICRO_BAR_RADIUS_DP.dp)),
    ) {
        if (incerta) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .drawBehind {
                        drawLine(
                            color = traco,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = size.height,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(MICRO_DASH_DP.dp.toPx(), MICRO_GAP_DP.dp.toPx()),
                            ),
                        )
                    },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(MICRO_BAR_RADIUS_DP.dp)),
            )
        }
    }
}

private const val MICRO_BAR_HEIGHT_DP = 8
private const val MICRO_BAR_RADIUS_DP = 4
private const val MICRO_DASH_DP = 5
private const val MICRO_GAP_DP = 3

// Só se nota a falta de análise deste nutriente quando ela é bem pior do que a do dia todo;
// caso contrário repetia-se o aviso que já está no topo do ecrã.
private const val COBERTURA_NOTAVEL_ABAIXO = 15

private fun periodLabel(p: StatsPeriod) = when (p) {
    StatsPeriod.DAY -> Res.string.stat_period_day
    StatsPeriod.WEEK -> Res.string.stat_period_week
    StatsPeriod.MONTH -> Res.string.stat_period_month
    StatsPeriod.YEAR -> Res.string.stat_period_year
}
