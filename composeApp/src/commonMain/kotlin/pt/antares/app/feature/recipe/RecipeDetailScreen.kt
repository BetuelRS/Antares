package pt.antares.app.feature.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.nutrition.MicroGap
import pt.antares.app.core.nutrition.NutritionFactsCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.model.MealSlot
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun RecipeDetailScreen(
    recipeId: String,
    slot: MealSlot,
    epochDay: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(recipeId) { viewModel.load(recipeId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.recipe_log), onBack = onBack) },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@AntaresScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(state.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "${state.nutrition.kcalPer100} ${stringResource(Res.string.common_kcal)} / 100 g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.quantityText,
                onValueChange = viewModel::setQuantity,
                label = {
                    Text(
                        if (state.byServings) {
                            stringResource(Res.string.recipe_quantity_servings)
                        } else {
                            stringResource(Res.string.food_quantity_g)
                        },
                    )
                },
                // Em doses, as gramas ficam por baixo: continuam a ser elas que vão para o
                // diário, e escondê-las era pedir confiança sem dar o número.
                supportingText = {
                    state.quantityGrams?.takeIf { state.byServings }?.let {
                        Text(stringResource(Res.string.recipe_servings_grams, it.roundToInt()))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            // A troca só existe na receita que diz em quantas doses se divide. Nas outras
            // não haveria para onde trocar.
            if (state.gramsPerServing != null) {
                TextButton(onClick = viewModel::toggleByServings) {
                    Text(
                        stringResource(
                            if (state.byServings) {
                                Res.string.recipe_log_by_grams
                            } else {
                                Res.string.recipe_log_by_servings
                            },
                        ),
                    )
                }
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

            NutritionFactsCard(
                breakdown = state.breakdown,
                gap = if (state.breakdown?.hasMicronutrients == true) MicroGap.NONE else MicroGap.RECIPE_INGREDIENTS,
                expandKey = recipeId,
                modifier = Modifier.fillMaxWidth(),
                source = Res.string.nutrition_source_recipe,
            )

            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = { viewModel.save(slot, epochDay) },
                enabled = state.quantityGrams != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
