package pt.antares.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.calc.Targets
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.model.ActivityLevel
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.GoalRates
import pt.antares.app.core.model.MacroStrategy
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.ProfileRepository
import kotlin.math.abs

enum class OnboardingStep {
    WELCOME, SEX, BIRTH, BODY, ACTIVITY, GOAL, GOAL_WEIGHT, RATE, PLAN_PREVIEW,
}

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,

    val sex: Sex? = null,
    val birthEpochDay: Long? = null,

    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val heightCm: String = "",

    val heightFt: String = "",
    val heightIn: String = "",
    val weightKg: String = "",
    val activityLevel: ActivityLevel? = null,
    val goalType: GoalType? = null,

    val goalWeightInput: String = "",
    val goalRateKcal: Int? = null,
    val macroStrategy: MacroStrategy = MacroStrategy.BALANCED,

    val proteinG: String = "",
    val carbsG: String = "",
    val fatG: String = "",
    val macrosEdited: Boolean = false,

    val preview: Targets? = null,
    val underage: Boolean = false,
    val saving: Boolean = false,
    val done: Boolean = false,
) {

    val parsedHeightCm: Int?
        get() = OnboardingInput.heightCm(unitSystem, heightCm, heightFt, heightIn)

    val parsedWeightKg: Double?
        get() = OnboardingInput.weightKg(unitSystem, weightKg)

    val parsedGoalWeightKg: Double?
        get() = OnboardingInput.goalWeightKg(unitSystem, goalWeightInput)

    val goalWeightContradicts: Boolean
        get() = OnboardingInput.goalContradictsDirection(
            losing = goalType == GoalType.LOSE,
            currentKg = parsedWeightKg,
            goalKg = parsedGoalWeightKg,
        )

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.SEX -> sex != null
            OnboardingStep.BIRTH -> birthEpochDay != null && !underage
            OnboardingStep.BODY -> parsedHeightCm != null && parsedWeightKg != null
            OnboardingStep.ACTIVITY -> activityLevel != null
            OnboardingStep.GOAL -> goalType != null

            OnboardingStep.GOAL_WEIGHT ->
                OnboardingInput.goalWeightAcceptable(unitSystem, goalWeightInput)
            OnboardingStep.RATE -> goalRateKcal != null
            OnboardingStep.PLAN_PREVIEW -> preview != null && macrosSumOk
        }

    val macrosSumOk: Boolean
        get() {
            val target = preview?.kcal ?: return false
            val p = proteinG.toIntOrNull() ?: return false
            val c = carbsG.toIntOrNull() ?: return false
            val f = fatG.toIntOrNull() ?: return false
            val sum = p * 4 + c * 4 + f * 9
            return abs(sum - target) <= target * 0.02
        }
}

