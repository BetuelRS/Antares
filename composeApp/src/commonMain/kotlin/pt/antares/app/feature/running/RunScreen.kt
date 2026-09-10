package pt.antares.app.feature.running

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.paceUnitLabel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.formatDurationMin
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.ui.CorridaNaLista
import pt.antares.app.feature.running.ui.HubDaCorrida
import pt.antares.app.feature.running.ui.LocationPermissionController
import pt.antares.app.feature.running.ui.LocationPermissionStatus
import pt.antares.app.feature.running.ui.PrTile
import pt.antares.app.feature.running.ui.RunFormat
import pt.antares.app.feature.running.ui.RunGoalType
import pt.antares.app.feature.running.ui.RunHubViewModel
import pt.antares.app.feature.running.ui.RunViewModel
import pt.antares.app.feature.running.ui.activityLabel
import pt.antares.app.feature.running.ui.rememberLocationPermission
import pt.antares.app.feature.running.ui.rememberLocationServicesEnabled
import pt.antares.app.feature.running.ui.rememberOpenAppSettings
import pt.antares.app.feature.running.ui.rememberOpenLocationSettings
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.nav_run
import pt.antares.app.generated.resources.run_autopause
import pt.antares.app.generated.resources.run_goal_distance
import pt.antares.app.generated.resources.run_goal_label
import pt.antares.app.generated.resources.run_goal_min
import pt.antares.app.generated.resources.run_goal_none
import pt.antares.app.generated.resources.run_goal_time
import pt.antares.app.generated.resources.run_gps_off_body
import pt.antares.app.generated.resources.run_gps_off_cta
import pt.antares.app.generated.resources.run_gps_off_title
import pt.antares.app.generated.resources.run_history_empty
import pt.antares.app.generated.resources.run_hub_autopause_off
import pt.antares.app.generated.resources.run_hub_autopause_on
import pt.antares.app.generated.resources.run_hub_goal_none
import pt.antares.app.generated.resources.run_hub_goal_value
import pt.antares.app.generated.resources.run_hub_options_change
import pt.antares.app.generated.resources.run_hub_options_title
import pt.antares.app.generated.resources.run_hub_recent
import pt.antares.app.generated.resources.run_hub_see_history
import pt.antares.app.generated.resources.run_hub_start
import pt.antares.app.generated.resources.run_hub_week
import pt.antares.app.generated.resources.run_hub_week_line
import pt.antares.app.generated.resources.run_hub_week_runs
import pt.antares.app.generated.resources.run_oem_body
import pt.antares.app.generated.resources.run_oem_ok
import pt.antares.app.generated.resources.run_oem_title
import pt.antares.app.generated.resources.run_perm_body
import pt.antares.app.generated.resources.run_perm_cta
import pt.antares.app.generated.resources.run_perm_denied_body
import pt.antares.app.generated.resources.run_perm_denied_retry
import pt.antares.app.generated.resources.run_perm_denied_settings
import pt.antares.app.generated.resources.run_perm_denied_title
import pt.antares.app.generated.resources.run_perm_title
import pt.antares.app.generated.resources.run_pr_10k
import pt.antares.app.generated.resources.run_pr_1k
import pt.antares.app.generated.resources.run_pr_5k
import pt.antares.app.generated.resources.run_pr_title
import pt.antares.app.generated.resources.run_type_ride
import pt.antares.app.generated.resources.run_type_run
import pt.antares.app.generated.resources.run_type_walk
import pt.antares.app.generated.resources.workout_hub_run_none_week

/**
 * O hub da corrida: o que se fez primeiro, e depois as opções.
 *
 * Até à 2.31.0 era um formulário — três grupos de opções e dois botões, e nem um número do
 * que se tinha corrido. A `estudo/areas/11-corrida.md` chama-lhe *«uma lista de opções, não um
 * ecrã de corrida»*, e o esboço 11 desenha o que está aqui: a semana em cima, as últimas
 * corridas e os recordes à vista, e as opções recolhidas numa linha que abre uma folha.
 *
 * A seta de voltar nasceu na 2.20.1, e o título com ela: empurrado a partir do painel de
 * treino, o ecrã não tem barra de baixo por onde sair.
 */
