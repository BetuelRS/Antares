package pt.antares.app.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.nutrition.EfsaReference
import pt.antares.app.core.nutrition.MicroGap
import pt.antares.app.core.nutrition.LogNutrition
import pt.antares.app.core.nutrition.NutritionFactsCard
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailSheet(
    log: FoodLogEntity,
    reference: EfsaReference?,
    lifeStage: LifeStage? = null,
    sex: Sex,
    onDismiss: () -> Unit,
) {
    val nutrition = remember(log.id, reference, sex) {
        LogNutrition.of(log.microsPer100Json, log.quantityGrams, reference, sex, lifeStage)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(log.nameSnapshot, style = MaterialTheme.typography.titleLarge)
            Text(
                "${log.quantityGrams.roundToInt()} " +
                    stringResource(if (log.isLiquid) Res.string.common_ml else Res.string.common_grams_short),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${log.kcalSnapshot} ${stringResource(Res.string.common_kcal)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val m = macroInitials()
                Text(
                    "${m.p} ${fmtG(log.proteinSnapshot)} g · ${m.c} ${fmtG(log.carbsSnapshot)} g · " +
                        "${m.f} ${fmtG(log.fatSnapshot)} g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            NutritionFactsCard(
                breakdown = nutrition.breakdown,
                gap = MicroGap.ofLog(log.origin, nutrition.breakdown?.hasMicronutrients == true),
                expandKey = log.id,
                modifier = Modifier.fillMaxWidth(),
                source = originRes(log.origin),
                sourceLabel = Res.string.log_origin_label,
            )
        }
    }
}

private fun originRes(origin: LogOrigin): StringResource = when (origin) {
    LogOrigin.MANUAL -> Res.string.log_origin_manual
    LogOrigin.BARCODE -> Res.string.log_origin_barcode
    LogOrigin.AI_TEXT -> Res.string.log_origin_ai_text
    LogOrigin.AI_PHOTO -> Res.string.log_origin_ai_photo
    LogOrigin.AI_LABEL -> Res.string.log_origin_ai_label
}
