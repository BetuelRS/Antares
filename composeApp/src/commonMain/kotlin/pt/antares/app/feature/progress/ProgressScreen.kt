package pt.antares.app.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.BeenHereCalc
import pt.antares.app.core.calc.ChartScale
import pt.antares.app.core.calc.EatingPatterns
import pt.antares.app.core.calc.ProgressCalc
import pt.antares.app.core.calc.ProgressRange
import pt.antares.app.core.calc.TimeAxis
import pt.antares.app.core.database.entities.ProgressPhotoEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.success
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresChart
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.designsystem.components.SplitRow
import pt.antares.app.core.model.mealSlotLabel
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import kotlinx.coroutines.launch
import pt.antares.app.core.util.axisDate
import pt.antares.app.core.util.rememberImageSharer
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.abs
import kotlin.math.roundToInt
import pt.antares.app.core.util.formatDurationMin
import pt.antares.app.core.util.formatMinuteOfDay

@Composable
fun ProgressSections(
    onWeightHistory: () -> Unit,
    onPhotos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (state.loading) {
        LoadingState(modifier)
        return
    }
    if (!state.hasAnything) {
        EmptyState(
            title = stringResource(Res.string.progress_empty_title),
            subtitle = stringResource(Res.string.progress_empty_subtitle),
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {

        if (state.hasWeight) HeroWeight(state)
        RangePicker(state.range, viewModel::setRange)

        SectionHeader(title = stringResource(Res.string.progress_q_changing))
        if (state.hasWeight) WeightCard(state, onWeightHistory)
        PhotosCard(state, onPhotos)
        state.lastVisit?.let { BeenHereCard(it, state) }

        SectionHeader(title = stringResource(Res.string.progress_q_consistent))
        ConsistencyCard(state)
        if (state.patterns.isNotEmpty()) PatternsCard(state)

        val temResultado = state.weeklyRateKg != null ||
            state.rangeChangeKg != null ||
            state.kcalComparison != null ||
            state.goals.isNotEmpty() ||
            state.milestones.isNotEmpty()
        if (temResultado) {
            SectionHeader(title = stringResource(Res.string.progress_q_working))
            RateCard(state)
            ShareCard(state)
            state.kcalComparison?.let { MonthComparisonCard(it) }
            if (state.goals.isNotEmpty()) GoalTimelineCard(state)
            if (state.milestones.isNotEmpty()) MilestonesCard(state)
        }
    }
}

@Composable
private fun PhotosCard(state: ProgressState, onPhotos: () -> Unit) {
    val primeira = state.photoFirst
    val ultima = state.photoLast

    AntaresCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onPhotos)) {
        Text(
            stringResource(Res.string.photos_title),
            style = MaterialTheme.typography.titleMedium,
        )
        when {

            primeira != null && ultima != null && primeira.id != ultima.id -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    PhotoThumb(primeira, Modifier.weight(1f))
                    PhotoThumb(ultima, Modifier.weight(1f))
                }
            }

            ultima != null -> {
                Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                    PhotoThumb(ultima, Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
                Text(
                    stringResource(Res.string.photos_need_second),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> Text(
                stringResource(Res.string.photos_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun PhotoThumb(foto: ProgressPhotoEntity, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PhotoImage(foto)
        Text(
            dayShortDated(foto.epochDay),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShareCard(state: ProgressState) {
    val mudanca = state.rangeChangeKg ?: return
    val imperial = state.unitSystem == UnitSystem.IMPERIAL
    val unidade = stringResource(if (imperial) Res.string.common_lb else Res.string.common_kg)
    val camada = rememberGraphicsLayer()
    val partilhar = rememberImageSharer()
    val scope = rememberCoroutineScope()
    val ficheiro = stringResource(Res.string.share_card_filename)

    AntaresCard(
        modifier = Modifier
            .fillMaxWidth()

            .drawWithContent {
                camada.record { this@drawWithContent.drawContent() }
                drawLayer(camada)
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${if (mudanca < 0) "−" else "+"}${fmtG(abs(mudanca).display(imperial))} $unidade",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(rangeLabel(state.range)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.progress_consistency_pct, state.consistencyPct),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.share_card_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { scope.launch { partilhar(ficheiro, camada.toImageBitmap()) } },
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(Res.string.share_card_cta),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun HeroWeight(state: ProgressState) {
    val imperial = state.unitSystem == UnitSystem.IMPERIAL
    val unidade = stringResource(if (imperial) Res.string.common_lb else Res.string.common_kg)
    val tendencia = state.trendNowKg ?: return

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AntaresChart(
                points = state.rangeWeightSeries.display(imperial),
                trend = state.rangeTrendSeries.display(imperial),
                height = HERO_CHART_DP,

                pointColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HERO_CHART_ALPHA),
                trendColor = MaterialTheme.colorScheme.primary.copy(alpha = HERO_CHART_ALPHA),
                gridColor = MaterialTheme.colorScheme.outline.copy(alpha = HERO_CHART_ALPHA),
                modifier = Modifier.fillMaxWidth(),
            )
            // Base por baixo do texto: o gráfico passa-lhe por trás e, mesmo a
            // 0.35 de alfa, as linhas cruzam os dígitos.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(HERO_SCRIM_RADIUS_DP.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = HERO_SCRIM_ALPHA),
                    )
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            ) {
                Text(
                    "${fmtG(tendencia.display(imperial))} $unidade",
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    stringResource(Res.string.today_weight_trend),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RangeChangeLine(state, imperial, unidade)
            }
        }
    }
}

@Composable
private fun RangeChangeLine(state: ProgressState, imperial: Boolean, unidade: String) {
    val mudanca = state.rangeChangeKg
    if (mudanca == null) {

        Text(
            stringResource(Res.string.progress_range_too_short),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val quanto = "${fmtG(abs(mudanca).display(imperial))} $unidade"
    Text(
        when {
            abs(mudanca) < CHANGE_EPSILON_KG -> stringResource(Res.string.progress_weight_same)
            mudanca < 0 -> stringResource(Res.string.progress_weight_lost, quanto)
            else -> stringResource(Res.string.progress_weight_gained, quanto)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RangePicker(atual: ProgressRange, onPick: (ProgressRange) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ProgressRange.entries.forEach { range ->
            FilterChip(
                selected = range == atual,
                onClick = { onPick(range) },
                label = { Text(stringResource(rangeLabel(range)), maxLines = 1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun rangeLabel(range: ProgressRange) = when (range) {
    ProgressRange.DAYS_30 -> Res.string.progress_range_30d
    ProgressRange.MONTHS_3 -> Res.string.progress_range_3m
    ProgressRange.YEAR -> Res.string.progress_range_1y
    ProgressRange.ALL -> Res.string.progress_range_all
}

@Composable
private fun RateCard(state: ProgressState) {
    val medido = state.weeklyRateKg ?: return
    val imperial = state.unitSystem == UnitSystem.IMPERIAL
    val unidade = stringResource(if (imperial) Res.string.common_lb else Res.string.common_kg)

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.progress_rate_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(
                Res.string.progress_rate_measured,
                "${fmtG(medido.display(imperial))} $unidade",
            ),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        state.desiredWeeklyRateKg?.let { pedido ->
            Text(
                stringResource(
                    Res.string.progress_rate_wanted,
                    "${fmtG(pedido.display(imperial))} $unidade",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightCard(state: ProgressState, onWeightHistory: () -> Unit) {
    val imperial = state.unitSystem == UnitSystem.IMPERIAL

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.progress_weight_title),
            style = MaterialTheme.typography.titleMedium,
        )

        AntaresChart(
            points = state.rangeWeightSeries.display(imperial),
            trend = state.rangeTrendSeries.display(imperial),
            targetValue = state.goalWeightKg?.display(imperial),
            modifier = Modifier.padding(top = Spacing.sm),
            labels = { escala, eixo -> ChartAxisLabels(escala, eixo) },
        )

        Text(
            stringResource(

                if (state.rangeWeightSeries.size < MIN_POINTS_FOR_TREND) {
                    Res.string.progress_weight_one_point
                } else {
                    Res.string.progress_trend_note
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )

        SecondaryButton(
            text = stringResource(Res.string.more_weight_history),
            onClick = onWeightHistory,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun ChartAxisLabels(scale: ChartScale, eixo: TimeAxis) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            fmtG(scale.min),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            fmtG(scale.max),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val marcas = eixo.tickDays()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        marcas.forEachIndexed { i, dia ->
            Text(
                axisDate(dia, eixo.spanDays),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,

                textAlign = when (i) {
                    0 -> TextAlign.Start
                    marcas.lastIndex -> TextAlign.End
                    else -> TextAlign.Center
                },
            )
        }
    }
}

@Composable
private fun BeenHereCard(visita: BeenHereCalc.Visit, state: ProgressState) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.beenhere_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(
                Res.string.beenhere_when,
                visita.daysAgo,
                dayShortDated(visita.epochDay),
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        if (!visita.hasComparison) {
            Text(
                stringResource(Res.string.beenhere_nothing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AntaresCard
        }

        visita.waistCm?.let { antes ->
            state.waistNowCm?.let { agora ->
                Text(
                    stringResource(Res.string.beenhere_waist, fmtG(antes), fmtG(agora)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        visita.bodyFatPct?.let { antes ->
            state.bodyFatNowPct?.let { agora ->
                Text(
                    stringResource(Res.string.beenhere_fat, fmtG(antes), fmtG(agora)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ConsistencyCard(state: ProgressState) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        SplitRow(
            leading = {
                Text(
                    stringResource(Res.string.progress_consistency_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            trailing = {
                Text(
                    stringResource(Res.string.progress_consistency_pct, state.consistencyPct),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        val marcado = MaterialTheme.success
        val vazio = MaterialTheme.colorScheme.surfaceVariant
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {

            WeekdayLegend()

            state.consistency.chunked(7).forEach { semana ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    semana.forEach { dia ->
                        Box(
                            Modifier
                                .size(CELL_DP.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {

                                        dia.inFuture -> vazio.copy(alpha = 0.3f)
                                        dia.logged -> marcado
                                        else -> vazio
                                    },
                                ),
                        )
                    }
                }
            }
        }
        Text(
            stringResource(Res.string.progress_consistency_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun WeekdayLegend() {
    val nomes = stringArrayResource(Res.array.weekdays_short)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {

        for (iso in 1..7) {
            val nome = nomes.getOrElse(iso % 7) { "" }
            Box(
                modifier = Modifier.size(width = LEGEND_DP.dp, height = CELL_DP.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(

                    nome.take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MonthComparisonCard(c: ProgressCalc.Comparison) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.progress_compare_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(Res.string.progress_compare_current, c.current.roundToInt()),
            style = MaterialTheme.typography.bodyLarge,
        )
        val delta = c.delta
        if (delta == null) {

            Text(
                stringResource(Res.string.progress_compare_no_previous),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(
                    if (delta >= 0) Res.string.progress_compare_up else Res.string.progress_compare_down,
                    abs(delta).roundToInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalTimelineCard(state: ProgressState) {
    val imperial = state.unitSystem == UnitSystem.IMPERIAL
    val unidade = stringResource(if (imperial) Res.string.common_lb else Res.string.common_kg)
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.progress_goals_title),
            style = MaterialTheme.typography.titleMedium,
        )
        state.goals.forEach { goal ->
            val alvo = "${fmtG(goal.targetKg.display(imperial))} $unidade"
            val atingido = goal.reachedOnEpochDay
            Text(
                if (atingido != null) {
                    stringResource(
                        Res.string.progress_goal_reached,
                        alvo,

                        dayShortDated(atingido),
                        goal.daysTaken ?: 0L,
                    )
                } else {
                    stringResource(Res.string.progress_goal_open, alvo)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun MilestonesCard(state: ProgressState) {
    val imperial = state.unitSystem == UnitSystem.IMPERIAL
    val unidade = stringResource(if (imperial) Res.string.common_lb else Res.string.common_kg)
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.progress_milestones_title),
            style = MaterialTheme.typography.titleMedium,
        )
        state.milestones.takeLast(MAX_MILESTONES).reversed().forEach { m ->
            Text(
                when (m.kind) {
                    ProgressCalc.Kind.LOGGING_DAYS ->
                        stringResource(Res.string.progress_milestone_days, m.value)
                    ProgressCalc.Kind.WEIGHT_CHANGE_KG -> {
                        val quanto = m.value.toDouble().display(imperial).roundToInt()
                        stringResource(Res.string.progress_milestone_weight, "$quanto $unidade")
                    }
                    ProgressCalc.Kind.GOAL_REACHED ->
                        stringResource(Res.string.progress_milestone_goal)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun PatternsCard(state: ProgressState) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.patterns_title),
            style = MaterialTheme.typography.titleMedium,
        )
        state.patterns.forEach { padrao ->
            Text(
                patternFact(padrao),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )

            if (state.patternSuggestions) {
                Text(
                    stringResource(patternTip(padrao.kind)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stringResource(Res.string.patterns_facts_only),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun patternFact(p: EatingPatterns.Pattern): String = when (p.kind) {
    EatingPatterns.Kind.WEEKEND_HIGHER ->
        stringResource(Res.string.patterns_weekend_higher, p.value)
    EatingPatterns.Kind.WEEKEND_LOWER ->
        stringResource(Res.string.patterns_weekend_lower, p.value)
    EatingPatterns.Kind.WEEKEND_PROTEIN_DROP ->
        stringResource(Res.string.patterns_weekend_protein, p.value)
    EatingPatterns.Kind.MEAL_CONCENTRATION ->
        stringResource(
            Res.string.patterns_concentration,
            p.value,
            mealSlotLabel(p.label),
        )

    // Os dois de baixo trazem minutos, e não uma contagem: um é uma duração, o outro uma
    // hora do dia.
    EatingPatterns.Kind.LONG_EATING_WINDOW ->
        stringResource(Res.string.patterns_long_window, formatDurationMin(p.value))
    EatingPatterns.Kind.LATE_LAST_MEAL ->
        stringResource(Res.string.patterns_late_meal, formatMinuteOfDay(p.value))
}

private fun patternTip(kind: EatingPatterns.Kind) = when (kind) {
    EatingPatterns.Kind.WEEKEND_HIGHER -> Res.string.patterns_tip_weekend_higher
    EatingPatterns.Kind.WEEKEND_LOWER -> Res.string.patterns_tip_weekend_lower
    EatingPatterns.Kind.WEEKEND_PROTEIN_DROP -> Res.string.patterns_tip_weekend_protein
    EatingPatterns.Kind.MEAL_CONCENTRATION -> Res.string.patterns_tip_concentration
    EatingPatterns.Kind.LONG_EATING_WINDOW -> Res.string.patterns_tip_long_window
    EatingPatterns.Kind.LATE_LAST_MEAL -> Res.string.patterns_tip_late_meal
}

private fun Double.display(imperial: Boolean): Double =
    UnitConversions.weightToDisplay(this, if (imperial) UnitSystem.IMPERIAL else UnitSystem.METRIC)

private fun List<Pair<Long, Double>>.display(imperial: Boolean): List<Pair<Long, Double>> =
    if (imperial) map { it.first to UnitConversions.kgToLb(it.second) } else this

private const val HERO_CHART_DP = 120

private const val HERO_CHART_ALPHA = 0.35f

private const val HERO_SCRIM_ALPHA = 0.82f
private const val HERO_SCRIM_RADIUS_DP = 16

private const val CELL_DP = 14
private const val LEGEND_DP = 16
private const val MAX_MILESTONES = 6

private const val CHANGE_EPSILON_KG = 0.05

private const val MIN_POINTS_FOR_TREND = 2
