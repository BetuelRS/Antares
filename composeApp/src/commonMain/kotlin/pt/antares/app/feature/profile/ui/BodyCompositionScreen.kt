package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.bodyWeightWithUnit
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.feature.onboarding.SelectableCard
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun BodyCompositionScreen(
    onBack: () -> Unit,
    viewModel: BodyCompositionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.bodycomp_title), onBack = onBack) },
    ) { padding ->
        if (state.loading || state.profile == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (state.weightKg == null) {

                Text(
                    stringResource(Res.string.bodycomp_no_weight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                return@Column
            }

            Text(
                stringResource(Res.string.bodycomp_question),
                style = MaterialTheme.typography.titleMedium,
            )

            SelectableCard(
                title = stringResource(Res.string.bodycomp_method_known),
                subtitle = stringResource(Res.string.bodycomp_method_known_desc),
                selected = state.method == BodyFatMethod.KNOWN,
                onClick = { viewModel.setMethod(BodyFatMethod.KNOWN) },
            )
            SelectableCard(
                title = stringResource(Res.string.bodycomp_method_measurements),
                subtitle = stringResource(Res.string.bodycomp_method_measurements_desc),
                selected = state.method == BodyFatMethod.MEASUREMENTS,
                onClick = { viewModel.setMethod(BodyFatMethod.MEASUREMENTS) },
            )
            SelectableCard(
                title = stringResource(Res.string.bodycomp_method_bmi),
                subtitle = stringResource(Res.string.bodycomp_method_bmi_desc),
                selected = state.method == BodyFatMethod.BMI,
                onClick = { viewModel.setMethod(BodyFatMethod.BMI) },
            )
            SelectableCard(
                title = stringResource(Res.string.bodycomp_method_none),
                subtitle = stringResource(Res.string.bodycomp_method_none_desc),
                selected = state.method == BodyFatMethod.NONE,
                onClick = { viewModel.setMethod(BodyFatMethod.NONE) },
            )

            when (state.method) {
                BodyFatMethod.KNOWN -> NumberField(
                    label = stringResource(Res.string.bodycomp_pct_label),
                    value = state.knownPct,
                    onChange = viewModel::setKnownPct,
                )
                BodyFatMethod.MEASUREMENTS -> {
                    Text(
                        stringResource(Res.string.bodycomp_measure_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NumberField(
                        label = stringResource(Res.string.bodycomp_waist),
                        value = state.waist,
                        onChange = viewModel::setWaist,
                    )
                    NumberField(
                        label = stringResource(Res.string.bodycomp_neck),
                        value = state.neck,
                        onChange = viewModel::setNeck,
                    )
                    if (state.needsHip) {
                        NumberField(
                            label = stringResource(Res.string.bodycomp_hip),
                            value = state.hip,
                            onChange = viewModel::setHip,
                        )
                        Text(
                            stringResource(Res.string.bodycomp_hip_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                BodyFatMethod.BMI, BodyFatMethod.NONE -> Unit
            }

            SectionHeader(title = stringResource(Res.string.bodycomp_extra_title))
            Text(
                stringResource(Res.string.bodycomp_extra_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NumberField(
                label = stringResource(Res.string.bodycomp_arm),
                value = state.arm,
                onChange = viewModel::setArm,
            )
            NumberField(
                label = stringResource(Res.string.bodycomp_thigh),
                value = state.thigh,
                onChange = viewModel::setThigh,
            )
            NumberField(
                label = stringResource(Res.string.bodycomp_chest),
                value = state.chest,
                onChange = viewModel::setChest,
            )

            PreviewCard(state, viewModel)

            PrimaryButton(
                text = stringResource(Res.string.bodycomp_save),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )
            if (state.saved) {
                Text(
                    stringResource(Res.string.bodycomp_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(state: BodyCompositionState, viewModel: BodyCompositionViewModel) {
    if (state.method == BodyFatMethod.NONE) return

    val stats = viewModel.preview()
    if (stats?.bodyFatPct == null) {

        val started = when (state.method) {
            BodyFatMethod.KNOWN -> state.knownPct.isNotBlank()
            BodyFatMethod.MEASUREMENTS -> state.waist.isNotBlank() && state.neck.isNotBlank()
            else -> true
        }
        if (started) {
            Text(
                stringResource(Res.string.bodycomp_invalid),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.profile_health_body_fat, fmtG(stats.bodyFatPct)),
            style = MaterialTheme.typography.titleMedium,
        )
        stats.leanMassKg?.let {
            Text(
                stringResource(
                    Res.string.profile_health_lean_mass,
                    bodyWeightWithUnit(it, rememberUnitSystem()),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        stats.fatMassKg?.let {
            Text(
                stringResource(
                    Res.string.bodycomp_fat_mass,
                    bodyWeightWithUnit(it, rememberUnitSystem()),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            stringResource(
                when (state.method) {
                    BodyFatMethod.KNOWN -> Res.string.bodycomp_known_note
                    BodyFatMethod.MEASUREMENTS -> Res.string.bodycomp_navy_note
                    else -> Res.string.bodycomp_bmi_note
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        stats.waistToHeight?.let { ratio ->
            val risk = stats.waistRisk ?: return@let
            Text(
                stringResource(
                    Res.string.profile_health_waist_ratio,
                    fmtG(ratio),
                    stringResource(
                        when (risk) {
                            pt.antares.app.core.calc.WaistRisk.HEALTHY -> Res.string.profile_waist_healthy
                            pt.antares.app.core.calc.WaistRisk.INCREASED -> Res.string.profile_waist_increased
                            pt.antares.app.core.calc.WaistRisk.HIGH -> Res.string.profile_waist_high
                        },
                    ),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
