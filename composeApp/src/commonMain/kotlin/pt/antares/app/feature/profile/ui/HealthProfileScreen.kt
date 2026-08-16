package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.BmiCategory
import pt.antares.app.core.calc.DailyGoals
import pt.antares.app.core.calc.ActivitySuggestion
import pt.antares.app.core.calc.AdaptiveTdee
import pt.antares.app.core.calc.BmrFormula
import pt.antares.app.core.calc.MeasurementProgress
import pt.antares.app.core.calc.BodyStats
import pt.antares.app.core.calc.WaistRisk
import pt.antares.app.core.designsystem.ratePerWeekWithUnit
import pt.antares.app.core.designsystem.lengthWithUnit
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.bodyWeightWithUnit
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.activityLevelLabel
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.BmiScale
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.designsystem.components.Sparkline
import pt.antares.app.core.designsystem.components.SplitRow
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.Sex
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HealthProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onWeightHistory: () -> Unit,
    onBodyComposition: () -> Unit,
    onShowMaths: () -> Unit,
    onMeasurementHistory: () -> Unit,
    onDietBreak: () -> Unit,
    onCoach: () -> Unit,
    onCycle: () -> Unit,
    viewModel: HealthProfileViewModel = koinViewModel(),
    goalChangeViewModel: GoalChangeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val goalChange by goalChangeViewModel.change.collectAsState()

    Scaffold(

        topBar = { AntaresTopBar(title = stringResource(Res.string.profile_health_title), onBack = onBack) },
    ) { padding ->
        if (state.loading || state.profile == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {

            goalChange?.let { GoalChangeCard(it, onDismiss = goalChangeViewModel::acknowledge) }

            TargetCard(state, onShowMaths)

            if (state.isStalled) StallCard(state, onDietBreak)

            state.activitySuggestion?.let {
                ActivitySuggestionCard(
                    suggestion = it,
                    onAccept = viewModel::acceptActivitySuggestion,
                    onDismiss = viewModel::dismissActivitySuggestion,
                )
            }

            CheckInCard(state, onCoach)

            SectionHeader(title = stringResource(Res.string.profile_health_body_section))
            WeightCard(state, onWeightHistory)

            if (state.showsCycleNote) CycleNoteCard(onCycle)

            state.body?.let { body ->
                BmiCard(body)

                if (state.hasOwnComposition) {
                    CompositionCard(body, onBodyComposition)
                } else {
                    AddCompositionCard(onBodyComposition)
                }
            }

            state.measurementProgress?.takeIf { it.isMeaningful }?.let {
                ProgressCard(it, onMeasurementHistory)
            }

            SectionHeader(title = stringResource(Res.string.profile_health_goal_section))
            GoalCard(state)
            ExpenditureCard(state)

            state.latestWeightKg?.let { weight ->
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(Res.string.profile_goals_other),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(
                            Res.string.profile_goal_water,
                            // Sem o treino do dia: este ecrã mostra a meta de base, e a do
                            // dia com treino aparece onde a água se regista.
                            DailyGoals.waterMl(state.profile?.sex ?: Sex.MALE, weight),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(Res.string.profile_goal_fibre, DailyGoals.fibreG()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            SecondaryButton(
                text = stringResource(Res.string.profile_health_edit),
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
            )

            AntaresCard(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                Text(
                    stringResource(Res.string.profile_help_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(Res.string.profile_help_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                stringResource(Res.string.profile_health_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun TargetCard(state: HealthProfileState, onShowMaths: () -> Unit) {
    val t = state.targets ?: return
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.profile_health_daily_target),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "${t.kcal} ${stringResource(Res.string.common_kcal)}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        val m = macroInitials()
        Text(
            "${m.p} ${t.proteinG}g · ${m.c} ${t.carbsG}g · ${m.f} ${t.fatG}g",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        t.energy?.let { e ->
            Text(
                stringResource(
                    when (e.formula) {
                        BmrFormula.MIFFLIN_ST_JEOR -> Res.string.profile_health_formula_mifflin
                        BmrFormula.KATCH_MCARDLE -> Res.string.profile_health_formula_katch
                        BmrFormula.CUNNINGHAM -> Res.string.profile_health_formula_cunningham
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // O basal medido pela fita não é um número exato, e mostrá-lo às décimas dizia
            // que era — ver `NavyUncertainty`.
            e.bmrUncertaintyKcal?.let { erro ->
                Text(
                    stringResource(
                        Res.string.profile_health_bmr_range,
                        e.bmr.roundToInt(),
                        erro.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.maintenanceKcal?.takeIf { it != t.kcal }?.let {
            Text(
                stringResource(Res.string.profile_health_maintenance, it, stringResource(Res.string.common_kcal)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }

        SecondaryButton(
            text = stringResource(Res.string.show_maths_cta),
            onClick = onShowMaths,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun ActivitySuggestionCard(
    suggestion: ActivitySuggestion.Suggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.activity_suggest_title),
            style = MaterialTheme.typography.titleMedium,
        )
        val sugerido = stringResource(activityLevelLabel(suggestion.suggested))
        val passos = suggestion.averageSteps.toString()
        Text(
            if (suggestion.current == null) {
                stringResource(Res.string.activity_suggest_body_unset, passos, sugerido)
            } else {
                stringResource(
                    Res.string.activity_suggest_body,
                    passos,
                    sugerido,
                    stringResource(activityLevelLabel(suggestion.current)),
                )
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            stringResource(Res.string.activity_suggest_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        SecondaryButton(
            text = stringResource(Res.string.activity_suggest_accept, sugerido),
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
        SecondaryButton(
            text = stringResource(Res.string.activity_suggest_dismiss),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun StallCard(state: HealthProfileState, onDietBreak: () -> Unit) {
    val assessment = AdaptiveTdee.assessPlateau(state.stallWeeks, state.loggedDaysPerWeek)
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.plateau_title, state.stallWeeks),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(
                when (assessment) {
                    AdaptiveTdee.Assessment.METABOLIC_ADAPTATION -> Res.string.plateau_adaptation
                    AdaptiveTdee.Assessment.LIKELY_UNDER_LOGGING -> Res.string.plateau_under_logging
                    AdaptiveTdee.Assessment.UNCLEAR -> Res.string.plateau_unclear
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (assessment == AdaptiveTdee.Assessment.METABOLIC_ADAPTATION) {
            SecondaryButton(

                text = stringResource(Res.string.plateau_diet_break_cta),
                onClick = onDietBreak,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun CheckInCard(state: HealthProfileState, onCoach: () -> Unit) {
    val weight = state.latestWeightKg ?: return
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.checkin_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(
                Res.string.checkin_weight,
                bodyWeightWithUnit(weight, rememberUnitSystem()),
                bodyWeightWithUnit(state.trendWeightKg ?: weight, rememberUnitSystem()),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            state.weeklyRateKg?.let {
                stringResource(
                    Res.string.checkin_rate,
                    ratePerWeekWithUnit(it, rememberUnitSystem()),
                )
            }
                ?: stringResource(Res.string.checkin_no_rate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton(
            text = stringResource(Res.string.checkin_open_coach),
            onClick = onCoach,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun CycleNoteCard(onCycle: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.cycle_note_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(Res.string.cycle_note_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SecondaryButton(
            text = stringResource(Res.string.more_cycle),
            onClick = onCycle,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun ProgressCard(progress: MeasurementProgress, onClick: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Text(
            stringResource(Res.string.measure_history_title),
            style = MaterialTheme.typography.titleSmall,
        )
        val from = progress.waistFrom
        val to = progress.waistTo
        if (from != null && to != null) {
            Text(
                stringResource(
                    Res.string.measure_waist_change,
                    lengthWithUnit(from, rememberUnitSystem()),
                    lengthWithUnit(to, rememberUnitSystem()),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val fatFrom = progress.fatFrom
        val fatTo = progress.fatTo
        if (fatFrom != null && fatTo != null) {
            Text(
                stringResource(Res.string.measure_fat_change, fmtG(fatFrom), fmtG(fatTo)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun GoalCard(state: HealthProfileState) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        val goal = state.goalWeightKg
        val current = state.latestWeightKg

        // Duas mensagens porque faltam duas coisas diferentes: sem pesagem não há
        // ponto de partida, mesmo com o alvo escolhido.
        if (goal == null || current == null) {
            Text(
                stringResource(
                    if (goal == null) {
                        Res.string.profile_health_no_goal_weight
                    } else {
                        Res.string.profile_health_no_weigh_in
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AntaresCard
        }

        Text(
            stringResource(
                Res.string.profile_health_goal_weight,
                bodyWeightWithUnit(goal, rememberUnitSystem()),
            ),
            style = MaterialTheme.typography.titleMedium,
        )

        val start = state.startWeightKg ?: current
        val total = abs(start - goal)
        val done = abs(start - current)

        if (total > 0.0) {
            LinearProgressIndicator(
                progress = { (done / total).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
            )
        }

        val p = state.projection
        Text(
            if (p?.reached == true) {
                stringResource(Res.string.profile_health_goal_reached)
            } else {
                stringResource(
                    Res.string.profile_health_goal_remaining,
                    bodyWeightWithUnit(
                        p?.remainingKg ?: abs(state.remainingToGoalKg ?: 0.0),
                        rememberUnitSystem(),
                    ),
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when {
            p == null || p.reached -> Unit
            p.movingAway -> Text(
                stringResource(Res.string.profile_health_moving_away),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            p.weeks != null && p.etaEpochDay != null -> {
                val date = epochDayToLocalDate(p.etaEpochDay)
                Text(
                    stringResource(
                        Res.string.profile_health_eta,
                        p.weeks,
                        "${date.monthNumber.toString().padStart(2, '0')}/${date.year}",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.profile_health_eta_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> Text(
                stringResource(Res.string.profile_health_no_eta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExpenditureCard(state: HealthProfileState) {
    val formula = state.targets?.energy?.tdee?.roundToInt() ?: return
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        val learned = state.learnedTdeeKcal
        val kcal = stringResource(Res.string.common_kcal)
        if (learned == null) {

            Text(
                stringResource(Res.string.profile_health_estimated_expenditure, formula, kcal),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(Res.string.profile_health_learned_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AntaresCard
        }
        Text(
            stringResource(Res.string.profile_health_real_expenditure, learned, kcal),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(Res.string.profile_health_formula_estimated, formula),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            if (state.adaptiveIsConfident) {
                stringResource(Res.string.profile_health_learned_confident, state.adaptiveWeeks)
            } else {
                stringResource(Res.string.profile_health_learned_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val GOAL_REACHED_TOLERANCE_KG = 0.3