@Composable
fun RunScreen(
    onBack: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenHistory: () -> Unit,
    onRun: (String) -> Unit,
    viewModel: RunViewModel = koinViewModel(),
    hubViewModel: RunHubViewModel = koinViewModel(),
) {
    val permission = rememberLocationPermission()
    val hub by hubViewModel.state.collectAsState()
    val unidades = rememberUnitSystem()
    val hoje = remember { todayEpochDay() }

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.nav_run), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).larguraDeLeitura(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                CartaoDaSemana(hub, unidades) {
                    PortaDaCorrida(permission, viewModel, onStart = { viewModel.start(); onOpenLive() })
                }
            }
            if (hub.carregado && hub.ultimas.isEmpty()) {
                // Quem nunca correu não tem lista nem recordes: uma frase, e o botão de cima.
                item {
                    Text(
                        stringResource(Res.string.run_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // As últimas antes dos recordes, pela ordem do esboço 11: o que se fez esta semana e
            // nas últimas saídas é o que se lê ao abrir; os recordes mudam uma vez por mês.
            if (hub.ultimas.isNotEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.run_hub_recent),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
                items(hub.ultimas, key = { it.id }) { run -> CorridaNaLista(run, unidades, hoje, onRun) }
                item {
                    SecondaryButton(
                        stringResource(Res.string.run_hub_see_history),
                        onOpenHistory,
                        Modifier.fillMaxWidth(),
                    )
                }
            }
            if (hub.temRecordes) {
                item { Recordes(hub) }
            }
        }
    }
}

/**
 * A semana em primeiro — é a razão de abrir o ecrã, nas palavras do esboço — e por baixo
 * dela a porta para correr, que muda com o estado da permissão.
 */
@Composable
private fun CartaoDaSemana(hub: HubDaCorrida, unidades: UnitSystem, porta: @Composable () -> Unit) {
    val virgula = virgulaDecimal()
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                stringResource(Res.string.run_hub_week),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (hub.corridasNaSemana > 0) {
                Text(
                    "${RunFormat.distanciaDaSemana(hub.metrosNaSemana, unidades, virgula)} " +
                        stringResource(distanceUnitLabel(unidades)),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    stringResource(
                        Res.string.run_hub_week_line,
                        pluralStringResource(Res.plurals.run_hub_week_runs, hub.corridasNaSemana, hub.corridasNaSemana),
                        formatDurationMin((hub.movimentoNaSemanaS / SEGUNDOS_POR_MINUTO).toInt()),
                        RunFormat.pace(hub.ritmoNaSemanaSegPorKm, unidades) +
                            stringResource(paceUnitLabel(unidades)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(Res.string.workout_hub_run_none_week),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            porta()
        }
    }
}

/**
 * O que está por baixo da semana: o botão de começar, ou o que falta para ele existir.
 *
 * Os quatro estados da permissão eram o ecrã inteiro, e quem tinha negado a localização não
 * via nada do que já tinha corrido. Por decisão do dono passaram a ser o fundo do cartão — a
 * semana, as últimas e os recordes são história gravada, e não precisam de GPS.
 */
@Composable
private fun PortaDaCorrida(
    permission: LocationPermissionController,
    viewModel: RunViewModel,
    onStart: () -> Unit,
) {
    when (permission.status) {
        LocationPermissionStatus.GRANTED -> {
            if (rememberLocationServicesEnabled()) {
                ComecarEOpcoes(viewModel, onStart)
            } else {
                Pedido(
                    titulo = stringResource(Res.string.run_gps_off_title),
                    corpo = stringResource(Res.string.run_gps_off_body),
                    cta = stringResource(Res.string.run_gps_off_cta),
                    onCta = rememberOpenLocationSettings(),
                )
            }
        }

        LocationPermissionStatus.NOT_REQUESTED ->
            Pedido(
                titulo = stringResource(Res.string.run_perm_title),
                corpo = stringResource(Res.string.run_perm_body),
                cta = stringResource(Res.string.run_perm_cta),
                onCta = permission::request,
            )

        LocationPermissionStatus.DENIED ->
            Pedido(
                titulo = stringResource(Res.string.run_perm_denied_title),
                corpo = stringResource(Res.string.run_perm_denied_body),
                cta = stringResource(Res.string.run_perm_denied_retry),
                onCta = permission::request,
                secundario = stringResource(Res.string.run_perm_denied_settings),
                onSecundario = rememberOpenAppSettings(),
            )
    }
}

/**
 * O botão grande e, por baixo, as opções numa linha — que se mudam raramente, e por isso
 * deixaram de ocupar o ecrã: o «Alterar» abre-as numa folha.
 */
@Composable
private fun ComecarEOpcoes(viewModel: RunViewModel, onStart: () -> Unit) {
    val type by viewModel.type.collectAsState()
    val autoPause by viewModel.autoPause.collectAsState()
    val goalType by viewModel.goalType.collectAsState()
    val goalValue by viewModel.goalValue.collectAsState()
    val oemShown by viewModel.oemWarningShown.collectAsState()
    var folhaAberta by remember { mutableStateOf(false) }

    if (!oemShown) {
        AlertDialog(
            onDismissRequest = viewModel::dismissOemWarning,
            title = { Text(stringResource(Res.string.run_oem_title)) },
            text = { Text(stringResource(Res.string.run_oem_body)) },
            confirmButton = {
                PrimaryButton(stringResource(Res.string.run_oem_ok), onClick = viewModel::dismissOemWarning)
            },
        )
    }

    PrimaryButton(stringResource(Res.string.run_hub_start), onStart, Modifier.fillMaxWidth())
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            resumoDasOpcoes(type, autoPause, goalType, goalValue),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { folhaAberta = true }) {
            Text(stringResource(Res.string.run_hub_options_change))
        }
    }

    if (folhaAberta) {
        FolhaDasOpcoes(viewModel, onDismiss = { folhaAberta = false })
    }
}

