package pt.antares.app.feature.fasting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.database.entities.FastingProtocolEntity
import pt.antares.app.core.database.entities.FastingSessionEntity
import pt.antares.app.feature.fasting.data.FastingRepository

data class FastingUiState(
    val protocols: List<FastingProtocolEntity> = emptyList(),
    val active: FastingSessionEntity? = null,
    val selectedProtocolId: String? = null,
) {
    val selectedProtocol: FastingProtocolEntity?
        get() = protocols.firstOrNull { it.id == selectedProtocolId } ?: protocols.firstOrNull()
}

class FastingViewModel(
    private val repository: FastingRepository,
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    private val _justEnded = MutableStateFlow(false)
    val justEnded: StateFlow<Boolean> = _justEnded

    val state: StateFlow<FastingUiState> = combine(
        repository.observeProtocols(),
        repository.observeActive(),
        selectedId,
    ) { protocols, active, selected ->
        FastingUiState(protocols = protocols, active = active, selectedProtocolId = selected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FastingUiState())

    fun selectProtocol(id: String) { selectedId.value = id }

    fun selectedIdOrDefault(): String? = state.value.selectedProtocol?.id

    fun start(protocolId: String) {
        _justEnded.value = false
        viewModelScope.launch { repository.startOrResume(protocolId) }
    }

    fun finish() {
        viewModelScope.launch { if (repository.finish()) _justEnded.value = true }
    }

    fun breakFast() {
        viewModelScope.launch { if (repository.breakFast()) _justEnded.value = true }
    }

    fun dismissJustEnded() { _justEnded.value = false }

    fun shiftStart(deltaMs: Long) {
        val current = state.value.active ?: return
        viewModelScope.launch { repository.adjustStart(current.startedAt + deltaMs) }
    }
}
