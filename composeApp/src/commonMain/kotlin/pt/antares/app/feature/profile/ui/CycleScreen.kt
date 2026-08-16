package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.CycleCalc
import pt.antares.app.core.calc.CycleDateError
import pt.antares.app.core.calc.CycleEdit
import pt.antares.app.core.database.entities.CycleEntity
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.DateDialog
import pt.antares.app.core.designsystem.components.rememberApagarComDesfazer
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.SplitRow
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.CycleRepository
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

data class CycleState(

    val entries: List<CycleEntity> = emptyList(),

    /** A última data recusada, para o ecrã dizer porquê em vez de não fazer nada. */
    val erro: CycleDateError? = null,
) {
    private val inicios: List<Long> get() = entries.map { it.startEpochDay }

    val periodos: List<CycleEdit.Periodo>
        get() = entries.map { CycleEdit.Periodo(it.id, it.startEpochDay, it.endEpochDay) }

    val averageCycleDays: Int? get() = CycleCalc.averageCycleDays(inicios)
    val dayOfCycle: Int? get() = CycleCalc.dayOfCycle(inicios, todayEpochDay())
    val predictedNextStart: Long? get() = CycleCalc.predictedNextStart(inicios)
    val retentionLikely: Boolean get() = CycleCalc.retentionLikely(inicios, todayEpochDay())

    val openPeriod: Boolean
        get() = entries.lastOrNull()?.endEpochDay == null && entries.isNotEmpty()
}

class CycleViewModel(
    private val repository: CycleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CycleState())
    val state: StateFlow<CycleState> = _state

    init {
        repository.observeAll()
            .onEach { linhas -> _state.update { it.copy(entries = linhas) } }
            .launchIn(viewModelScope)
    }

    /**
     * As três escritas passam pela mesma validação, e é ela que decide se há escrita: um
     * período no futuro ou por cima de outro entra na média dos ciclos e leva consigo a
     * previsão do próximo — que é a única coisa que este ecrã serve para fazer.
     */
    fun start(epochDay: Long = todayEpochDay()) {
        val s = _state.value
        val erro = CycleEdit.validateStart(epochDay, todayEpochDay(), s.periodos)
        if (erro != null) {
            _state.update { it.copy(erro = erro) }
            return
        }
        viewModelScope.launch { repository.recordStart(epochDay) }
        _state.update { it.copy(erro = null) }
    }

    fun end(epochDay: Long = todayEpochDay()) {
        val s = _state.value
        val aberto = s.entries.lastOrNull { it.endEpochDay == null } ?: return
        val erro = CycleEdit.validateEnd(
            inicio = aberto.startEpochDay,
            novoFim = epochDay,
            hoje = todayEpochDay(),
            existentes = s.periodos,
            aIgnorar = aberto.id,
        )
        if (erro != null) {
            _state.update { it.copy(erro = erro) }
            return
        }
        viewModelScope.launch { repository.recordEnd(epochDay) }
        _state.update { it.copy(erro = null) }
    }

    fun editar(id: String, inicio: Long, fim: Long?) {
        val s = _state.value
        val hoje = todayEpochDay()
        val erro = CycleEdit.validateStart(inicio, hoje, s.periodos, aIgnorar = id)
            ?: fim?.let { CycleEdit.validateEnd(inicio, it, hoje, s.periodos, aIgnorar = id) }
        if (erro != null) {
            _state.update { it.copy(erro = erro) }
            return
        }
        viewModelScope.launch { repository.updateDates(id, inicio, fim) }
        _state.update { it.copy(erro = null) }
    }

    fun dispensarErro() = _state.update { it.copy(erro = null) }

    fun delete(id: String) = viewModelScope.launch { repository.delete(id) }

    fun restore(entry: CycleEntity) = viewModelScope.launch { repository.restore(entry) }
}

