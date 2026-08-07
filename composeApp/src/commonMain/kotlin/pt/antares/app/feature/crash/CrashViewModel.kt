package pt.antares.app.feature.crash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.antares.app.core.crash.CrashReport
import pt.antares.app.core.crash.CrashStore

data class CrashUi(
    val aLer: Boolean = true,
    val relatorio: String? = null,
    val culpado: String? = null,
) {
    val temCrash: Boolean get() = relatorio != null
}

class CrashViewModel(
    private val store: CrashStore,
    private val io: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(CrashUi())
    val state: StateFlow<CrashUi> = _state.asStateFlow()

    init {
        recarregar()
    }

    private fun recarregar() {
        viewModelScope.launch {

            val texto = withContext(io) { store.read() }
            _state.value = CrashUi(
                aLer = false,
                relatorio = texto,

                culpado = texto?.let { t ->
                    CrashReport.culpado(t.lines().map { it.trim().removePrefix("at ") })
                },
            )
        }
    }

    fun limpar() {
        viewModelScope.launch {
            withContext(io) { store.clear() }
            _state.value = CrashUi(aLer = false, relatorio = null, culpado = null)
        }
    }
}
