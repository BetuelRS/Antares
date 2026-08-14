package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.BmrFormula
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.ConfirmDialog
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.GoalRates
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.datastore.StoredAiUsage
import pt.antares.app.core.nutrition.LifeStageDrv
import pt.antares.app.core.nutrition.microLabelRes
import pt.antares.app.core.privacy.PrivacyViewModel
import pt.antares.app.core.util.UnitConversions
import pt.antares.app.core.util.epochDayToLocalDate
import pt.antares.app.core.util.rememberZipSharer
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.onboarding.SelectableCard
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.settings_profile_title), onBack = onBack) },
    ) { padding ->
        val profile = state.profile
        if (state.loading || profile == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {

            state.targets?.let { t ->
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.settings_current_targets), style = MaterialTheme.typography.titleMedium)
                    val kcalLabel = stringResource(Res.string.common_kcal)
                    Text("${t.kcal} $kcalLabel", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    val m = macroInitials()
                    Text(
                        "${m.p} ${t.proteinG}g · ${m.c} ${t.carbsG}g · ${m.f} ${t.fatG}g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (viewModel.floorWarningActive()) {
                        Text(
                            stringResource(Res.string.onb_rate_floor_warning),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            SectionHeader(title = stringResource(Res.string.settings_section_body))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = profile.sex == Sex.MALE,
                    onClick = { viewModel.setSex(Sex.MALE) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(stringResource(Res.string.onb_sex_male)) }
                SegmentedButton(
                    selected = profile.sex == Sex.FEMALE,
                    onClick = { viewModel.setSex(Sex.FEMALE) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(stringResource(Res.string.onb_sex_female)) }
            }

            if (profile.unitSystem == UnitSystem.IMPERIAL) {
                val (ft0, in0) = UnitConversions.cmToFtIn(profile.heightCm)
                var feet by remember(profile.heightCm) { mutableStateOf("$ft0") }
                var inches by remember(profile.heightCm) { mutableStateOf("$in0") }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = feet,
                        onValueChange = { text ->
                            feet = text.filter(Char::isDigit).take(1)
                            val f = feet.toIntOrNull()
                            val i = inches.toIntOrNull()
                            if (f != null && i != null) viewModel.setHeight(UnitConversions.ftInToCm(f, i))
                        },
                        label = { Text(stringResource(Res.string.settings_height_ft)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = inches,
                        onValueChange = { text ->
                            inches = text.filter(Char::isDigit).take(2)
                            val f = feet.toIntOrNull()
                            val i = inches.toIntOrNull()
                            if (f != null && i != null) viewModel.setHeight(UnitConversions.ftInToCm(f, i))
                        },
                        label = { Text(stringResource(Res.string.settings_height_in)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                var heightText by remember(profile.heightCm) { mutableStateOf("${profile.heightCm}") }
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { text ->
                        heightText = text.filter(Char::isDigit).take(3)
                        heightText.toIntOrNull()?.let(viewModel::setHeight)
                    },
                    label = { Text("${stringResource(Res.string.settings_height)} (${stringResource(Res.string.common_cm)})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            if (viewModel.heightCheckDue()) {
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(Res.string.height_recheck_title, profile.heightCm),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(Res.string.height_recheck_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SecondaryButton(
                        text = stringResource(Res.string.height_recheck_confirm),
                        onClick = viewModel::confirmHeight,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    )
                }
            }

            if (profile.bodyFatPct != null) {
                SectionHeader(title = stringResource(Res.string.settings_formula_title))
                SelectableCard(
                    title = stringResource(Res.string.settings_formula_auto),
                    subtitle = stringResource(Res.string.settings_formula_auto_desc),
                    selected = profile.bmrFormulaOverride == null,
                    onClick = { viewModel.setBmrFormula(null) },
                )
                SelectableCard(
                    title = stringResource(Res.string.settings_formula_cunningham),
                    subtitle = stringResource(Res.string.settings_formula_cunningham_desc),
                    selected = profile.bmrFormulaOverride == BmrFormula.CUNNINGHAM,
                    onClick = { viewModel.setBmrFormula(BmrFormula.CUNNINGHAM) },
                )
            }

            SectionHeader(title = stringResource(Res.string.settings_trend_window))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = (profile.trendWindowDays ?: 7) == 7,
                    onClick = { viewModel.setTrendWindow(7) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(stringResource(Res.string.settings_trend_window_7)) }
                SegmentedButton(
                    selected = profile.trendWindowDays == 28,
                    onClick = { viewModel.setTrendWindow(28) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(stringResource(Res.string.settings_trend_window_28)) }
            }
            Text(
                stringResource(Res.string.settings_trend_window_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader(title = stringResource(Res.string.settings_activity))
            val activityLabels = listOf(
                ActivityLevel.SEDENTARY to Res.string.onb_activity_sedentary,
                ActivityLevel.LIGHT to Res.string.onb_activity_light,
                ActivityLevel.MODERATE to Res.string.onb_activity_moderate,
                ActivityLevel.HIGH to Res.string.onb_activity_high,
                ActivityLevel.ATHLETE to Res.string.onb_activity_athlete,
            )
            activityLabels.forEach { (level, label) ->
                SelectableCard(
                    title = stringResource(label),
                    selected = profile.activityLevel == level,
                    onClick = { viewModel.setActivity(level) },
                )
            }

            SectionHeader(title = stringResource(Res.string.settings_section_goal))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = profile.goalType == GoalType.LOSE,
                    onClick = { viewModel.setGoal(GoalType.LOSE) },
                    shape = SegmentedButtonDefaults.itemShape(0, 3),
                ) { Text(stringResource(Res.string.onb_goal_lose)) }
                SegmentedButton(
                    selected = profile.goalType == GoalType.MAINTAIN,
                    onClick = { viewModel.setGoal(GoalType.MAINTAIN) },
                    shape = SegmentedButtonDefaults.itemShape(1, 3),
                ) { Text(stringResource(Res.string.onb_goal_maintain)) }
                SegmentedButton(
                    selected = profile.goalType == GoalType.GAIN,
                    onClick = { viewModel.setGoal(GoalType.GAIN) },
                    shape = SegmentedButtonDefaults.itemShape(2, 3),
                ) { Text(stringResource(Res.string.onb_goal_gain)) }
            }

            SelectableCard(
                title = stringResource(Res.string.goal_recomp),
                subtitle = stringResource(Res.string.goal_recomp_desc),
                selected = profile.goalType == GoalType.RECOMP,
                onClick = { viewModel.setGoal(GoalType.RECOMP) },
            )

            if (profile.goalType != GoalType.MAINTAIN) {
                WeeklyRateSection(
                    goal = profile.goalType,
                    goalRateKcal = profile.goalRateKcal,
                    weightKg = state.latestWeightKg,
                    ageYears = NutritionCalc.ageYears(profile.birthEpochDay, todayEpochDay()),
                    aboveSafeZone = viewModel.rateAboveSafeZone(),
                    deficitSuspended = viewModel.pregnancyRemovedDeficit(),
                    onRateChange = viewModel::setWeeklyRate,
                )
            }

            val goalWeightInitial = profile.goalWeightKg?.let { fmtG(it) } ?: ""
            var goalWeightText by remember(profile.goalWeightKg) {
                mutableStateOf(goalWeightInitial)
            }
            OutlinedTextField(
                value = goalWeightText,
                onValueChange = { text ->
                    goalWeightText = text
                    viewModel.setGoalWeight(text.trim().replace(',', '.').toDoubleOrNull())
                },
                label = { Text(stringResource(Res.string.settings_goal_weight)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (viewModel.goalWeightBelowHealthy()) {
                val range = viewModel.healthyRange()
                Text(
                    if (range != null) {
                        stringResource(
                            Res.string.settings_goal_weight_low_range,
                            fmtG(range.start),
                            fmtG(range.endInclusive),
                        )
                    } else {
                        stringResource(Res.string.settings_goal_weight_low)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            val goalFatInitial = profile.goalBodyFatPct?.let { fmtG(it) } ?: ""
            var goalFatText by remember(profile.goalBodyFatPct) { mutableStateOf(goalFatInitial) }
            OutlinedTextField(
                value = goalFatText,
                onValueChange = { text ->
                    goalFatText = text
                    viewModel.setGoalBodyFat(text.trim().replace(',', '.').toDoubleOrNull())
                },
                label = { Text(stringResource(Res.string.settings_goal_body_fat)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (viewModel.pregnancyRemovedDeficit()) {
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(Res.string.settings_pregnancy_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(Res.string.settings_pregnancy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (viewModel.goalWeightReached()) {
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(Res.string.settings_goal_reached_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(Res.string.settings_goal_reached_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SecondaryButton(
                        text = stringResource(Res.string.settings_goal_reached_cta),
                        onClick = viewModel::switchToMaintenance,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    )
                }
            }

            SectionHeader(title = stringResource(Res.string.settings_section_macros))
            val strategies = listOf(
                MacroStrategy.BALANCED to Res.string.settings_strategy_balanced,
                MacroStrategy.HIGH_PROTEIN to Res.string.settings_strategy_high_protein,
                MacroStrategy.LOW_CARB to Res.string.settings_strategy_low_carb,
                MacroStrategy.KETO to Res.string.settings_strategy_keto,
                MacroStrategy.CUSTOM to Res.string.settings_strategy_custom,
            )
            strategies.forEach { (strategy, label) ->
                SelectableCard(
                    title = stringResource(label),
                    selected = profile.macroStrategy == strategy,
                    onClick = { viewModel.setStrategy(strategy) },
                )
            }

            if (profile.macroStrategy == MacroStrategy.CUSTOM) {
                CustomMacrosEditor(
                    initialP = profile.customProteinG ?: 0,
                    initialC = profile.customCarbsG ?: 0,
                    initialF = profile.customFatG ?: 0,
                    onCommit = viewModel::setCustomMacros,
                )
            }

            SectionHeader(title = stringResource(Res.string.settings_section_units))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = profile.unitSystem == UnitSystem.METRIC,
                    onClick = { viewModel.setUnitSystem(UnitSystem.METRIC) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(stringResource(Res.string.settings_units_metric)) }
                SegmentedButton(
                    selected = profile.unitSystem == UnitSystem.IMPERIAL,
                    onClick = { viewModel.setUnitSystem(UnitSystem.IMPERIAL) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(stringResource(Res.string.settings_units_imperial)) }
            }

            SectionHeader(title = stringResource(Res.string.settings_section_exercise))
            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.settings_addback), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(Res.string.settings_addback_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingSwitchRow(
                title = stringResource(Res.string.settings_pattern_suggestions),
                desc = stringResource(Res.string.settings_pattern_suggestions_desc),
                checked = state.patternSuggestions,
                onChange = viewModel::setPatternSuggestions,
            )

            if (profile.sex == Sex.FEMALE) LifeStageSection(profile.lifeStage, viewModel)

            SectionHeader(title = stringResource(Res.string.settings_section_fasting))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.settings_fasting_notif), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(Res.string.settings_fasting_notif_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.fastingNotifications,
                    onCheckedChange = viewModel::setFastingNotifications,
                )
            }

            SectionHeader(title = stringResource(Res.string.settings_section_notifications))
            SettingSwitchRow(
                title = stringResource(Res.string.settings_notif_meals),
                desc = stringResource(Res.string.settings_notif_meals_desc),
                checked = state.mealReminders,
                onChange = viewModel::setMealReminders,
            )
            SettingSwitchRow(
                title = stringResource(Res.string.settings_notif_weighin),
                desc = stringResource(Res.string.settings_notif_weighin_desc),
                checked = state.weighInReminder,
                onChange = viewModel::setWeighInReminder,
            )
            SettingSwitchRow(
                title = stringResource(Res.string.settings_notif_coach),
                desc = stringResource(Res.string.settings_notif_coach_desc),
                checked = state.coachReadyNotif,
                onChange = viewModel::setCoachReadyNotif,
            )
            SettingSwitchRow(
                title = stringResource(Res.string.settings_notif_quiet),
                desc = stringResource(Res.string.settings_notif_quiet_desc),
                checked = state.quietHours,
                onChange = viewModel::setQuietHours,
            )

            state.aiUsage?.let { usage ->
                SectionHeader(title = stringResource(Res.string.settings_section_ai))
                AiQuotaMeter(usage)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.settings_adaptive_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(Res.string.settings_adaptive_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.adaptiveTargets,
                    onCheckedChange = viewModel::setAdaptiveTargets,
                )
            }

            SectionHeader(title = stringResource(Res.string.settings_section_privacy))
            PrivacySection()
        }
    }
}

@Composable
private fun CustomMacrosEditor(
    initialP: Int,
    initialC: Int,
    initialF: Int,
    onCommit: (Int, Int, Int) -> Unit,
) {
    var p by remember { mutableStateOf("$initialP") }
    var c by remember { mutableStateOf("$initialC") }
    var f by remember { mutableStateOf("$initialF") }

    LaunchedEffect(p, c, f) {
        val pi = p.toIntOrNull() ?: return@LaunchedEffect
        val ci = c.toIntOrNull() ?: return@LaunchedEffect
        val fi = f.toIntOrNull() ?: return@LaunchedEffect
        kotlinx.coroutines.delay(600)
        onCommit(pi, ci, fi)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = p,
            onValueChange = { p = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(Res.string.onb_plan_protein)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = c,
            onValueChange = { c = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(Res.string.onb_plan_carbs)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = f,
            onValueChange = { f = it.filter(Char::isDigit).take(3) },
            label = { Text(stringResource(Res.string.onb_plan_fat)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }
}

@Composable
private fun AiQuotaMeter(usage: StoredAiUsage) {
    val today = todayEpochDay().let { epochDayToLocalDate(it).toString() }
    val remaining = usage.remaining(today)

    val used = (usage.limit - remaining).coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(
                if (usage.trial) Res.string.settings_ai_trial_meter
                else Res.string.settings_ai_pro_meter,
                used,
                usage.limit,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        LinearProgressIndicator(
            progress = { if (usage.limit > 0) used.toFloat() / usage.limit else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(
                if (usage.trial) Res.string.settings_ai_trial_hint
                else Res.string.settings_ai_pro_hint,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
