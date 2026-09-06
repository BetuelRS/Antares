package pt.antares.app.feature.running

import kotlinx.coroutines.flow.MutableStateFlow
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.GeoSample
import pt.antares.app.feature.running.domain.RunEngine
import pt.antares.app.feature.running.domain.RunResult

/**
 * O estado da corrida em curso, num objeto de processo. É deliberado e não um descuido: o
 * serviço em primeiro plano e o ecrã têm ciclos de vida independentes — o ecrã pode morrer
 * e voltar enquanto a corrida continua —, e um ViewModel não sobreviveria a isso.
 */
internal object RunTrackingState {
    val live = MutableStateFlow(RunLiveState())
    val last = MutableStateFlow<RunResult?>(null)

    private var engine: RunEngine? = null
    private val path = mutableListOf<Pair<Double, Double>>()

    fun begin(type: ActivityType, weightKg: Double, autoPause: Boolean) {
        engine = RunEngine(type, weightKg, autoPause)
        path.clear()
        last.value = null
        live.value = RunLiveState(active = true, type = type, autoPause = autoPause)
    }

    fun onSample(sample: GeoSample) {
        val e = engine ?: return
        val metrics = e.onSample(sample)

        // O mesmo limite de erro do [RunEngine], aplicado aqui outra vez porque o percurso
        // desenhado é guardado à parte das métricas: um ponto mau desenharia um risco a
        // atravessar o mapa mesmo sem contar para a distância.
        val usable = sample.accM <= 30.0

        // Em pausa o percurso **não cresce**, e o mapa fica com o traço onde a corrida
        // parou. O ponto continua a ser lido — é assim que o mapa continua a mostrar onde
        // a pessoa está —, mas o caminho até ao bebedouro não é caminho de corrida.
        if (usable && !metrics.pausaManual) path.add(sample.lat to sample.lon)
        live.value = live.value.copy(
            metrics = metrics,
            path = path.toList(),
            hasFix = live.value.hasFix || usable,
            parciais = e.parciaisAteAgora(),
        )
    }

    fun pausar() {
        val e = engine ?: return
        e.pausar()

        // O estado do ecrã tem de mudar já, e não à próxima amostra: entre duas leituras
        // do GPS passam segundos, e um botão que só muda daqui a três parece avariado.
        live.value = live.value.copy(metrics = live.value.metrics.copy(pausaManual = true))
    }

    fun retomar() {
        val e = engine ?: return
        e.retomar()
        live.value = live.value.copy(metrics = live.value.metrics.copy(pausaManual = false))
    }

    fun finish() {
        val e = engine ?: return
        last.value = e.finish()
        live.value = live.value.copy(active = false)
        engine = null
    }

    fun discard() {
        engine = null
        path.clear()
        last.value = null
        live.value = RunLiveState()
    }

    fun isActive(): Boolean = engine != null
}
