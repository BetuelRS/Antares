package pt.antares.app.feature.running.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import pt.antares.app.core.calc.HistoryFilter
import pt.antares.app.core.calc.Mes
import pt.antares.app.core.database.daos.CorridaNaListaRow
import pt.antares.app.core.database.entities.RunEntity
import pt.antares.app.core.health.HealthAvailability
import pt.antares.app.core.health.HealthGateway
import pt.antares.app.core.util.inicioDoDiaMs
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.core.util.weekStartEpochDay
import pt.antares.app.feature.running.RunController
import pt.antares.app.feature.running.RunLiveState
import pt.antares.app.feature.running.data.RunRepository
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RitmoMedio
import pt.antares.app.feature.running.domain.RunPrCalc
import pt.antares.app.feature.running.domain.Split

class RunSummaryViewModel(
    private val controller: RunController,
    private val repository: RunRepository,
) : ViewModel() {

    val state: StateFlow<RunLiveState> = controller.state

    fun splits(): List<Split> = controller.lastResult.value?.splits ?: emptyList()

    fun save(name: String, onSaved: () -> Unit) {
        val s = state.value
        viewModelScope.launch {
            repository.save(
                type = s.type,
                metrics = s.metrics,
                path = s.path,
                splits = splits(),
                name = name.ifBlank { "" },
            )
            controller.discard()
            onSaved()
        }
    }

    fun discard(onDone: () -> Unit) {
        controller.discard()
        onDone()
    }
}

/**
 * O histórico é a **lista** das corridas, e mais nada.
 *
 * Os recordes e os totais saíram daqui na 2.31.0 e passaram ao hub, por decisão do dono: um
 * facto num sítio só. Enquanto estavam nos dois, este ecrã tinha de ler o histórico inteiro
 * — a `polyline` e os parciais de cada corrida — para escrever seis números que o hub já
 * tinha ao lado.
 */
data class RunHistoryState(
    val runs: List<CorridaNaListaRow> = emptyList(),
    val mes: Mes? = null,
    val tipo: ActivityType? = null,
) {
    val meses: List<Mes> get() = HistoryFilter.mesesDe(runs.map { it.startedAt })

    /** Os tipos que estão de facto no histórico: oferecer «bicicleta» a quem nunca pedalou é ruído. */
    val tipos: List<ActivityType> get() = runs.map { it.type }.distinct()

    val visiveis: List<CorridaNaListaRow>
        get() = HistoryFilter.porMes(runs, mes) { it.startedAt }
            .filter { tipo == null || it.type == tipo }
}

class RunHistoryViewModel(
    repository: RunRepository,
) : ViewModel() {

    // Os filtros vivem à parte do histórico e cruzam-se com ele: assim uma corrida gravada
    // agora entra na lista sem apagar o filtro que estava escolhido.
    private val filtros = MutableStateFlow<Pair<Mes?, ActivityType?>>(null to null)

    val state: StateFlow<RunHistoryState> = combine(
        repository.observeCorridas(),
        filtros,
    ) { runs, (mes, tipo) ->
        RunHistoryState(runs = runs, mes = mes, tipo = tipo)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunHistoryState())

    fun setMes(mes: Mes?) { filtros.value = mes to filtros.value.second }

    fun setTipo(tipo: ActivityType?) { filtros.value = filtros.value.first to tipo }
}

/**
 * O que o hub da corrida mostra antes de se começar a correr.
 *
 * `carregado` distingue «ainda não li a base» de «não há corrida nenhuma» — sem ele, o ecrã
 * pisca o estado vazio à entrada e depois enche-se. É o mesmo campo, pela mesma razão, que o
 * `CentroDeTreino` tem.
 */
data class HubDaCorrida(
    val carregado: Boolean = false,
    val corridasNaSemana: Int = 0,
    val metrosNaSemana: Double = 0.0,
    val movimentoNaSemanaS: Long = 0,
    val ultimas: List<CorridaNaListaRow> = emptyList(),
    val pr1kMs: Long? = null,
    val pr5kMs: Long? = null,
    val pr10kMs: Long? = null,
) {
    /**
     * O ritmo da semana sai dos totais, e não da média dos ritmos de cada corrida — ver o
     * [RitmoMedio], que é onde a razão está escrita.
     */
    val ritmoNaSemanaSegPorKm: Int get() = RitmoMedio.secPorKm(metrosNaSemana, movimentoNaSemanaS)

    /** Sem recorde nenhum, a fila dos três é uma fila de três traços: não se mostra. */
    val temRecordes: Boolean get() = pr1kMs != null || pr5kMs != null || pr10kMs != null
}

