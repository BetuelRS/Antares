package pt.antares.app.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.antares.app.core.ai.AiFoodItem
import pt.antares.app.core.ai.AiRepository
import pt.antares.app.core.ai.AiUsage
import pt.antares.app.core.ai.AiWarnings
import pt.antares.app.core.ai.withGrams
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult

enum class AiPhase { INPUT, ANALYZING, REVIEW, ERROR }

data class AiState(
    val phase: AiPhase = AiPhase.INPUT,
    val text: String = "",

    val items: List<AiFoodItem> = emptyList(),
    val warnings: List<String> = emptyList(),
    val usage: AiUsage? = null,
    val error: AppError? = null,

    val inputError: Boolean = false,
    val saved: Boolean = false,
) {
    val totalKcal: Int get() = items.sumOf { it.kcal }

    val canConfirm: Boolean get() = items.isNotEmpty()

    val notFood: Boolean get() = warnings.contains(AiWarnings.NOT_FOOD)

    val vague: Boolean get() = warnings.contains(AiWarnings.VAGUE_ITEM)
}

class AiViewModel(
    private val repository: AiRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AiState())
    val state: StateFlow<AiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onTextChange(value: String) {

        _state.value = _state.value.copy(text = value.take(MAX_TEXT_CHARS))
    }

    fun analyzeText() {
        val text = _state.value.text.trim()

        if (text.length < MIN_TEXT_CHARS) {
            _state.value = _state.value.copy(phase = AiPhase.ERROR, inputError = true, error = null)
            return
        }
        run(LogOrigin.AI_TEXT) { repository.analyzeText(text) }
    }

    fun analyzePhoto(base64: String, mime: String) {
        run(LogOrigin.AI_PHOTO) { repository.analyzePhoto(base64, mime) }
    }

    private var origin: LogOrigin = LogOrigin.AI_TEXT

    private fun run(from: LogOrigin, block: suspend () -> AppResult<pt.antares.app.core.ai.FoodAnalysis>) {
        origin = from
        job?.cancel()
        _state.value = _state.value.copy(phase = AiPhase.ANALYZING, error = null, inputError = false)
        job = viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Success -> _state.value = _state.value.copy(
                    phase = AiPhase.REVIEW,
                    items = result.value.items,
                    warnings = result.value.warnings,
                    usage = result.value.usage,
                )

                is AppResult.Failure -> _state.value = _state.value.copy(
                    phase = AiPhase.ERROR,
                    error = result.error,
                )
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _state.value = _state.value.copy(phase = AiPhase.INPUT)
    }

    fun setGrams(index: Int, grams: Double) {
        val items = _state.value.items.toMutableList()
        val item = items.getOrNull(index) ?: return
        items[index] = item.withGrams(grams)
        _state.value = _state.value.copy(items = items)
    }

    fun removeItem(index: Int) {
        val items = _state.value.items.toMutableList()
        if (index !in items.indices) return
        items.removeAt(index)
        _state.value = _state.value.copy(items = items)
    }

    fun confirm(mealSlot: MealSlot, epochDay: Long) {
        val items = _state.value.items
        if (items.isEmpty()) return
        viewModelScope.launch {
            repository.confirmFood(items, mealSlot, epochDay, origin)
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun reset() {
        job?.cancel()
        _state.value = AiState(usage = _state.value.usage)
    }

    companion object {

        const val MAX_TEXT_CHARS = 2_000

        const val MIN_TEXT_CHARS = 3
    }
}
