package pt.antares.app.feature.workout.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class SessionPickBus {
    private val _picked = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val picked: SharedFlow<String> = _picked

    suspend fun emit(exerciseId: String) = _picked.emit(exerciseId)
}
