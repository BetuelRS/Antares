package pt.antares.app.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import pt.antares.app.core.calc.BmrFormula
import pt.antares.app.core.calc.BodyComposition
import pt.antares.app.core.calc.HeightCheck
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.calc.TargetWarning
import pt.antares.app.core.calc.Targets
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.EnergyUnit
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.GoalRates
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.datastore.WATER_REMINDER_DEFAULT_H
import pt.antares.app.core.datastore.StoredAiUsage
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.onboarding.OnboardingStep
import pt.antares.app.feature.profile.data.ProfileRepository

data class ProfileSettingsState(
    val loading: Boolean = true,
    val profile: UserProfileEntity? = null,
    val latestWeightKg: Double? = null,

    val targets: Targets? = null,
    val fastingNotifications: Boolean = true,
    val adaptiveTargets: Boolean = true,

    val patternSuggestions: Boolean = false,

    val mealReminders: Boolean = true,
    val weighInReminder: Boolean = true,
    val coachReadyNotif: Boolean = true,
    val quietHours: Boolean = true,

    // Desligado por omissão, ao contrário dos outros três.
    val waterReminder: Boolean = false,
    val waterReminderIntervalH: Int = WATER_REMINDER_DEFAULT_H,

    val aiUsage: StoredAiUsage? = null,
    val saved: Boolean = false,
)