@Composable
private fun resumoDasOpcoes(type: ActivityType, autoPause: Boolean, goalType: RunGoalType, goalValue: Int): String {
    val unidades = rememberUnitSystem()
    val pausa = stringResource(if (autoPause) Res.string.run_hub_autopause_on else Res.string.run_hub_autopause_off)
    val meta = when (goalType) {
        RunGoalType.NONE -> stringResource(Res.string.run_hub_goal_none)
        RunGoalType.DISTANCE -> stringResource(
            Res.string.run_hub_goal_value,
            "${rotuloDaMeta(goalValue, unidades)} ${stringResource(distanceUnitLabel(unidades))}",
        )
        RunGoalType.TIME -> stringResource(
            Res.string.run_hub_goal_value,
            "${goalValue / SEGUNDOS_POR_MINUTO} ${stringResource(Res.string.run_goal_min)}",
        )
    }
    return listOf(stringResource(activityLabel(type)), pausa, meta).joinToString(SEPARADOR)
}

/**
 * O número que se mostra para uma meta em metros: o redondo da pastilha, quando é uma delas.
 *
 * Quando não é — uma meta de 5 km gravada antes de a pessoa passar a milhas —, escreve-se a
 * distância exacta. Dividir e arredondar dizia «3 mi» a uma meta de 3,1.
 */
@Composable
private fun rotuloDaMeta(metros: Int, unidades: UnitSystem): String =
    metasDe(unidades).firstOrNull { it.first == metros }?.second?.toString()
        ?: RunFormat.distance(metros.toDouble(), unidades, virgulaDecimal())