class OnboardingViewModel(
    private val repository: ProfileRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    fun setSex(sex: Sex) = _state.update { it.copy(sex = sex) }

    fun setBirth(epochDay: Long) = _state.update {
        val age = NutritionCalc.ageYears(epochDay, todayEpochDay())
        it.copy(birthEpochDay = epochDay, underage = age < 16)
    }

    fun setUnitSystem(unit: UnitSystem) = _state.update {
        if (it.unitSystem == unit) it
        else it.copy(unitSystem = unit, heightCm = "", heightFt = "", heightIn = "", weightKg = "", goalWeightInput = "")
    }

    fun setHeight(text: String) = _state.update { it.copy(heightCm = text.filter(Char::isDigit).take(3)) }

    fun setHeightFeet(text: String) = _state.update { it.copy(heightFt = text.filter(Char::isDigit).take(1)) }

    fun setHeightInches(text: String) = _state.update { it.copy(heightIn = text.filter(Char::isDigit).take(2)) }

    fun setWeight(text: String) = _state.update {
        it.copy(weightKg = text.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6))
    }

    fun setGoalWeight(text: String) = _state.update {
        it.copy(goalWeightInput = text.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6))
    }

    fun setActivity(level: ActivityLevel) = _state.update { it.copy(activityLevel = level) }

    fun setGoal(goal: GoalType) = _state.update {

        it.copy(
            goalType = goal,
            goalRateKcal = if (goal == GoalType.MAINTAIN) GoalRates.MAINTAIN else null,

            goalWeightInput = if (goal == GoalType.MAINTAIN) "" else it.goalWeightInput,
        )
    }

    fun setWeeklyRate(kgPerWeek: Double) = _state.update { s ->
        val magnitude = abs(kgPerWeek)
        val signed = if (s.goalType == GoalType.LOSE) -magnitude else magnitude
        s.copy(goalRateKcal = NutritionCalc.kcalPerDayFromWeeklyKg(signed))
    }

    fun setStrategy(strategy: MacroStrategy) = _state.update { it.copy(macroStrategy = strategy, macrosEdited = false) }

    fun editProtein(text: String) = editMacro { it.copy(proteinG = text.filter(Char::isDigit).take(3), macrosEdited = true) }
    fun editCarbs(text: String) = editMacro { it.copy(carbsG = text.filter(Char::isDigit).take(3), macrosEdited = true) }
    fun editFat(text: String) = editMacro { it.copy(fatG = text.filter(Char::isDigit).take(3), macrosEdited = true) }

    private fun editMacro(transform: (OnboardingState) -> OnboardingState) = _state.update(transform)

    fun resetMacrosToPreset() = _state.update { s ->
        val t = s.preview ?: return@update s
        s.copy(proteinG = "${t.proteinG}", carbsG = "${t.carbsG}", fatG = "${t.fatG}", macrosEdited = false)
    }

    fun next() {
        val s = _state.value
        if (!s.canContinue) return

        if (s.step == OnboardingStep.PLAN_PREVIEW) {
            save()
            return
        }
        val nextStep = OnboardingFlow.next(s.step, s.goalType) ?: return
        _state.update { it.copy(step = nextStep) }

        if (nextStep == OnboardingStep.PLAN_PREVIEW) recomputePreview(fillMacros = true)
    }

    fun back(): Boolean {
        val s = _state.value
        val previous = OnboardingFlow.previous(s.step, s.goalType) ?: return false
        _state.update { it.copy(step = previous) }
        return true
    }

    private fun recomputePreview(fillMacros: Boolean) {
        val s = _state.value
        val draft = draftProfile(s) ?: return
        val weight = s.parsedWeightKg ?: return
        val targets = NutritionCalc.dailyTargets(draft, weight, todayEpochDay())
        _state.update {
            if (fillMacros && !it.macrosEdited) {
                it.copy(
                    preview = targets,
                    proteinG = "${targets.proteinG}",
                    carbsG = "${targets.carbsG}",
                    fatG = "${targets.fatG}",
                )
            } else {
                it.copy(preview = targets)
            }
        }
    }

    private fun draftProfile(s: OnboardingState): UserProfileEntity? {
        return UserProfileEntity(
            sex = s.sex ?: return null,
            birthEpochDay = s.birthEpochDay ?: return null,
            heightCm = s.parsedHeightCm ?: return null,
            activityLevel = s.activityLevel ?: return null,
            goalType = s.goalType ?: return null,
            goalRateKcal = s.goalRateKcal ?: return null,

            unitSystem = s.unitSystem,
            goalWeightKg = s.parsedGoalWeightKg,
            macroStrategy = s.macroStrategy,
            customProteinG = null,
            customCarbsG = null,
            customFatG = null,
            updatedAt = 0L,
        )
    }

    private fun save() {
        val s = _state.value
        val base = draftProfile(s) ?: return
        val weight = s.parsedWeightKg ?: return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {

            val profile = if (s.macrosEdited) {
                base.copy(
                    macroStrategy = MacroStrategy.CUSTOM,
                    customProteinG = s.proteinG.toIntOrNull(),
                    customCarbsG = s.carbsG.toIntOrNull(),
                    customFatG = s.fatG.toIntOrNull(),
                )
            } else {
                base
            }

            repository.upsertWeight(todayEpochDay(), weight, note = null)
            repository.saveProfile(profile)
            preferences.setOnboardingDone(true)
            _state.update { it.copy(saving = false, done = true) }
        }
    }
}
