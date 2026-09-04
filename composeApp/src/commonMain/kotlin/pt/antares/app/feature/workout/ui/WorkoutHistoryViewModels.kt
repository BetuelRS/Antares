package pt.antares.app.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pt.antares.app.core.calc.HistoryFilter
import pt.antares.app.core.calc.Mes
import pt.antares.app.core.calc.StatsPeriod
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.workout.data.EstatisticasDoTreino
import pt.antares.app.feature.workout.data.ExerciseRecord
import pt.antares.app.feature.workout.data.RoutineOption
import pt.antares.app.feature.workout.data.SessionBreakdown
import pt.antares.app.feature.workout.data.SessionSummary
import pt.antares.app.feature.workout.data.WorkoutHistoryRepository

data class WorkoutHistoryState(
    val todos: List<SessionSummary> = emptyList(),
    val rotinas: List<RoutineOption> = emptyList(),
    val mes: Mes? = null,
    val rotinaId: String? = null,
) {
    val meses: List<Mes> get() = HistoryFilter.mesesDe(todos.map { it.startedAt })

    /**
     * A lista depois dos filtros. Os dois cruzam-se: mês **e** rotina, e não um ou outro —
     * quem procura os empurrares de fevereiro procura os dois ao mesmo tempo.
     *
     * **Filtra-se por rotina e não por exercício**, que era o que estava. Filtrar sessões por
     * exercício devolve os dias em que ele foi feito, e mostra a data e o volume da sessão —
     * quem escreve «supino» quer a progressão do supino, e essa vive no detalhe do exercício,
     * que já tem o gráfico. É a queixa do `estudo/areas/10`, e o esboço 10 desenha a rotina.
     */
    val visiveis: List<SessionSummary>
        get() = HistoryFilter.porMes(todos, mes) { it.startedAt }
            .filter { rotinaId == null || rotinaId == it.routineId }

}

class WorkoutHistoryViewModel(
    private val repository: WorkoutHistoryRepository,
) : ViewModel() {

    private val filtros = MutableStateFlow(WorkoutHistoryState())
    val state: StateFlow<WorkoutHistoryState> = filtros

    init {
        repository.observeHistory()
            .onEach { linhas -> filtros.update { it.copy(todos = linhas) } }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            filtros.update { it.copy(rotinas = repository.routineOptions()) }
        }
    }

    fun setMes(mes: Mes?) = filtros.update { it.copy(mes = mes) }

    fun setRotina(id: String?) = filtros.update { it.copy(rotinaId = id) }

    fun limparFiltros() = filtros.update { it.copy(mes = null, rotinaId = null) }
}

class WorkoutDetailViewModel(
    private val repository: WorkoutHistoryRepository,
) : ViewModel() {
    private val _breakdown = MutableStateFlow<SessionBreakdown?>(null)
    val breakdown: StateFlow<SessionBreakdown?> = _breakdown

    fun load(sessionId: String) {
        viewModelScope.launch { _breakdown.update { repository.breakdown(sessionId) } }
    }
}

data class WorkoutStatsState(
    val loading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.WEEK,
    val estatisticas: EstatisticasDoTreino = EstatisticasDoTreino(),
    val records: List<ExerciseRecord> = emptyList(),
) {
    /** O recorde mais recente, para o ecrã o poder assinalar. Nulo sem recordes nenhuns. */
    val recordeMaisRecente: Long? get() = records.maxOfOrNull { it.epochDay }
}

/**
 * As estatísticas do treino.
 *
 * **Abre na semana e não no dia**, ao contrário do ecrã da nutrição: um dia de nutrição é uma
 * pergunta que se faz — «o que é que comi hoje?» —, e um dia de treino é um treino ou nenhum.
 * A pergunta deste ecrã é sobre o que se repete.
 */
class WorkoutStatsViewModel(
    private val repository: WorkoutHistoryRepository,
) : ViewModel() {

    private val period = MutableStateFlow(StatsPeriod.WEEK)
    private val records = MutableStateFlow<List<ExerciseRecord>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<WorkoutStatsState> = combine(
        period,
        period.flatMapLatest { p ->
            val hoje = todayEpochDay()
            repository.observeEstatisticas(
                desdeMs = Clock.System.now().toEpochMilliseconds() - p.dias * MS_POR_DIA,
                diasDoPeriodo = p.dias,
                hojeEpochDay = hoje,
                semanas = p.semanas,
            )
        },
        records,
    ) { p, estatisticas, recs ->
        WorkoutStatsState(
            loading = estatisticas.loading,
            period = p,
            estatisticas = estatisticas,
            records = recs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PARAGEM_MS), WorkoutStatsState())

    init {
        // Os recordes são o melhor de **sempre** e não mudam com o período escolhido — lê-los
        // outra vez a cada toque num chip era percorrer todas as séries de todos os treinos
        // para chegar exactamente ao mesmo resultado.
        viewModelScope.launch { records.value = repository.records() }
    }

    fun setPeriod(p: StatsPeriod) { period.value = p }

    private companion object {
        const val MS_POR_DIA = 24L * 60 * 60 * 1000
        const val PARAGEM_MS = 5_000L
    }
}
