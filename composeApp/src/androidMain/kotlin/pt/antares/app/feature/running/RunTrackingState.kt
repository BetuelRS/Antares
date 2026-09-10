package pt.antares.app.feature.running

import kotlinx.coroutines.flow.MutableStateFlow
import pt.antares.app.core.model.UnitSystem
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

    fun begin(type: ActivityType, weightKg: Double, autoPause: Boolean, unidades: UnitSystem) {
        engine = RunEngine(type, weightKg, autoPause)
        last.value = null
        live.value = RunLiveState(active = true, type = type, autoPause = autoPause, unidades = unidades)
    }

    fun onSample(sample: GeoSample) {
        val e = engine ?: return
        val metrics = e.onSample(sample)

        // Só para o «a apanhar sinal» do ecrã. O percurso já não se decide aqui: vem do motor,
        // feito só das posições que ele aceitou — em pausa não cresce, e um salto descartado
        // não entra. Aqui guardava-se tudo o que tivesse precisão aceitável, saltos incluídos.
        val usable = sample.accM <= 30.0

        live.value = live.value.copy(
            metrics = metrics,
            path = e.percurso(),
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

    /**
     * Fecha uma volta. O estado só muda quando o motor a aceitou — em pausa, ou sem um metro
     * andado desde a anterior, não há volta e o ecrã não pode fingir que houve.
     */
    fun volta() {
        val e = engine ?: return
        if (e.volta() == null) return
        live.value = live.value.copy(parciais = e.parciaisAteAgora())
    }

    fun finish() {
        val e = engine ?: return
        last.value = e.finish()
        live.value = live.value.copy(active = false)
        engine = null
    }

    fun discard() {
        engine = null
        last.value = null
        live.value = RunLiveState()
    }

    fun isActive(): Boolean = engine != null
}
