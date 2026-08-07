package pt.antares.app.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.antares.app.core.health.HealthAvailability
import pt.antares.app.core.health.HealthImport
import pt.antares.app.core.health.HealthPublisher
import pt.antares.app.core.health.HealthRepository
import pt.antares.app.core.util.todayEpochDay

data class HealthState(
    val availability: HealthAvailability = HealthAvailability.NOT_SUPPORTED,
    val granted: Boolean = false,
    val importing: Boolean = false,

    val lastImport: HealthImport? = null,
    val loading: Boolean = true,
)

class HealthViewModel(
    private val repository: HealthRepository,
    private val publisher: HealthPublisher,
) : ViewModel() {

    private val _state = MutableStateFlow(HealthState())
    val state: StateFlow<HealthState> = _state.asStateFlow()

    val permissions: Set<String> get() = repository.permissions + publisher.writePermissions

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val availability = repository.availability()
            val granted = repository.hasPermissions()
            _state.value = _state.value.copy(
                availability = availability,
                granted = granted,
                loading = false,
            )

            if (granted && _state.value.lastImport == null) importNow()
        }
    }

    fun importNow() {
        if (_state.value.importing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(importing = true)
            val result = repository.importNow()

            publisher.publishNow(todayEpochDay())
            _state.value = _state.value.copy(importing = false, lastImport = result)
        }
    }
}
