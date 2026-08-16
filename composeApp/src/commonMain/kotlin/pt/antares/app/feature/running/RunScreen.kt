package pt.antares.app.feature.running

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.ui.LocationPermissionStatus
import pt.antares.app.feature.running.ui.RunGoalType
import pt.antares.app.feature.running.ui.RunViewModel
import pt.antares.app.feature.running.ui.rememberLocationPermission
import pt.antares.app.feature.running.ui.rememberLocationServicesEnabled
import pt.antares.app.feature.running.ui.rememberOpenAppSettings
import pt.antares.app.feature.running.ui.rememberOpenLocationSettings
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_autopause
import pt.antares.app.generated.resources.run_goal_distance
import pt.antares.app.generated.resources.run_goal_label
import pt.antares.app.generated.resources.run_goal_min
import pt.antares.app.generated.resources.run_goal_none
import pt.antares.app.generated.resources.run_goal_time
import pt.antares.app.generated.resources.run_gps_off_body
import pt.antares.app.generated.resources.run_gps_off_cta
import pt.antares.app.generated.resources.run_gps_off_title
import pt.antares.app.generated.resources.run_history_title
import pt.antares.app.generated.resources.run_hub_start
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
import pt.antares.app.generated.resources.run_type_ride
import pt.antares.app.generated.resources.run_type_run
import pt.antares.app.generated.resources.run_type_walk

@Composable
fun RunScreen(
    onOpenLive: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: RunViewModel = koinViewModel(),
) {
    val permission = rememberLocationPermission()
    when (permission.status) {
        LocationPermissionStatus.GRANTED -> {

            val locationEnabled = rememberLocationServicesEnabled()
            if (locationEnabled) {
                RunHub(
                    viewModel = viewModel,
                    onStart = { viewModel.start(); onOpenLive() },
                    onHistory = onOpenHistory,
                )
            } else {
                val openLocationSettings = rememberOpenLocationSettings()
                PermissionRationale(
                    title = stringResource(Res.string.run_gps_off_title),
                    body = stringResource(Res.string.run_gps_off_body),
                    cta = stringResource(Res.string.run_gps_off_cta),
                    onCta = openLocationSettings,
                )
            }
        }

        LocationPermissionStatus.NOT_REQUESTED ->
            PermissionRationale(
                title = stringResource(Res.string.run_perm_title),
                body = stringResource(Res.string.run_perm_body),
                cta = stringResource(Res.string.run_perm_cta),
                onCta = permission::request,
            )

        LocationPermissionStatus.DENIED -> {
            val openSettings = rememberOpenAppSettings()
            PermissionRationale(
                title = stringResource(Res.string.run_perm_denied_title),
                body = stringResource(Res.string.run_perm_denied_body),
                cta = stringResource(Res.string.run_perm_denied_retry),
                onCta = permission::request,
                secondaryCta = stringResource(Res.string.run_perm_denied_settings),
                onSecondary = openSettings,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunHub(
    viewModel: RunViewModel,
    onStart: () -> Unit,
    onHistory: () -> Unit,
) {
    val type by viewModel.type.collectAsState()
    val autoPause by viewModel.autoPause.collectAsState()
    val oemShown by viewModel.oemWarningShown.collectAsState()
    val goalType by viewModel.goalType.collectAsState()
    val goalValue by viewModel.goalValue.collectAsState()
    val unidades = rememberUnitSystem()

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

    Column(
        modifier = Modifier.fillMaxSize().larguraDeLeitura().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
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
                onClick = { viewModel.setGoal(RunGoalType.DISTANCE, 5000) },
                label = { Text(stringResource(Res.string.run_goal_distance)) },
            )
            FilterChip(
                selected = goalType == RunGoalType.TIME,
                onClick = { viewModel.setGoal(RunGoalType.TIME, 1800) },
                label = { Text(stringResource(Res.string.run_goal_time)) },
            )
        }
        if (goalType == RunGoalType.DISTANCE) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                // A meta guarda-se sempre em metros; o que muda são as distâncias redondas
                // que se oferecem. Três, cinco e dez quilómetros não são distâncias redondas
                // para quem corre em milhas, e converter as métricas dava «3,1 mi».
                val metas = if (unidades == UnitSystem.IMPERIAL) METAS_MI else METAS_KM
                metas.forEach { (metros, valor) ->
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
                listOf(1200, 1800, 2700).forEach { s ->
                    FilterChip(
                        selected = goalValue == s,
                        onClick = { viewModel.setGoal(RunGoalType.TIME, s) },
                        label = { Text("${s / 60} ${stringResource(Res.string.run_goal_min)}") },
                    )
                }
            }
        }
        PrimaryButton(stringResource(Res.string.run_hub_start), onStart, Modifier.fillMaxWidth())
        SecondaryButton(stringResource(Res.string.run_history_title), onHistory, Modifier.fillMaxWidth())
    }
}

@Composable
private fun TypeChip(value: ActivityType, selected: ActivityType, label: String, onSelect: (ActivityType) -> Unit) {
    FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
}

@Composable
private fun PermissionRationale(
    title: String,
    body: String,
    cta: String,
    onCta: () -> Unit,
    secondaryCta: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(text = cta, onClick = onCta)
        if (secondaryCta != null && onSecondary != null) {
            SecondaryButton(text = secondaryCta, onClick = onSecondary)
        }
    }
}

// Metas em metros, com o número redondo que se mostra ao lado. As milhas não são conversões
// das métricas: são as distâncias que quem corre em milhas reconhece.
private val METAS_KM = listOf(3000 to 3, 5000 to 5, 10000 to 10)
private val METAS_MI = listOf(1609 to 1, 4828 to 3, 8047 to 5)
