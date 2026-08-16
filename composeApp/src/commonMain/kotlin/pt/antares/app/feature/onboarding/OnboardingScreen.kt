package pt.antares.app.feature.onboarding

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.BmrFormula
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.calc.TargetWarning
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.StarField
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.GoalRates
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.UnitSystem
import kotlinx.datetime.LocalDate
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.core.util.toEpochDay
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.done) {
        if (state.done) onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        StarField(modifier = Modifier.fillMaxSize())
        Column(
        modifier = Modifier
            .fillMaxSize()

            .imePadding()
            .padding(Spacing.xl),
    ) {

        // O saltar fica em cima, ao lado da barra, e não junto ao continuar: são nove
        // passos, e quem quer só ver a app não devia ter de descer o ecrã para sair de uma
        // pergunta que a app sabe responder sozinha.
        val progress = OnboardingFlow.progress(state.step, state.goalType)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f),
            )
            if (OnboardingFlow.canSkip(state.step)) {
                TextButton(onClick = viewModel::skip) {
                    Text(stringResource(Res.string.onb_skip))
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (state.step) {
                OnboardingStep.WELCOME -> WelcomeStep()
                OnboardingStep.SEX -> SexStep(state, viewModel)
                OnboardingStep.BIRTH -> BirthStep(state, viewModel)
                OnboardingStep.BODY -> BodyStep(state, viewModel)
                OnboardingStep.ACTIVITY -> ActivityStep(state, viewModel)
                OnboardingStep.GOAL -> GoalStep(state, viewModel)
                OnboardingStep.GOAL_WEIGHT -> GoalWeightStep(state, viewModel)
                OnboardingStep.RATE -> RateStep(state, viewModel)
                OnboardingStep.PLAN_PREVIEW -> PlanStep(state, viewModel)
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (state.step != OnboardingStep.WELCOME) {
                SecondaryButton(
                    text = stringResource(Res.string.common_back),
                    onClick = { viewModel.back() },
                    modifier = Modifier.weight(1f),
                )
            }
            PrimaryButton(
                text = when (state.step) {
                    OnboardingStep.WELCOME -> stringResource(Res.string.onb_welcome_cta)
                    OnboardingStep.PLAN_PREVIEW -> stringResource(Res.string.onb_finish)
                    else -> stringResource(Res.string.common_continue)
                },
                onClick = viewModel::next,
                enabled = state.canContinue && !state.saving,
                modifier = Modifier.weight(2f),
            )
        }
        }
    }
}

/**
 * O primeiro ecrã. É o único sítio onde alguém lê o que a app faz de diferente antes de
 * decidir se a usa.
 *
 * O argumento — as contas à vista, os alimentos medidos em Portugal, nada sair do telemóvel —
 * estava numa frase só, corrida, com 60% da altura do ecrã vazia por baixo. Três afirmações
 * numa linha lêem-se como uma, e nenhuma fica.
 *
 * Agora são três, cada uma com o seu ícone e a sua explicação. O vazio enche-se com o que há
 * para dizer, e não esticando o que já lá estava.
 */
