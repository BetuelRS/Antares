package pt.antares.app.feature.running

import kotlinx.coroutines.flow.StateFlow
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunMetrics
import pt.antares.app.feature.running.domain.Split
import pt.antares.app.feature.running.domain.RunResult

data class RunLiveState(
    val active: Boolean = false,
    val type: ActivityType = ActivityType.RUN,
    val autoPause: Boolean = true,
    val metrics: RunMetrics = RunMetrics(),

    /**
     * As unidades da pessoa, lidas **uma vez ao começar**.
     *
     * Estão aqui e não no ecrã porque quem as precisa fora dele é a notificação, e essa
     * vive num serviço sem composição por baixo. Sem elas, a notificação escrevia
     * quilómetros a quem escolheu milhas — que é o defeito que o `RunFormat` proíbe por
     * escrito e ao qual ela chegava por fora.
     */
    val unidades: UnitSystem = UnitSystem.METRIC,

    val path: List<Pair<Double, Double>> = emptyList(),

    val hasFix: Boolean = false,

    /**
     * Os quilómetros já fechados, **a meio da corrida**. O ecrã mostra os dois últimos: sem
     * eles, só se sabe se se acelerou ou abrandou depois de acabar.
     */
    val parciais: List<Split> = emptyList(),
)

interface RunController {
    val state: StateFlow<RunLiveState>

    val lastResult: StateFlow<RunResult?>

    fun start(type: ActivityType, autoPause: Boolean)

    /** A pausa que a pessoa pede. O GPS continua a ler; nada disso conta. */
    fun pausar()

    fun retomar()

    /** Fecha uma volta aqui. Não faz nada em pausa nem sem distância desde a anterior. */
    fun volta()

    fun stop()
    fun discard()
}

class NoopRunController : RunController {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(RunLiveState())
    override val state: StateFlow<RunLiveState> = _state
    private val _last = kotlinx.coroutines.flow.MutableStateFlow<RunResult?>(null)
    override val lastResult: StateFlow<RunResult?> = _last
    override fun start(type: ActivityType, autoPause: Boolean) {}
    override fun pausar() {}
    override fun retomar() {}
    override fun volta() {}
    override fun stop() {}
    override fun discard() {}
}