class ProfileSettingsViewModel(
    private val repository: ProfileRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSettingsState())
    val state: StateFlow<ProfileSettingsState> = _state

    init {

        // Os fluxos são juntos em três andares porque o `combine` do Kotlin só aceita cinco
        // de cada vez, e aqui há treze. Os agrupamentos são por tema, não por conveniência.
        val base = combine(
            repository.observeProfile(),
            repository.observeLatestWeight(),
            repository.observeTargets(),
            preferences.fastingNotifications,
        ) { profile, weight, targets, fastingNotif ->
            ProfileSettingsState(
                loading = false,
                profile = profile,
                latestWeightKg = weight?.weightKg,
                targets = targets,
                fastingNotifications = fastingNotif,
            )
        }

        val notifFlags = combine(
            preferences.mealReminders,
            preferences.weighInReminder,
            preferences.coachReadyNotif,
            preferences.quietHoursEnabled,
            combine(preferences.waterReminder, preferences.waterReminderIntervalH) { on, h -> on to h },
        ) { meal, weighIn, coach, quiet, agua ->
            NotifFlags(meal, weighIn, coach, quiet, agua.first, agua.second)
        }

        val withAi = combine(
            base,
            preferences.adaptiveTargets,
            preferences.aiUsage,
            preferences.patternSuggestions,
        ) { state, adaptive, usage, sugestoes ->
            state.copy(adaptiveTargets = adaptive, aiUsage = usage, patternSuggestions = sugestoes)
        }

        combine(withAi, notifFlags) { state, flags ->
            state.copy(
                mealReminders = flags.meal,
                weighInReminder = flags.weighIn,
                coachReadyNotif = flags.coach,
                quietHours = flags.quiet,
                waterReminder = flags.water,
                waterReminderIntervalH = flags.waterIntervalH,
            )
        }
            // `saved` sobrevive à emissão nova: é o aviso de "guardado" no ecrã, e vem de
            // uma ação e não da base. Sem isto, gravar o perfil apagava o próprio aviso,
            // porque a alteração dispara logo uma emissão que o levaria consigo.
            .onEach { fresh -> _state.update { fresh.copy(saved = it.saved) } }
            .launchIn(viewModelScope)
    }

    private data class NotifFlags(
        val meal: Boolean,
        val weighIn: Boolean,
        val coach: Boolean,
        val quiet: Boolean,
        val water: Boolean,
        val waterIntervalH: Int,
    )

    fun setWaterReminder(enabled: Boolean) =
        viewModelScope.launch { preferences.setWaterReminder(enabled) }.let {}

    fun setWaterReminderInterval(hours: Int) =
        viewModelScope.launch { preferences.setWaterReminderIntervalH(hours) }.let {}

    fun setMealReminders(enabled: Boolean) =
        viewModelScope.launch { preferences.setMealReminders(enabled) }.let {}

    fun setWeighInReminder(enabled: Boolean) =
        viewModelScope.launch { preferences.setWeighInReminder(enabled) }.let {}

    fun setCoachReadyNotif(enabled: Boolean) =
        viewModelScope.launch { preferences.setCoachReadyNotif(enabled) }.let {}

    fun setQuietHours(enabled: Boolean) =
        viewModelScope.launch { preferences.setQuietHoursEnabled(enabled) }.let {}

    fun setFastingNotifications(enabled: Boolean) {
        viewModelScope.launch { preferences.setFastingNotifications(enabled) }
    }

    fun setPatternSuggestions(enabled: Boolean) {
        viewModelScope.launch { preferences.setPatternSuggestions(enabled) }
    }

    fun setAdaptiveTargets(enabled: Boolean) {
        viewModelScope.launch { preferences.setAdaptiveTargets(enabled) }
    }

    /**
     * Todas as alterações ao perfil passam por aqui. Cada uma grava logo, sem botão de
     * confirmar: são definições, e o ecrã mostra o efeito na meta em tempo real.
     */
    private fun save(transform: (UserProfileEntity) -> UserProfileEntity) {
        val current = _state.value.profile ?: return
        viewModelScope.launch {
            repository.saveProfile(transform(current))
            _state.update { it.copy(saved = true) }
        }
    }

    fun setSex(sex: Sex) = save { it.copy(sex = sex) }
    fun setHeight(cm: Int) {
        if (cm in 100..250) save { it.copy(heightCm = cm) }
    }

    fun setActivity(level: ActivityLevel) {
        respondido(OnboardingStep.ACTIVITY)
        save { it.copy(activityLevel = level) }
    }

    /**
     * Responder aqui apaga a pergunta que ficou pendente do arranque. Sem isto, o cartão do
     * Hoje continuava a pedir uma resposta que já tinha sido dada — e um aviso que não
     * desaparece quando se faz o que ele pede deixa de ser lido.
     */
    private fun respondido(step: OnboardingStep) {
        viewModelScope.launch { preferences.clearOnboardingSkipped(step.name) }
    }

    // Mudar de objetivo repõe o ritmo por omissão desse objetivo. Manter o anterior daria
    // um défice a quem acabou de escolher ganhar peso.
    fun setGoal(goal: GoalType) = save {
        respondido(OnboardingStep.GOAL)

        val rate = when (goal) {
            GoalType.MAINTAIN -> GoalRates.MAINTAIN
            GoalType.LOSE -> NutritionCalc.kcalPerDayFromWeeklyKg(-GoalRates.DEFAULT_LOSE_KG_WEEK)
            GoalType.GAIN -> NutritionCalc.kcalPerDayFromWeeklyKg(GoalRates.DEFAULT_GAIN_KG_WEEK)

            GoalType.RECOMP -> NutritionCalc.kcalPerDayFromWeeklyKg(-GoalRates.RECOMP_KG_WEEK)
        }
        it.copy(goalType = goal, goalRateKcal = rate)
    }

    fun setWeeklyRate(kgPerWeek: Double) = save { profile ->
        respondido(OnboardingStep.RATE)
        val magnitude = abs(kgPerWeek)

        // A recomposição conta como perda: o défice é pequeno, mas é défice — o que muda é
        // a composição, e não a direção.
        val losing = profile.goalType == GoalType.LOSE || profile.goalType == GoalType.RECOMP
        val signed = if (losing) -magnitude else magnitude
        profile.copy(goalRateKcal = NutritionCalc.kcalPerDayFromWeeklyKg(signed))
    }

    fun setGoalWeight(kg: Double?) = save {
        respondido(OnboardingStep.GOAL_WEIGHT)
        it.copy(goalWeightKg = kg?.takeIf { v -> v in PLAUSIBLE_GOAL_WEIGHT_KG })
    }

    fun setStrategy(strategy: MacroStrategy) = save { profile ->
        if (strategy == MacroStrategy.CUSTOM) {

            // Passar a manual arranca com os números que estavam a valer, em vez de campos
            // vazios: dá um ponto de partida a quem só quer mexer num dos três.
            val t = _state.value.targets
            profile.copy(
                macroStrategy = strategy,
                customProteinG = t?.proteinG,
                customCarbsG = t?.carbsG,
                customFatG = t?.fatG,
            )
        } else {
            profile.copy(macroStrategy = strategy)
        }
    }

    fun setCustomMacros(proteinG: Int, carbsG: Int, fatG: Int) = save {
        it.copy(
            macroStrategy = MacroStrategy.CUSTOM,
            customProteinG = proteinG,
            customCarbsG = carbsG,
            customFatG = fatG,
        )
    }

    fun setLifeStage(stage: LifeStage) = save { it.copy(lifeStage = stage) }

    fun setUnitSystem(system: UnitSystem) = save { it.copy(unitSystem = system) }
    fun setEnergyUnit(unit: EnergyUnit) = save { it.copy(energyUnit = unit) }

    fun floorWarningActive(): Boolean {
        val warnings = _state.value.targets?.warnings ?: return false
        return TargetWarning.FLOOR_CLAMPED in warnings || TargetWarning.BMR_FLOOR_CLAMPED in warnings
    }

    fun rateAboveSafeZone(): Boolean =
        _state.value.targets?.warnings?.contains(TargetWarning.RATE_ABOVE_SAFE_ZONE) == true

    fun goalWeightReached(): Boolean =
        _state.value.targets?.warnings?.contains(TargetWarning.GOAL_WEIGHT_REACHED) == true

    fun pregnancyRemovedDeficit(): Boolean =
        _state.value.targets?.warnings?.contains(TargetWarning.NO_DEFICIT_IN_PREGNANCY) == true

    fun goalWeightBelowHealthy(): Boolean {
        val p = _state.value.profile ?: return false
        val goal = p.goalWeightKg ?: return false
        return BodyComposition.isGoalWeightBelowHealthy(goal, p.heightCm)
    }

    fun healthyRange(): ClosedFloatingPointRange<Double>? =
        _state.value.profile?.let { BodyComposition.healthyWeightRange(it.heightCm) }

    fun switchToMaintenance() = save {
        it.copy(goalType = GoalType.MAINTAIN, goalRateKcal = GoalRates.MAINTAIN)
    }

    fun heightCheckDue(): Boolean {
        val p = _state.value.profile ?: return false
        val today = todayEpochDay()
        return HeightCheck.isDue(
            ageYears = NutritionCalc.ageYears(p.birthEpochDay, today),
            confirmedEpochDay = p.heightConfirmedEpochDay,

            // Milissegundos para dias por divisão inteira, sem passar pelo fuso: o que
            // interessa é a distância entre duas datas, e um dia de erro não muda nada
            // numa verificação que acontece de dois em dois anos.
            profileUpdatedEpochDay = p.updatedAt / MS_PER_DAY,
            todayEpochDay = today,
        )
    }

    fun confirmHeight() = save { it.copy(heightConfirmedEpochDay = todayEpochDay()) }

    fun setBmrFormula(formula: BmrFormula?) = save { it.copy(bmrFormulaOverride = formula) }

    fun setTrendWindow(days: Int) = save { it.copy(trendWindowDays = days) }

    fun setGoalBodyFat(pct: Double?) = save {
        it.copy(goalBodyFatPct = pct?.takeIf { v -> v in PLAUSIBLE_BODY_FAT_PCT })
    }

    companion object {

        // Valores fora destes intervalos são engano de digitação, e são descartados em
        // silêncio: o campo volta ao que estava em vez de aceitar um objetivo impossível.
        private val PLAUSIBLE_GOAL_WEIGHT_KG = 30.0..300.0

        private val PLAUSIBLE_BODY_FAT_PCT = 3.0..60.0

        private const val MS_PER_DAY = 86_400_000L
    }
}
