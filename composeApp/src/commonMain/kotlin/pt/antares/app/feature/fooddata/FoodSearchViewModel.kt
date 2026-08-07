package pt.antares.app.feature.fooddata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.feature.recipe.RecipeRepository
import pt.antares.app.feature.recipe.RecipeSummary
import pt.antares.app.feature.templates.MealTemplateRepository

enum class SearchTab { SEARCH, RECENTS, FAVORITES, MINE, RECIPES, TEMPLATES }

data class FoodSearchState(
    val query: String = "",
    val tab: SearchTab = SearchTab.SEARCH,
    val results: List<FoodEntity> = emptyList(),
    val searching: Boolean = false,

    val onlineResults: List<FoodEntity> = emptyList(),
    val searchingOnline: Boolean = false,

    val selected: Set<String> = emptySet(),
)

@OptIn(FlowPreview::class)
class FoodSearchViewModel(
    private val repository: FoodRepository,
    private val offRepository: OffRepository,
    recipeRepository: RecipeRepository,
    private val templateRepository: MealTemplateRepository,
    private val diaryRepository: pt.antares.app.feature.diary.DiaryRepository,
) : ViewModel() {

    val recipes: StateFlow<List<RecipeSummary>> = recipeRepository.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<MealTemplateEntity>> = templateRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _templateApplied = MutableStateFlow(false)
    val templateApplied: StateFlow<Boolean> = _templateApplied

    private val _state = MutableStateFlow(FoodSearchState())
    val state: StateFlow<FoodSearchState> = _state

    private val _openFood = MutableStateFlow<String?>(null)
    val openFood: StateFlow<String?> = _openFood

    val recents: StateFlow<List<FoodEntity>> = repository.observeRecents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites: StateFlow<List<FoodEntity>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val myFoods: StateFlow<List<FoodEntity>> = repository.observeMyFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostLogged: StateFlow<List<FoodEntity>> = repository.observeMostLogged(limit = 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suggestions: StateFlow<List<FoodEntity>> =
        combine(_state, mostLogged, recents) { s, top, rec ->
            val procura = TextNormalize.normalize(s.query.trim())
            if (procura.isEmpty()) {
                emptyList()
            } else {
                (top + rec)
                    .distinctBy { it.id }
                    .filter { TextNormalize.normalize(it.namePt.ifBlank { it.nameEn }).contains(procura) }
                    .take(MAX_SUGGESTIONS)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val queryFlow = MutableStateFlow("")

    init {

        queryFlow
            .debounce(300)
            .onEach { q ->
                if (q.length < 2) {
                    _state.update { it.copy(results = emptyList(), searching = false) }
                } else {
                    _state.update { it.copy(searching = true) }
                    val results = repository.search(q)
                    _state.update { it.copy(results = results, searching = false) }
                }
            }
            .launchIn(viewModelScope)

        queryFlow
            .debounce(400)
            .onEach { q ->
                if (q.length < 3) {
                    _state.update { it.copy(onlineResults = emptyList(), searchingOnline = false) }
                } else {
                    _state.update { it.copy(searchingOnline = true) }

                    val online = offRepository.searchOnline(q)

                    val localIds = _state.value.results.map { it.id }.toSet()
                    _state.update {
                        it.copy(
                            onlineResults = online.orEmpty().filter { f -> f.id !in localIds },
                            searchingOnline = false,
                        )
                    }
                    recordMissIfWorthIt(q, online?.size)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun recordMissIfWorthIt(query: String, onlineHits: Int?) {
        if (!SearchMissRule.shouldRecord(query, _state.value.results.size, onlineHits)) return
        val canonica = SearchMissRule.normalize(query) ?: return
        viewModelScope.launch { repository.recordSearchMiss(canonica) }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun setTab(tab: SearchTab) = _state.update { it.copy(tab = tab) }

    fun selectOnline(food: FoodEntity) {
        viewModelScope.launch {
            repository.cacheOnline(food)
            _openFood.value = food.id
        }
    }

    fun consumeOpenFood() {
        _openFood.value = null
    }

    fun applyTemplate(templateId: String, slot: MealSlot, epochDay: Long) {
        viewModelScope.launch {
            templateRepository.applyTemplate(templateId, slot, epochDay)
            _templateApplied.value = true
        }
    }

    fun consumeTemplateApplied() {
        _templateApplied.value = false
    }

    fun toggleSelect(foodId: String) = _state.update { s ->
        s.copy(
            selected = if (foodId in s.selected) s.selected - foodId else s.selected + foodId,
        )
    }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    fun logSelected(slot: MealSlot, epochDay: Long) {
        val ids = _state.value.selected
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                val food = repository.byId(id) ?: return@forEach
                val gramas = diaryRepository.defaultPortionFor(food)
                diaryRepository.logFood(food, gramas, slot, epochDay)
                repository.touchLastUsed(food.id, amountG = gramas)
            }
            _state.update { it.copy(selected = emptySet()) }

            _templateApplied.value = true
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch { templateRepository.deleteTemplate(templateId) }
    }

    private companion object {

        const val MAX_SUGGESTIONS = 6
    }
}