@Composable
fun CycleScreen(
    onBack: () -> Unit,
    viewModel: CycleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Qual dos três seletores está aberto: início, fim, ou a correção de um registo.
    var aEscolher by remember { mutableStateOf<EscolhaDeData?>(null) }
    val apagar = rememberApagarComDesfazer()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.cycle_title), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .larguraDeLeitura()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Text(
                    stringResource(Res.string.cycle_why),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                Text(
                    stringResource(Res.string.cycle_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            item { StatusCard(state) }

            state.erro?.let { erro ->
                item {
                    Text(
                        stringResource(erroDaData(erro)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item {
                // Dois botões por marcação: «hoje», que é o gesto de nove em cada dez vezes,
                // e a data, para quem se lembra dois dias depois. Antes só havia o primeiro,
                // e marcar um início atrasado dava um ciclo com dois dias a mais.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    PrimaryButton(
                        text = stringResource(Res.string.cycle_start_today),
                        onClick = { viewModel.start() },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(Res.string.cycle_pick_date),
                        onClick = { aEscolher = EscolhaDeData.Inicio },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.openPeriod) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        PrimaryButton(
                            text = stringResource(Res.string.cycle_end_today),
                            onClick = { viewModel.end() },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = stringResource(Res.string.cycle_pick_date),
                            onClick = { aEscolher = EscolhaDeData.Fim },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            items(state.entries.reversed(), key = { it.id }) { linha ->
                CycleRow(
                    linha = linha,
                    onEditStart = { aEscolher = EscolhaDeData.CorrigirInicio(linha.id) },
                    onEditEnd = { aEscolher = EscolhaDeData.CorrigirFim(linha.id) },
                    onDelete = {
                        apagar({ viewModel.delete(linha.id) }, { viewModel.restore(linha) })
                    },
                )
            }
        }
    }

    aEscolher?.let { escolha ->
        val linha = escolha.id?.let { id -> state.entries.firstOrNull { it.id == id } }
        DateDialog(
            title = stringResource(escolha.titulo),
            initialEpochDay = escolha.inicial(linha),
            onPick = { dia ->
                viewModel.dispensarErro()
                when (escolha) {
                    is EscolhaDeData.Inicio -> viewModel.start(dia)
                    is EscolhaDeData.Fim -> viewModel.end(dia)
                    is EscolhaDeData.CorrigirInicio ->
                        linha?.let { viewModel.editar(it.id, dia, it.endEpochDay) }
                    is EscolhaDeData.CorrigirFim ->
                        linha?.let { viewModel.editar(it.id, it.startEpochDay, dia) }
                }
            },
            onDismiss = { aEscolher = null },
        )
    }
}

/** As quatro datas que este ecrã pede, e o que cada uma faz com o dia escolhido. */
private sealed interface EscolhaDeData {
    val id: String? get() = null
    val titulo: StringResource

    fun inicial(linha: CycleEntity?): Long = todayEpochDay()

    data object Inicio : EscolhaDeData {
        override val titulo get() = Res.string.cycle_start_on
    }

    data object Fim : EscolhaDeData {
        override val titulo get() = Res.string.cycle_end_on
    }

    data class CorrigirInicio(override val id: String) : EscolhaDeData {
        override val titulo get() = Res.string.cycle_start_on
        override fun inicial(linha: CycleEntity?) = linha?.startEpochDay ?: todayEpochDay()
    }

    data class CorrigirFim(override val id: String) : EscolhaDeData {
        override val titulo get() = Res.string.cycle_end_on
        override fun inicial(linha: CycleEntity?) = linha?.endEpochDay ?: todayEpochDay()
    }
}

private fun erroDaData(erro: CycleDateError) = when (erro) {
    CycleDateError.NO_FUTURO -> Res.string.cycle_error_future
    CycleDateError.SOBREPOE -> Res.string.cycle_error_overlap
    CycleDateError.FIM_ANTES_DO_INICIO -> Res.string.cycle_error_end_before_start
}

@Composable
private fun StatusCard(state: CycleState) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        val dia = state.dayOfCycle
        if (dia == null) {
            Text(
                stringResource(Res.string.cycle_no_data),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@AntaresCard
        }
        Text(
            stringResource(Res.string.cycle_day, dia),
            style = MaterialTheme.typography.titleMedium,
        )

        state.averageCycleDays?.let {
            Text(
                stringResource(Res.string.cycle_average, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.predictedNextStart?.let {
            Text(
                stringResource(Res.string.cycle_next, dayShortDated(it)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.retentionLikely) {

            Text(
                stringResource(Res.string.cycle_retention),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun CycleRow(
    linha: CycleEntity,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onDelete: () -> Unit,
) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        SplitRow(
            leading = {
                Column {
                    // A data toca-se para a corrigir: quem se enganou a marcar só tinha o
                    // caixote do lixo, e apagar levava a duração do ciclo consigo.
                    Text(
                        dayShortDated(linha.startEpochDay),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable(role = Role.Button, onClick = onEditStart),
                    )
                    val duracao = CycleCalc.periodLengthDays(linha.startEpochDay, linha.endEpochDay)
                    Text(

                        if (duracao == null) {
                            stringResource(Res.string.cycle_open)
                        } else {
                            stringResource(Res.string.cycle_days, duracao)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(role = Role.Button, onClick = onEditEnd),
                    )
                }
            },
            trailing = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.common_delete),
                    )
                }
            },
        )
    }
}
