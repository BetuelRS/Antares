package pt.antares.app.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.Sex
import pt.antares.app.core.model.LifeStage
import pt.antares.app.core.nutrition.EfsaReference
import pt.antares.app.core.nutrition.LogNutrition
import pt.antares.app.core.nutrition.MicroGap
import pt.antares.app.core.nutrition.NutritionFactsCard
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailSheet(
    slot: MealSlot,
    slotName: String,
    logs: List<FoodLogEntity>,
    reference: EfsaReference?,
    lifeStage: LifeStage? = null,
    sex: Sex,
    onDismiss: () -> Unit,
) {
    val nutrition = remember(logs, reference, sex) {
        LogNutrition.ofLogs(logs.map { it.microsPer100Json to it.quantityGrams }, reference, sex, lifeStage)
    }
    val kcal = logs.sumOf { it.kcalSnapshot }
    val protein = logs.sumOf { it.proteinSnapshot }
    val carbs = logs.sumOf { it.carbsSnapshot }
    val fat = logs.sumOf { it.fatSnapshot }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(Res.string.meal_detail_title, slotName),
                style = MaterialTheme.typography.titleLarge,
            )

            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "$kcal ${stringResource(Res.string.common_kcal)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val m = macroInitials()
                Text(
                    "${m.p} ${fmtG(protein)} g · ${m.c} ${fmtG(carbs)} g · ${m.f} ${fmtG(fat)} g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            NutritionFactsCard(
                breakdown = nutrition.breakdown,
                gap = if (nutrition.breakdown?.hasMicronutrients == true) {
                    MicroGap.NONE
                } else {
                    MicroGap.NOT_MEASURED
                },
                expandKey = "meal-$slot",
                modifier = Modifier.fillMaxWidth(),
                source = Res.string.meal_origin,
            )
        }
    }
}
