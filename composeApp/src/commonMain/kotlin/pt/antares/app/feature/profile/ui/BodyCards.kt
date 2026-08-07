package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.calc.BmiCategory
import pt.antares.app.core.calc.BodyStats
import pt.antares.app.core.calc.MeasurementProgress
import pt.antares.app.core.calc.WaistRisk
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.BmiScale
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.Sparkline
import pt.antares.app.core.designsystem.components.SplitRow
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun WeightCard(state: HealthProfileState, onWeightHistory: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onWeightHistory)) {
        val weight = state.latestWeightKg
        if (weight == null) {
            Text(
                stringResource(Res.string.profile_health_no_weight),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AntaresCard
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${fmtG(weight)} kg", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f, fill = false).padding(end = Spacing.md))
            state.weeklyRateKg?.let { rate ->

                val arrow = if (rate < 0) "↓" else "↑"
                Text(
                    "$arrow ${fmtG(abs(rate))} ${stringResource(Res.string.profile_health_kg_per_week)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.trendWeightKg?.let { trend ->
            Text(
                stringResource(Res.string.profile_health_trend, fmtG(trend)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.weightSeries.size >= HealthProfileViewModel.MIN_POINTS_FOR_CHART) {
            Sparkline(
                primary = state.weightSeries,
                secondary = state.trendSeries,
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
internal fun BmiCard(body: BodyStats) {
    val bmi = body.bmi ?: return
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        SplitRow(
            leading = {
                Text(
                    "${stringResource(Res.string.profile_health_bmi)} ${fmtG(bmi)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            trailing = {
                body.bmiCategory?.let {
                    Text(
                        stringResource(it.label()),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )

        BmiScale(bmi = bmi, modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm))
        body.healthyWeightRangeKg?.let { range ->
            Text(
                stringResource(
                    Res.string.profile_health_healthy_range,
                    fmtG(range.start),
                    fmtG(range.endInclusive),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            stringResource(Res.string.profile_health_bmi_caveat),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        body.waistToHeight?.let { ratio ->
            val risk = body.waistRisk ?: return@let
            Text(
                stringResource(
                    Res.string.profile_health_waist_ratio,
                    fmtG(ratio),
                    stringResource(risk.label()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AddCompositionCard(onClick: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Text(
            stringResource(Res.string.profile_health_add_composition),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(Res.string.profile_health_add_composition_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun CompositionCard(body: BodyStats, onClick: () -> Unit) {
    val fat = body.bodyFatPct ?: return
    AntaresCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        SplitRow(
            leading = {
                Text(
                    stringResource(Res.string.profile_health_body_fat, fmtG(fat)),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            trailing = {
                body.leanMassKg?.let {
                    Text(
                        stringResource(Res.string.profile_health_lean_mass, fmtG(it)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )

        body.bodyFatSource?.let {
            Text(
                stringResource(it.label()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        body.ffmi?.let {
            Text(
                stringResource(Res.string.profile_health_ffmi, fmtG(it)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Text(
                stringResource(Res.string.profile_health_ffmi_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun BmiCategory.label(): StringResource = when (this) {
    BmiCategory.UNDERWEIGHT -> Res.string.profile_bmi_underweight
    BmiCategory.HEALTHY -> Res.string.profile_bmi_healthy
    BmiCategory.OVERWEIGHT -> Res.string.profile_bmi_overweight
    BmiCategory.OBESE -> Res.string.profile_bmi_obese
}

private fun WaistRisk.label(): StringResource = when (this) {
    WaistRisk.HEALTHY -> Res.string.profile_waist_healthy
    WaistRisk.INCREASED -> Res.string.profile_waist_increased
    WaistRisk.HIGH -> Res.string.profile_waist_high
}

private fun BodyFatSource.label(): StringResource = when (this) {
    BodyFatSource.MEASURED -> Res.string.profile_bf_measured
    BodyFatSource.NAVY -> Res.string.profile_bf_navy
    BodyFatSource.BMI -> Res.string.profile_bf_bmi
}
