package pt.antares.app.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pt.antares.app.core.datastore.AppPreferences

class MeViewModel(preferences: AppPreferences) : ViewModel() {

    val gamificationEnabled: StateFlow<Boolean> = preferences.gamificationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