private fun metasDe(unidades: UnitSystem) = if (unidades == UnitSystem.IMPERIAL) METAS_MI else METAS_KM

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FolhaDasOpcoes(viewModel: RunViewModel, onDismiss: () -> Unit) {
    val type by viewModel.type.collectAsState()
    val autoPause by viewModel.autoPause.collectAsState()
    val goalType by viewModel.goalType.collectAsState()
    val goalValue by viewModel.goalValue.collectAsState()
    val unidades = rememberUnitSystem()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(stringResource(Res.string.run_hub_options_title), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TypeChip(ActivityType.RUN, type, stringResource(Res.string.run_type_run), viewModel::setType)
                TypeChip(ActivityType.WALK, type, stringResource(Res.string.run_type_walk), viewModel::setType)
                TypeChip(ActivityType.RIDE, type, stringResource(Res.string.run_type_ride), viewModel::setType)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.run_autopause), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = autoPause, onCheckedChange = viewModel::setAutoPause)
            }

            Text(stringResource(Res.string.run_goal_label), style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(
                    selected = goalType == RunGoalType.NONE,
                    onClick = { viewModel.setGoal(RunGoalType.NONE, 0) },
                    label = { Text(stringResource(Res.string.run_goal_none)) },
                )
                FilterChip(
                    selected = goalType == RunGoalType.DISTANCE,
                    // A do meio das da unidade escolhida: 5 km, ou 3 mi. Era sempre 5 000 m, e em
                    // milhas não havia pastilha nenhuma acesa a seguir ao toque.
                    onClick = { viewModel.setGoal(RunGoalType.DISTANCE, metasDe(unidades)[1].first) },
                    label = { Text(stringResource(Res.string.run_goal_distance)) },
                )
                FilterChip(
                    selected = goalType == RunGoalType.TIME,
                    onClick = { viewModel.setGoal(RunGoalType.TIME, METAS_S[1]) },
                    label = { Text(stringResource(Res.string.run_goal_time)) },
                )
            }
            if (goalType == RunGoalType.DISTANCE) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    // A meta guarda-se sempre em metros; o que muda são as distâncias redondas
                    // que se oferecem. Três, cinco e dez quilómetros não são distâncias redondas
                    // para quem corre em milhas, e converter as métricas dava «3,1 mi».
                    metasDe(unidades).forEach { (metros, valor) ->
                        FilterChip(
                            selected = goalValue == metros,
                            onClick = { viewModel.setGoal(RunGoalType.DISTANCE, metros) },
                            label = { Text("$valor ${stringResource(distanceUnitLabel(unidades))}") },
                        )
                    }
                }
            }
            if (goalType == RunGoalType.TIME) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    METAS_S.forEach { s ->
                        FilterChip(
                            selected = goalValue == s,
                            onClick = { viewModel.setGoal(RunGoalType.TIME, s) },
                            label = { Text("${s / SEGUNDOS_POR_MINUTO} ${stringResource(Res.string.run_goal_min)}") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Recordes(hub: HubDaCorrida) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(Res.string.run_pr_title), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            PrTile(stringResource(Res.string.run_pr_1k), recorde(hub.pr1kMs), Modifier.weight(1f))
            PrTile(stringResource(Res.string.run_pr_5k), recorde(hub.pr5kMs), Modifier.weight(1f))
            PrTile(stringResource(Res.string.run_pr_10k), recorde(hub.pr10kMs), Modifier.weight(1f))
        }
    }
}

private fun recorde(ms: Long?): String = ms?.let { RunFormat.clock(it) } ?: SEM_RECORDE

@Composable
private fun TypeChip(value: ActivityType, selected: ActivityType, label: String, onSelect: (ActivityType) -> Unit) {
    FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
}

@Composable
private fun Pedido(
    titulo: String,
    corpo: String,
    cta: String,
    onCta: () -> Unit,
    secundario: String? = null,
    onSecundario: (() -> Unit)? = null,
) {
    Text(titulo, style = MaterialTheme.typography.titleMedium)
    Text(corpo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PrimaryButton(text = cta, onClick = onCta, modifier = Modifier.fillMaxWidth())
    if (secundario != null && onSecundario != null) {
        SecondaryButton(text = secundario, onClick = onSecundario, modifier = Modifier.fillMaxWidth())
    }
}

private const val SEPARADOR = " · "
private const val SEM_RECORDE = "--"
private const val SEGUNDOS_POR_MINUTO = 60

// Metas em metros, com o número redondo que se mostra ao lado. As milhas não são conversões
// das métricas: são as distâncias que quem corre em milhas reconhece.
private val METAS_KM = listOf(3000 to 3, 5000 to 5, 10000 to 10)
private val METAS_MI = listOf(1609 to 1, 4828 to 3, 8047 to 5)
private val METAS_S = listOf(1200, 1800, 2700)
