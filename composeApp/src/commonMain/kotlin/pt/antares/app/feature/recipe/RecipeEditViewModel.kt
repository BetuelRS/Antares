package pt.antares.app.feature.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.RecipeNutrition
import pt.antares.app.core.database.entities.RecipeIngredientEntity

data class RecipeEditState(
    val recipeId: String? = null,
    val name: String = "",
    val yieldText: String = "",
    val rows: List<IngredientRow> = emptyList(),
    val nutrition: RecipeNutrition = RecipeNutrition(0, 0.0, 0.0, 0.0, 0.0),
    val saved: Boolean = false,
) {
    val yieldGrams: Double? get() = yieldText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
    val valid: Boolean get() = name.trim().length >= 2 && rows.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditViewModel(
    private val repository: RecipeRepository,
) : ViewModel() {

    private val recipeId = MutableStateFlow<String?>(null)
    private val name = MutableStateFlow("")
    private val yieldText = MutableStateFlow("")
    private val saved = MutableStateFlow(false)

    private val rows = recipeId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeIngredientRows(id)
    }

    val state: StateFlow<RecipeEditState> = combine(recipeId, name, yieldText, rows, saved) { id, nm, yt, rws, sv ->
        val yieldG = yt.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
        RecipeEditState(
            recipeId = id,
            name = nm,
            yieldText = yt,
            rows = rws,
            nutrition = repository.nutritionFrom(rws, yieldG),
            saved = sv,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeEditState())

    fun start(existingId: String?) {
        if (existingId != null && recipeId.value == null) {
            recipeId.value = existingId
            viewModelScope.launch {
                repository.recipeById(existingId)?.let { r ->
                    name.value = r.name
                    yieldText.value = r.yieldGrams?.let { formatGrams(it) } ?: ""
                }
            }
        }
    }

    fun setName(value: String) { name.value = value.take(80) }
    fun setYield(value: String) { yieldText.value = value.filter { it.isDigit() || it == '.' || it == ',' }.take(6) }

    fun ensureRecipeThen(navigate: (String) -> Unit) {
        viewModelScope.launch {
            val id = recipeId.value ?: repository.createRecipe(
                name.value.ifBlank { " " },
                yieldText.value.replace(',', '.').toDoubleOrNull(),
            ).also { recipeId.value = it }
            navigate(id)
        }
    }

    fun updateGrams(ingredient: RecipeIngredientEntity, gramsText: String) {
        val grams = gramsText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: return
        viewModelScope.launch { repository.updateIngredientGrams(ingredient, grams) }
    }

    fun removeIngredient(ingredient: RecipeIngredientEntity) {
        viewModelScope.launch { repository.removeIngredient(ingredient) }
    }

    fun delete() {
        val id = recipeId.value ?: return
        viewModelScope.launch {
            repository.deleteRecipe(id)
            saved.value = true
        }
    }

    fun save() {
        viewModelScope.launch {
            val yieldG = yieldText.value.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
            val id = recipeId.value ?: repository.createRecipe(name.value, yieldG).also { recipeId.value = it }
            repository.updateRecipe(id, name.value, yieldG)
            saved.value = true
        }
    }

    private fun formatGrams(v: Double): String {
        val i = v.toInt()
        return if (v == i.toDouble()) i.toString() else v.toString()
    }
}
