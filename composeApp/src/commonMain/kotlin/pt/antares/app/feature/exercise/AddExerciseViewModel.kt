package pt.antares.app.feature.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.ai.AiRepository
import pt.antares.app.core.ai.ExerciseAnalysis
import pt.antares.app.core.calc.MetCalc
import pt.antares.app.core.exercise.MetActivity
import pt.antares.app.core.exercise.MetCatalog
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.feature.ai.AiViewModel
import pt.antares.app.feature.profile.data.ProfileRepository

data class AddExerciseState(
    val loading: Boolean = true,
    val query: String = "",
    val category: String? = null,
    val categories: List<String> = emptyList(),
    val results: List<MetActivity> = emptyList(),

    // As últimas atividades registadas, a mais recente primeiro.
    val recentes: List<MetActivity> = emptyList(),

    val selected: MetActivity? = null,
    val durationMin: Int = 30,
    val weightKg: Double = 70.0,
    val saved: Boolean = false,

    val ai: AiExerciseState = AiExerciseState(),
) {
    val previewKcal: Int get() = selected?.let { MetCalc.kcal(it.met, weightKg, durationMin) } ?: 0
    val canSave: Boolean get() = selected != null && durationMin > 0

    /**
     * Os recentes só se mostram antes de se procurar.
     *
     * Com uma palavra escrita ou uma categoria escolhida, quem está no ecrã já disse o que
     * quer — e uma secção de atalhos por cima de uma lista filtrada é a resposta a outra
     * pergunta.
     */
    val mostrarRecentes: Boolean get() = recentes.isNotEmpty() && query.isBlank() && category == null
}

data class AiExerciseState(
    val open: Boolean = false,
    val text: String = "",
    val busy: Boolean = false,
    val draft: ExerciseAnalysis? = null,
    val error: AppError? = null,
) {

    val canConfirm: Boolean get() = draft?.kcal != null
}

class AddExerciseViewModel(
    private val repository: ExerciseRepository,
    profileRepository: ProfileRepository,
    private val ai: AiRepository,
) : ViewModel() {

    private var catalog: MetCatalog? = null
    private val _state = MutableStateFlow(AddExerciseState())
    val state: StateFlow<AddExerciseState> = _state

    init {
        viewModelScope.launch {
            val loaded = repository.loadCatalog()
            catalog = loaded

            // Um id que já não existe na tabela cai aqui em silêncio, e é o que se quer: a
            // tabela vem com a app e pode encolher entre versões, mas um registo antigo que
            // a nomeie continua no diário.
            val recentes = repository.recentMetIds().mapNotNull(loaded::byId)

            _state.update {
                it.copy(
                    loading = false,
                    categories = loaded.categories(),
                    results = loaded.activities,
                    recentes = recentes,
                )
            }
        }

        profileRepository.observeLatestWeight()
            .onEach { w -> w?.let { weight -> _state.update { it.copy(weightKg = weight.weightKg) } } }
            .launchIn(viewModelScope)
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        refilter()
    }

    fun setCategory(category: String?) {
        _state.update { it.copy(category = category) }
        refilter()
    }

    fun select(activity: MetActivity) = _state.update { it.copy(selected = activity) }

    fun setDuration(min: Int) =
        _state.update { it.copy(durationMin = min.coerceIn(1, ExerciseRepository.MAX_DURATION_MIN)) }

    fun changeDuration(delta: Int) = setDuration(_state.value.durationMin + delta)

    fun save(epochDay: Long) {
        val s = _state.value
        val activity = s.selected ?: return
        if (!s.canSave) return
        viewModelScope.launch {
            repository.logManual(
                epochDay = epochDay,
                label = activity.namePt,
                metId = activity.id,
                met = activity.met,
                durationMin = s.durationMin,
                kcal = s.previewKcal,
            )
            _state.update { it.copy(saved = true) }
        }
    }

    fun openAi() = _state.update { it.copy(ai = AiExerciseState(open = true)) }

    fun closeAi() = _state.update { it.copy(ai = AiExerciseState()) }

    fun setAiText(text: String) =
        _state.update { it.copy(ai = it.ai.copy(text = text.take(AiViewModel.MAX_TEXT_CHARS))) }

    fun analyzeAi() {
        val text = _state.value.ai.text.trim()
        if (text.isEmpty()) return
        _state.update { it.copy(ai = it.ai.copy(busy = true, error = null)) }
        viewModelScope.launch {
            when (val result = ai.analyzeExercise(text)) {
                is AppResult.Success -> _state.update {
                    it.copy(ai = it.ai.copy(busy = false, draft = result.value))
                }

                is AppResult.Failure -> _state.update {
                    it.copy(ai = it.ai.copy(busy = false, error = result.error))
                }
            }
        }
    }

    fun confirmAi(epochDay: Long) {
        val draft = _state.value.ai.draft ?: return
        val kcal = draft.kcal ?: return
        viewModelScope.launch {
            repository.logManual(
                epochDay = epochDay,
                label = draft.activity,
                metId = null,
                met = draft.met,
                durationMin = draft.durationMin?.toInt() ?: 0,
                kcal = kcal,
            )
            _state.update { it.copy(saved = true) }
        }
    }

    private fun refilter() {
        val all = catalog?.activities ?: return
        val q = TextNormalize.normalize(_state.value.query.trim())
        val cat = _state.value.category
        val filtered = all.filter { a ->
            (cat == null || a.category == cat) &&
                (q.isEmpty() || TextNormalize.normalize("${a.namePt} ${a.nameEn}").contains(q))
        }
        _state.update { it.copy(results = filtered) }
    }
}
