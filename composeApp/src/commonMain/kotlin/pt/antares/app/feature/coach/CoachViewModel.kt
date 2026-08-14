package pt.antares.app.feature.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import pt.antares.app.core.calc.WeeklyAggregate
import pt.antares.app.core.coach.CoachRepository
import pt.antares.app.core.coach.CoachTrigger
import pt.antares.app.core.database.entities.CoachReportEntity
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult

data class CoachReportUi(
    val id: String,
    val weekStartEpochDay: Long,
    val weekEndEpochDay: Long,
    val wins: List<String>,
    val observations: List<String>,
    val adjustments: List<String>,
    val focus: String,
    val aggregate: WeeklyAggregate?,
    val proposedKcal: Int?,
    val previousKcal: Int?,

    val observedTdee: Int?,
    val proposalAccepted: Boolean?,
) {

    // Só há proposta por responder se ela existe e ainda não foi aceite nem recusada —
    // `proposalAccepted` a null é "por responder", não "recusada".
    val hasOpenProposal: Boolean get() = proposedKcal != null && proposalAccepted == null

    // Distingue o relatório da semana passada de um antigo aberto no histórico: só o
    // primeiro é acionável.
    fun isFresh(todayEpochDay: Long): Boolean =
        CoachTrigger.targetWeekStart(todayEpochDay) == weekStartEpochDay
    val deltaKcal: Int? get() = if (proposedKcal != null && previousKcal != null) {
        proposedKcal - previousKcal
    } else {
        null
    }
}

data class CoachState(
    val loading: Boolean = false,
    val reports: List<CoachReportUi> = emptyList(),
    val error: AppError? = null,
) {
    val latest: CoachReportUi? get() = reports.firstOrNull()
}

class CoachViewModel(
    private val repository: CoachRepository,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<AppError?>(null)

    val state: StateFlow<CoachState> = repository.observeReports()
        .map { rows -> CoachState(reports = rows.map(::toUi)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachState())

    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    val error: StateFlow<AppError?> = _error.asStateFlow()

    // Gerar ignora-se se já estiver a gerar: o relatório escreve por semana, e duas
    // gerações a correr ao mesmo tempo disputariam a mesma linha.
    fun generate() {
        if (_loading.value) return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            when (val r = repository.generateManual()) {
                is AppResult.Failure -> _error.value = r.error
                is AppResult.Success -> Unit
            }
            _loading.value = false
        }
    }

    fun acceptProposal(report: CoachReportUi) {
        viewModelScope.launch {
            val entity = repository.byId(report.id) ?: return@launch
            when (val r = repository.acceptProposal(entity)) {
                is AppResult.Failure -> _error.value = r.error
                is AppResult.Success -> Unit
            }
        }
    }

    fun dismissProposal(report: CoachReportUi) {
        viewModelScope.launch {
            repository.byId(report.id)?.let { repository.dismissProposal(it) }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun toUi(e: CoachReportEntity) = CoachReportUi(
        id = e.id,
        weekStartEpochDay = e.weekStartEpochDay,
        weekEndEpochDay = e.weekStartEpochDay + 6,
        wins = decodeList(e.winsJson),
        observations = decodeList(e.observationsJson),
        adjustments = decodeList(e.adjustmentsJson),
        focus = e.focus,
        // Um agregado que não desserializa vira null e o ecrã mostra o relatório sem os
        // números: um relatório escrito por uma versão anterior não pode deitar a lista abaixo.
        aggregate = runCatching { json.decodeFromString<WeeklyAggregate>(e.aggregateJson) }.getOrNull(),
        proposedKcal = e.proposedKcal,
        previousKcal = e.previousKcal,
        observedTdee = e.observedTdee,
        proposalAccepted = e.proposalAccepted,
    )

    private fun decodeList(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
}