/**
 * O modelo de leitura do hub da corrida.
 *
 * Separado do [RunViewModel] de propósito: aquele é o das opções e do comando da corrida, e
 * o ecrã a correr também o usa. Juntar aqui a história fazia o `RunLiveScreen` carregar um
 * repositório para desenhar um cronómetro.
 */
class RunHubViewModel(
    repository: RunRepository,
    hoje: Long = todayEpochDay(),
    zona: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    // A semana é a ISO do `weekStartEpochDay`, que é a mesma do painel de treino e a mesma
    // do progresso. Duas definições de «esta semana» dentro da app davam dois números certos
    // e incompatíveis no mesmo ecrã — foi a lição da 2.25.0.
    private val inicioDaSemana = weekStartEpochDay(hoje)

    val state: StateFlow<HubDaCorrida> = combine(
        repository.observeSemana(
            deMs = inicioDoDiaMs(inicioDaSemana, zona),
            ateMs = inicioDoDiaMs(inicioDaSemana + DIAS_DA_SEMANA, zona),
        ),
        repository.observeUltimas(ULTIMAS_CORRIDAS),
        repository.observeParciais(),
    ) { semana, ultimas, parciais ->
        HubDaCorrida(
            carregado = true,
            corridasNaSemana = semana.corridas,
            metrosNaSemana = semana.metros,
            movimentoNaSemanaS = semana.movimentoS,
            ultimas = ultimas,
            // Os recordes são de sempre e não da semana: um recorde pessoal filtrado por
            // segunda-feira deixa de ser um recorde.
            pr1kMs = RunPrCalc.bestTimeMs(parciais, 1),
            pr5kMs = RunPrCalc.bestTimeMs(parciais, 5),
            pr10kMs = RunPrCalc.bestTimeMs(parciais, 10),
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HubDaCorrida())

    companion object {
        // Três, como o painel de treino mostra três treinos: é o hub irmão, e o esboço
        // desenha duas linhas como exemplo e não como conta.
        const val ULTIMAS_CORRIDAS = 3
        private const val DIAS_DA_SEMANA = 7
    }
}

data class RunDetailState(
    val run: RunEntity? = null,
    val path: List<Pair<Double, Double>> = emptyList(),
    val splits: List<Split> = emptyList(),

    /** A média que o Health Connect tiver para a janela da corrida. Nula sem relógio. */
    val fcMedia: Int? = null,

    /** O Health Connect existe e a leitura da frequência cardíaca ainda não foi concedida. */
    val podePedirFc: Boolean = false,
)

/**
 * O detalhe de uma corrida, e a frequência cardíaca dela se houver.
 *
 * A frequência cardíaca **lê-se ao abrir e não se grava**, por decisão do dono — «só a ler o
 * que houver». Um relógio sincroniza com o Health Connect quando lhe apetece, às vezes horas
 * depois da corrida: gravada no fim, ficava nula para sempre em metade delas. Lida aqui,
 * aparece assim que o relógio a entregar, e sem uma coluna nova na base.
 */
class RunDetailViewModel(
    private val repository: RunRepository,
    private val saude: HealthGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(RunDetailState())
    val state: StateFlow<RunDetailState> = _state

    val permissoesDaFc: Set<String> get() = saude.heartRatePermissions

    fun load(id: String) {
        viewModelScope.launch {
            val run = repository.byId(id) ?: return@launch
            _state.value = RunDetailState(
                run = run,
                path = repository.decodePath(run),
                splits = repository.splitsOf(run),
            )
            lerFc(run)
        }
    }

    /** Depois de a pessoa responder ao pedido de permissão — concedida ou não. */
    fun lerFcOutraVez() {
        val run = _state.value.run ?: return
        viewModelScope.launch { lerFc(run) }
    }

    private suspend fun lerFc(run: RunEntity) {
        val disponivel = saude.availability() == HealthAvailability.AVAILABLE
        val concedida = disponivel && saude.hasHeartRatePermission()
        val media = if (concedida) saude.heartRateAvg(run.startedAt, run.endedAt) else null
        _state.value = _state.value.copy(fcMedia = media, podePedirFc = disponivel && !concedida)
    }
}
