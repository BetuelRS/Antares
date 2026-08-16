package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.bodyWeightWithUnit
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.model.GoalRates
import pt.antares.app.core.model.GoalType
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.nutrition.LifeStageDrv
import pt.antares.app.core.nutrition.microLabelRes
import pt.antares.app.feature.onboarding.SelectableCard
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
internal fun WeeklyRateSection(
    goal: GoalType,
    goalRateKcal: Int,
    weightKg: Double?,
    ageYears: Int?,
    aboveSafeZone: Boolean,

    deficitSuspended: Boolean,
    onRateChange: (Double) -> Unit,
) {

    val losing = goal == GoalType.LOSE || goal == GoalType.RECOMP
    val range = if (losing) GoalRates.LOSE_RANGE_KG_WEEK else GoalRates.GAIN_RANGE_KG_WEEK
    val current = abs(NutritionCalc.weeklyKgFromKcalPerDay(goalRateKcal))
        .coerceIn(range.start, range.endInclusive)

    Text(
        stringResource(
            if (losing) Res.string.settings_rate_lose else Res.string.settings_rate_gain,
            bodyWeightWithUnit(current, rememberUnitSystem()),
        ),
        style = MaterialTheme.typography.titleMedium,
    )

    val steps = ((range.endInclusive - range.start) / GoalRates.STEP_KG_WEEK).roundToInt() - 1
    Slider(
        value = current.toFloat(),
        onValueChange = { onRateChange(it.toDouble()) },
        valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
        steps = steps,
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        if (deficitSuspended) {
            stringResource(Res.string.settings_rate_suspended)
        } else {
            stringResource(
                if (losing) Res.string.settings_rate_deficit else Res.string.settings_rate_surplus,
                abs(goalRateKcal),
            )
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    weightKg?.takeIf { goal != GoalType.RECOMP }?.let { w ->
        val safe = if (losing) {
            NutritionCalc.safeWeeklyLossKg(w, ageYears)
        } else {
            NutritionCalc.safeWeeklyGainKg(w)
        }
        Text(
            stringResource(
                Res.string.settings_rate_safe_zone,
                bodyWeightWithUnit(safe.start, rememberUnitSystem()),
                bodyWeightWithUnit(safe.endInclusive, rememberUnitSystem()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (aboveSafeZone) {
            Text(
                stringResource(
                    if (losing) Res.string.settings_rate_too_fast_lose
                    else Res.string.settings_rate_too_fast_gain,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
internal fun LifeStageSection(current: LifeStage?, viewModel: ProfileSettingsViewModel) {
    SectionHeader(title = stringResource(Res.string.settings_section_lifestage))
    Text(
        stringResource(Res.string.settings_lifestage_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val opcoes = listOf(
        LifeStage.NONE to Res.string.settings_lifestage_none,
        LifeStage.PREGNANCY to Res.string.settings_lifestage_pregnancy,
        LifeStage.LACTATION to Res.string.settings_lifestage_lactation,
        LifeStage.POSTMENOPAUSAL to Res.string.settings_lifestage_postmenopausal,
    )
    opcoes.forEach { (stage, label) ->
        SelectableCard(
            title = stringResource(label),

            selected = (current ?: LifeStage.NONE) == stage,
            onClick = { viewModel.setLifeStage(stage) },
        )
    }

    val ajustadas = LifeStageDrv.adjustments(current)
    if (ajustadas.isNotEmpty()) {

        val nomes = ajustadas.map { stringResource(microLabelRes(it.key)) }

        Text(
            stringResource(Res.string.settings_lifestage_changed, nomes.joinToString(", ")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(Res.string.settings_lifestage_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
