package pt.antares.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.core.designsystem.ThemeMode

class SettingsViewModel(
    private val preferences: AppPreferences,
) : ViewModel() {

    val mealNames: StateFlow<Map<pt.antares.app.core.model.MealSlot, String>> = preferences.mealNames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setMealName(slot: pt.antares.app.core.model.MealSlot, name: String) = viewModelScope.launch {
        preferences.setMealName(slot, name)
    }

    val adaptiveTargets: StateFlow<Boolean> = preferences.adaptiveTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .map { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        preferences.setThemeMode(mode.name)
    }

    fun setAdaptiveTargets(enabled: Boolean) = viewModelScope.launch {
        preferences.setAdaptiveTargets(enabled)
    }
}
