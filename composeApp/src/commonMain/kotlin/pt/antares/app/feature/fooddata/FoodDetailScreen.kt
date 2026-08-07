package pt.antares.app.feature.fooddata

import androidx.compose.foundation.layout.Arrangement
import pt.antares.app.core.nutrition.FoodProvenance
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.nutrition.MicroGap
import pt.antares.app.core.nutrition.NutritionFactsCard
import pt.antares.app.core.nutrition.provenanceRes
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.model.MealSlot
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodDetailScreen(
    foodId: String,
    slot: MealSlot,
    epochDay: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: FoodDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(foodId) { viewModel.load(foodId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        topBar = {
            AntaresTopBar(title = stringResource(Res.string.food_portion_title), onBack = onBack)
        },
    ) { padding ->
        val food = state.food
        if (state.loading || food == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(food.namePt.ifBlank { food.nameEn }, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${food.kcal} ${stringResource(Res.string.common_kcal)} / 100 g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        if (food.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = stringResource(Res.string.food_favorite),
                        tint = if (food.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!food.verified) {
                Text(
                    stringResource(Res.string.food_estimated_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val unit = stringResource(if (food.isLiquid) Res.string.common_ml else Res.string.common_grams_short)
            OutlinedTextField(
                value = state.quantityText,
                onValueChange = viewModel::setQuantity,
                label = {
                    Text(stringResource(if (food.isLiquid) Res.string.food_quantity_ml else Res.string.food_quantity_g))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            state.usualG?.let { usual ->
                Text(
                    stringResource(Res.string.food_usual_amount, usual.roundToInt(), unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {

                food.lastAmountG?.let { last ->
                    val duplicate = last.roundToInt() == 100 ||
                        last.roundToInt() == 15 ||
                        food.servingGrams?.roundToInt() == last.roundToInt()
                    if (!duplicate) {
                        AssistChip(
                            onClick = { viewModel.setQuick(last) },
                            label = { Text(stringResource(Res.string.food_last_amount, last.roundToInt())) },
                        )
                    }
                }
                AssistChip(onClick = { viewModel.setQuick(100.0) }, label = { Text("100 $unit") })
                food.servingGrams?.let { grams ->
                    AssistChip(
                        onClick = { viewModel.setQuick(grams) },
                        label = { Text("${food.servingName ?: stringResource(Res.string.food_serving)} (${grams.roundToInt()} $unit)") },
                    )
                }
                AssistChip(onClick = { viewModel.setQuick(15.0) }, label = { Text(stringResource(Res.string.food_tbsp)) })
            }

            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${state.previewKcal} ${stringResource(Res.string.common_kcal)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Spacing.xs))
                val m = macroInitials()
                Text(
                    "${m.p} ${fmtG(state.previewP)} g · ${m.c} ${fmtG(state.previewC)} g · ${m.f} ${fmtG(state.previewF)} g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            NutritionPanel(state)

            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = { viewModel.save(slot, epochDay) },
                enabled = state.quantityGrams != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NutritionPanel(state: PortionState) {
    val food = state.food ?: return
    val breakdown = state.breakdown
    val hasMicros = breakdown?.hasMicronutrients == true
    NutritionFactsCard(
        breakdown = breakdown,
        gap = MicroGap.of(food.source, hasMicros),
        expandKey = food.id,
        modifier = Modifier.fillMaxWidth(),
        source = provenanceRes(FoodProvenance.of(food.source, food.id), hasMicros),
    )
}
