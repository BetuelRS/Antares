package pt.antares.app.feature.running.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.antares.app.core.datastore.AppPreferences
import pt.antares.app.feature.running.RunController
import pt.antares.app.feature.running.RunLiveState
import pt.antares.app.feature.running.domain.ActivityType

enum class RunGoalType { NONE, DISTANCE, TIME }

class RunViewModel(
    private val controller: RunController,
    private val preferences: AppPreferences,
) : ViewModel() {

    val state: StateFlow<RunLiveState> = controller.state

    val oemWarningShown: StateFlow<Boolean> = preferences.runOemWarningShown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun dismissOemWarning() {
        viewModelScope.launch { preferences.setRunOemWarningShown() }
    }

    // Semeados das preferências e não só em memória. Ficam em `MutableStateFlow` para o
    // `start()` os poder ler sem suspender: um `stateIn` devolveria o valor por omissão à
    // primeira leitura, e quem carregasse em começar mal a app abrisse arrancava uma
    // corrida com o tipo errado.
    private val _autoPause = MutableStateFlow(true)
    val autoPause: StateFlow<Boolean> = _autoPause

    private val _type = MutableStateFlow(ActivityType.RUN)
    val type: StateFlow<ActivityType> = _type

    init {
        viewModelScope.launch {
            _autoPause.value = preferences.runAutoPauseOnce()
            _type.value = runCatching { ActivityType.valueOf(preferences.runTypeOnce()) }
                .getOrDefault(ActivityType.RUN)
        }
    }

    val goalType: StateFlow<RunGoalType> = preferences.runGoalType
        .map { runCatching { RunGoalType.valueOf(it) }.getOrDefault(RunGoalType.NONE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunGoalType.NONE)

    val goalValue: StateFlow<Int> = preferences.runGoalValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setGoal(type: RunGoalType, value: Int) {
        viewModelScope.launch { preferences.setRunGoal(type.name, value) }
    }

    fun setAutoPause(enabled: Boolean) {
        _autoPause.value = enabled
        viewModelScope.launch { preferences.setRunAutoPause(enabled) }
    }

    fun setType(type: ActivityType) {
        _type.value = type
        viewModelScope.launch { preferences.setRunType(type.name) }
    }

    fun start() = controller.start(_type.value, _autoPause.value)
    fun finish() = controller.stop()
    fun discard() = controller.discard()
}
