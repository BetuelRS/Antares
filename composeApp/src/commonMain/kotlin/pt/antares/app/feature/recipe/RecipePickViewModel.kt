package pt.antares.app.feature.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RecipePickViewModel(
    private val repository: RecipeRepository,
) : ViewModel() {

    fun add(recipeId: String, foodId: String, onAdded: () -> Unit) {
        viewModelScope.launch {
            repository.addIngredient(recipeId, foodId, DEFAULT_GRAMS)
            onAdded()
        }
    }

    private companion object {
        const val DEFAULT_GRAMS = 100.0
    }
}
