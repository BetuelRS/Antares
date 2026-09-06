package pt.antares.app.feature.running

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.antares.app.core.database.daos.UserProfileDao
import pt.antares.app.core.database.daos.WeightLogDao
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunResult

class AndroidRunController(
    private val context: Context,
    private val weightLogDao: WeightLogDao,
    private val userProfileDao: UserProfileDao,
) : RunController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val state: StateFlow<RunLiveState> = RunTrackingState.live
    override val lastResult: StateFlow<RunResult?> = RunTrackingState.last

    override fun start(type: ActivityType, autoPause: Boolean) {
        scope.launch {
            val weight = weightLogDao.latest()?.weightKg ?: 70.0
            // As unidades leem-se **uma vez, aqui**, e viajam no estado até à notificação.
            // O serviço não tem composição por baixo e não tinha por onde as perguntar —
            // era essa a razão de escrever quilómetros a quem escolheu milhas.
            val unidades = userProfileDao.get()?.unitSystem ?: UnitSystem.METRIC
            RunTrackingState.begin(type, weight, autoPause, unidades)
            val intent = Intent(context, RunTrackerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun pausar() = RunTrackingState.pausar()

    override fun retomar() = RunTrackingState.retomar()

    override fun volta() = RunTrackingState.volta()

    override fun stop() {
        RunTrackingState.finish()
        stopService()
    }

    override fun discard() {
        RunTrackingState.discard()
        stopService()
    }

    private fun stopService() {
        context.stopService(Intent(context, RunTrackerService::class.java))
    }
}