@Composable
private fun WelcomeStep() {
    Text(stringResource(Res.string.onb_welcome_title), style = MaterialTheme.typography.headlineMedium)
    Text(
        stringResource(Res.string.onb_welcome_lead),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.sm))

    ArgumentoRow(
        icon = Icons.Default.Calculate,
        titulo = stringResource(Res.string.onb_welcome_maths_title),
        corpo = stringResource(Res.string.onb_welcome_maths_body),
    )
    ArgumentoRow(
        icon = Icons.Default.Place,
        titulo = stringResource(Res.string.onb_welcome_food_title),
        corpo = stringResource(Res.string.onb_welcome_food_body),
    )
    ArgumentoRow(
        icon = Icons.Default.Lock,
        titulo = stringResource(Res.string.onb_welcome_offline_title),
        corpo = stringResource(Res.string.onb_welcome_offline_body),
    )

    Spacer(Modifier.height(Spacing.sm))
    Text(
        stringResource(Res.string.onb_welcome_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ArgumentoRow(icon: ImageVector, titulo: String, corpo: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Icon(
            imageVector = icon,
            // Decorativo: o texto ao lado diz tudo o que o ícone diz, e um leitor de ecrã
            // que os lesse aos dois repetia-se.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(ICONE_DP.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(
                corpo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val ICONE_DP = 28

@Composable
private fun SexStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Text(stringResource(Res.string.onb_sex_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(Res.string.onb_sex_subtitle), style = MaterialTheme.typography.bodyMedium)
    SelectableCard(
        title = stringResource(Res.string.onb_sex_male),
        selected = state.sex == Sex.MALE,
        onClick = { viewModel.setSex(Sex.MALE) },
    )
    SelectableCard(
        title = stringResource(Res.string.onb_sex_female),
        selected = state.sex == Sex.FEMALE,
        onClick = { viewModel.setSex(Sex.FEMALE) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Text(stringResource(Res.string.onb_birth_title), style = MaterialTheme.typography.headlineMedium)

    val currentYear = epochDayToLocalDate(todayEpochDay()).year

    // Sem isto o calendário abre no mês corrente, e quem tem 30 anos recua 360
    // meses a tocar na seta. Ninguém nasce este mês, por isso abrimos numa idade
    // adulta plausível — o ano continua a um toque no cabeçalho.
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.birthEpochDay?.times(86_400_000L),
        initialDisplayedMonthMillis = state.birthEpochDay?.times(86_400_000L)
            ?: LocalDate(currentYear - DEFAULT_BIRTH_AGE, 1, 1).toEpochDay() * 86_400_000L,
        yearRange = (currentYear - MAX_PLAUSIBLE_AGE)..currentYear,
    )
    LaunchedEffect(pickerState.selectedDateMillis) {
        pickerState.selectedDateMillis?.let { millis ->
            viewModel.setBirth(millis / 86_400_000L)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val naturalWidth = 360.dp
        val scale = (maxWidth / naturalWidth).coerceAtMost(1f)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .layout { measurable, _ ->

                    val placeable = measurable.measure(Constraints.fixedWidth(naturalWidth.roundToPx()))

                    val w = (placeable.width * scale).roundToInt()
                    val h = (placeable.height * scale).roundToInt()
                    layout(w, h) {
                        placeable.placeWithLayer(0, 0) {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }
                },
        ) {
            DatePicker(state = pickerState, showModeToggle = false, title = null, headline = null)
        }
    }
    if (state.underage) {
        Text(
            stringResource(Res.string.onb_birth_underage),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BodyStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val imperial = state.unitSystem == UnitSystem.IMPERIAL
    Text(stringResource(Res.string.onb_body_title), style = MaterialTheme.typography.headlineMedium)

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        UnitSystem.entries.forEachIndexed { indice, unidade ->
            SegmentedButton(
                selected = state.unitSystem == unidade,
                onClick = { viewModel.setUnitSystem(unidade) },
                shape = SegmentedButtonDefaults.itemShape(indice, UnitSystem.entries.size),

                label = {
                    Text(
                        stringResource(
                            if (unidade == UnitSystem.METRIC) {
                                Res.string.settings_units_metric_short
                            } else {
                                Res.string.settings_units_imperial_short
                            },
                        ),
                        maxLines = 1,
                    )
                },
            )
        }
    }
    if (imperial) {

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedTextField(
                value = state.heightFt,
                onValueChange = viewModel::setHeightFeet,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(Res.string.onb_body_height_ft)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.heightIn,
                onValueChange = viewModel::setHeightInches,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(Res.string.onb_body_height_in)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
    } else {
        OutlinedTextField(
            value = state.heightCm,
            onValueChange = viewModel::setHeight,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(Res.string.onb_body_height)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
    OutlinedTextField(
        value = state.weightKg,
        onValueChange = viewModel::setWeight,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        label = {
            Text(
                "${stringResource(Res.string.onb_body_weight)} " +
                    "(${stringResource(if (imperial) Res.string.common_lb else Res.string.common_kg)})",
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun GoalWeightStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val imperial = state.unitSystem == UnitSystem.IMPERIAL
    Text(stringResource(Res.string.onb_goal_weight_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(Res.string.onb_goal_weight_subtitle), style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(
        value = state.goalWeightInput,
        onValueChange = viewModel::setGoalWeight,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),

        label = {
            Text(
                "${stringResource(Res.string.onb_goal_weight_label)} " +
                    "(${stringResource(if (imperial) Res.string.common_lb else Res.string.common_kg)})",
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    if (state.goalWeightContradicts) {
        Text(
            stringResource(Res.string.onb_goal_weight_contradiction),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActivityStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Text(stringResource(Res.string.onb_activity_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(Res.string.onb_activity_subtitle), style = MaterialTheme.typography.bodyMedium)
    val options = listOf(
        Triple(ActivityLevel.SEDENTARY, Res.string.onb_activity_sedentary, Res.string.onb_activity_sedentary_desc),
        Triple(ActivityLevel.LIGHT, Res.string.onb_activity_light, Res.string.onb_activity_light_desc),
        Triple(ActivityLevel.MODERATE, Res.string.onb_activity_moderate, Res.string.onb_activity_moderate_desc),
        Triple(ActivityLevel.HIGH, Res.string.onb_activity_high, Res.string.onb_activity_high_desc),
        Triple(ActivityLevel.ATHLETE, Res.string.onb_activity_athlete, Res.string.onb_activity_athlete_desc),
    )
    options.forEach { (level, title, desc) ->
        SelectableCard(
            title = stringResource(title),
            subtitle = stringResource(desc),
            selected = state.activityLevel == level,
            onClick = { viewModel.setActivity(level) },
        )
    }
}

@Composable
private fun GoalStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Text(stringResource(Res.string.onb_goal_title), style = MaterialTheme.typography.headlineMedium)
    SelectableCard(
        title = stringResource(Res.string.onb_goal_lose),
        selected = state.goalType == GoalType.LOSE,
        onClick = { viewModel.setGoal(GoalType.LOSE) },
    )
    SelectableCard(
        title = stringResource(Res.string.onb_goal_maintain),
        selected = state.goalType == GoalType.MAINTAIN,
        onClick = { viewModel.setGoal(GoalType.MAINTAIN) },
    )
    SelectableCard(
        title = stringResource(Res.string.onb_goal_gain),
        selected = state.goalType == GoalType.GAIN,
        onClick = { viewModel.setGoal(GoalType.GAIN) },
    )
}

@Composable
private fun RateStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Text(stringResource(Res.string.onb_rate_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(Res.string.onb_rate_subtitle), style = MaterialTheme.typography.bodyMedium)

    val losing = state.goalType == GoalType.LOSE
    val range = if (losing) GoalRates.LOSE_RANGE_KG_WEEK else GoalRates.GAIN_RANGE_KG_WEEK
    val default = if (losing) GoalRates.DEFAULT_LOSE_KG_WEEK else GoalRates.DEFAULT_GAIN_KG_WEEK
    val current = state.goalRateKcal
        ?.let { abs(NutritionCalc.weeklyKgFromKcalPerDay(it)) }
        ?.coerceIn(range.start, range.endInclusive)
        ?: default

    LaunchedEffect(state.goalType) {
        if (state.goalRateKcal == null) viewModel.setWeeklyRate(default)
    }

    Text(
        stringResource(
            if (losing) Res.string.settings_rate_lose else Res.string.settings_rate_gain,
            fmtG(current),
        ),
        style = MaterialTheme.typography.titleLarge,
    )
    val steps = ((range.endInclusive - range.start) / GoalRates.STEP_KG_WEEK).roundToInt() - 1
    Slider(
        value = current.toFloat(),
        onValueChange = { viewModel.setWeeklyRate(it.toDouble()) },
        valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
        steps = steps,
        modifier = Modifier.fillMaxWidth(),
    )
    state.goalRateKcal?.let { rate ->
        Text(
            stringResource(
                if (losing) Res.string.settings_rate_deficit else Res.string.settings_rate_surplus,
                abs(rate),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val age = state.birthEpochDay?.let { NutritionCalc.ageYears(it, todayEpochDay()) }
    state.weightKg.replace(',', '.').toDoubleOrNull()?.let { weight ->
        val safe = if (losing) {
            NutritionCalc.safeWeeklyLossKg(weight, age)
        } else {
            NutritionCalc.safeWeeklyGainKg(weight)
        }
        Text(
            stringResource(
                Res.string.settings_rate_safe_zone,
                fmtG(safe.start),
                fmtG(safe.endInclusive),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (current > safe.endInclusive) {
            Text(
                stringResource(
                    if (losing) Res.string.settings_rate_too_fast_lose
                    else Res.string.settings_rate_too_fast_gain,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PlanStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Text(stringResource(Res.string.onb_plan_title), style = MaterialTheme.typography.headlineMedium)
    val t = state.preview ?: return
    val kcal = stringResource(Res.string.common_kcal)

    t.energy?.let { e ->
        val tdee = e.tdee.roundToInt()
        val bmr = e.bmr.roundToInt()
        Text(stringResource(Res.string.onb_plan_burn), style = MaterialTheme.typography.titleMedium)
        Text(
            "~$tdee $kcal",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(Res.string.onb_plan_burn_breakdown, bmr, tdee - bmr),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (e.formula == BmrFormula.KATCH_MCARDLE) {
            Text(
                stringResource(Res.string.onb_plan_formula_katch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.md))

        val diff = t.kcal - tdee
        if (state.goalType != GoalType.MAINTAIN && diff != 0) {
            val weekly = abs(NutritionCalc.weeklyKgFromKcalPerDay(state.goalRateKcal ?: 0))
            Text(
                stringResource(
                    if (state.goalType == GoalType.LOSE) {
                        Res.string.onb_plan_to_lose
                    } else {
                        Res.string.onb_plan_to_gain
                    },
                    fmtG(weekly),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    Text(stringResource(Res.string.onb_plan_target), style = MaterialTheme.typography.titleMedium)
    Text("${t.kcal} $kcal", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
    t.energy?.let { e ->
        val diff = t.kcal - e.tdee.roundToInt()
        if (diff != 0) {
            Text(
                stringResource(
                    if (diff < 0) Res.string.onb_plan_less_than_burn else Res.string.onb_plan_more_than_burn,
                    abs(diff),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (TargetWarning.FLOOR_CLAMPED in t.warnings || TargetWarning.BMR_FLOOR_CLAMPED in t.warnings) {
        Text(
            stringResource(Res.string.onb_rate_floor_warning),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Spacer(Modifier.height(Spacing.md))
    Text(stringResource(Res.string.onb_plan_macros), style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = state.proteinG,
            onValueChange = viewModel::editProtein,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(Res.string.onb_plan_protein)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.carbsG,
            onValueChange = viewModel::editCarbs,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(Res.string.onb_plan_carbs)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.fatG,
            onValueChange = viewModel::editFat,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(Res.string.onb_plan_fat)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }
    if (!state.macrosSumOk) {
        Text(
            stringResource(Res.string.onb_plan_sum_invalid),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (state.macrosEdited) {
        SecondaryButton(
            text = stringResource(Res.string.onb_plan_reset),
            onClick = viewModel::resetMacrosToPreset,
        )
    }

    Spacer(Modifier.height(Spacing.md))
    Text(
        stringResource(Res.string.onb_plan_estimates_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun SelectableCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val Int.dp get() = androidx.compose.ui.unit.Dp(this.toFloat())

private const val MAX_PLAUSIBLE_AGE = 110

/** Só decide onde o calendário abre; não pré-preenche data nenhuma. */
private const val DEFAULT_BIRTH_AGE = 30
