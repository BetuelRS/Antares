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

/**
 * O arranque inteiro num objeto. Os campos de texto guardam o que a pessoa escreveu, com
 * erros e tudo, e as propriedades derivadas é que os interpretam: assim o campo não se
 * limpa nem se reformata debaixo dos dedos a meio de escrever.
 */
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

    // Um `when` exaustivo sobre os passos: acrescentar um passo novo sem dizer o que o
    // valida deixa de compilar, em vez de o deixar passar sempre.
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

    /**
     * Os macros editados à mão têm de somar as calorias da meta, com 2% de tolerância —
     * é o que o arredondamento a gramas inteiras deixa de folga. Sem isto, a pessoa saía
     * do arranque com uma meta que se contradiz a si própria.
     */
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

    // Abaixo de 16 anos a app não avança: uma meta de calorias para quem ainda cresce é
    // matéria clínica, e a app não a propõe.
    fun setBirth(epochDay: Long) = _state.update {
        val age = NutritionCalc.ageYears(epochDay, todayEpochDay())
        it.copy(birthEpochDay = epochDay, underage = age < 16)
    }

    // Mudar de unidades limpa os campos em vez de os converter: 70 escrito em quilos e
    // reinterpretado em libras seria um valor plausível e errado.
    fun setUnitSystem(unit: UnitSystem) = _state.update {
        if (it.unitSystem == unit) it
        else it.copy(unitSystem = unit, heightCm = "", heightFt = "", heightIn = "", weightKg = "", goalWeightInput = "")
    }

    // Filtrar na entrada em vez de validar na saída: o campo nunca chega a aceitar letras,
    // e o limite de dígitos impede alturas de quatro algarismos.
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

    /**
     * O seletor dá sempre um ritmo positivo; o sinal vem do objetivo. Converte-se logo aqui
     * para kcal por dia, que é como o perfil o guarda.
     */
    fun setWeeklyRate(kgPerWeek: Double) = _state.update { s ->
        val magnitude = abs(kgPerWeek)
        val signed = if (s.goalType == GoalType.LOSE) -magnitude else magnitude
        s.copy(goalRateKcal = NutritionCalc.kcalPerDayFromWeeklyKg(signed))
    }

    // Escolher uma estratégia esquece as edições à mão: são os números dela que passam a
    // valer, e mantê-los daria uma estratégia com os macros de outra.
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
        // O objetivo decide o percurso: quem escolhe manter salta os passos do peso-alvo e
        // do ritmo, que não lhe fazem pergunta nenhuma.
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

    /**
     * Um perfil provisório a partir do que já foi respondido, ou null se faltar alguma
     * coisa. Os `?: return null` encadeados fazem os campos obrigatórios estarem todos num
     * sítio só: acrescentar um ao perfil obriga a decidir aqui se é preciso ou não.
     */
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

            // Mexer nos macros muda a estratégia para manual: a partir daí são os números
            // da pessoa que valem, e a app deixa de os recalcular quando o peso mudar.
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

            // O peso primeiro: as metas do perfil dependem da última pesagem, e sem ela o
            // primeiro ecrã abriria com um peso de recurso em vez do que se acabou de dar.
            repository.upsertWeight(todayEpochDay(), weight, note = null)
            repository.saveProfile(profile)
            // A marca fica em último: um arranque interrompido a meio recomeça do princípio
            // em vez de deixar a app com meio perfil e o arranque dado por feito.
            preferences.setOnboardingDone(true)
            _state.update { it.copy(saving = false, done = true) }
        }
    }
}
