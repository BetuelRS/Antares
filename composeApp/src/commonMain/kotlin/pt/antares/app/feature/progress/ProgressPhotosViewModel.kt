package pt.antares.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.database.entities.ProgressPhotoEntity

data class ProgressPhotosState(

    val photos: List<ProgressPhotoEntity> = emptyList(),
    val saving: Boolean = false,
) {

    val canCompare: Boolean
        get() = photos.size >= 2 && photos.first().epochDay != photos.last().epochDay
}

class ProgressPhotosViewModel(
    private val repository: ProgressPhotoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressPhotosState())
    val state: StateFlow<ProgressPhotosState> = _state

    init {
        repository.observeAll()
            .onEach { fotos -> _state.update { it.copy(photos = fotos) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            repository.orphans().forEach { repository.remove(it) }
        }
    }

    fun add(base64Jpeg: String) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            repository.add(base64Jpeg)
            _state.update { it.copy(saving = false) }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch { repository.remove(id) }
    }
}
