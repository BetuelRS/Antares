package pt.antares.app.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.admin.AdminRepository
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult

enum class AdminMessage { NONE, ENABLED, DISABLED, BAD_CODE, NETWORK, ERROR }

data class AdminState(
    val code: String = "",
    val loading: Boolean = false,
    val message: AdminMessage = AdminMessage.NONE,
)

class AdminViewModel(
    private val repository: AdminRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    val unlimited: StateFlow<Boolean> = repository.unlimited
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setCode(value: String) {
        _state.value = _state.value.copy(code = value, message = AdminMessage.NONE)
    }

    fun submit(enable: Boolean) {
        val code = _state.value.code.trim()
        if (code.isEmpty() || _state.value.loading) return
        _state.value = _state.value.copy(loading = true, message = AdminMessage.NONE)
        viewModelScope.launch {
            val message = when (val r = repository.setUnlimited(code, enable)) {
                is AppResult.Success ->
                    if (r.value) AdminMessage.ENABLED else AdminMessage.DISABLED
                is AppResult.Failure -> when (r.error) {
                    AppError.Unauthorized -> AdminMessage.BAD_CODE
                    AppError.Network -> AdminMessage.NETWORK
                    else -> AdminMessage.ERROR
                }
            }

            val clearCode = message == AdminMessage.ENABLED || message == AdminMessage.DISABLED
            _state.value = _state.value.copy(
                loading = false,
                message = message,
                code = if (clearCode) "" else _state.value.code,
            )
        }
    }
}
