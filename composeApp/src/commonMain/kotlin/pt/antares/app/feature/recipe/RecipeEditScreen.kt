package pt.antares.app.feature.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.portionUnitLabel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.feature.fooddata.paraCampo
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.rememberApagarComDesfazer
import pt.antares.app.core.designsystem.components.ConfirmDialog
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun RecipeEditScreen(
    recipeId: String?,
    onAddIngredient: (String) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecipeEditViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()
    var confirmarApagar by remember { mutableStateOf(false) }
    val apagar = rememberApagarComDesfazer()

    LaunchedEffect(recipeId) { viewModel.start(recipeId) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(if (recipeId == null) Res.string.recipe_new else Res.string.recipe_edit),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text(stringResource(Res.string.recipe_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                )
            }
            item {
                OutlinedTextField(
                    value = state.yieldText,
                    onValueChange = viewModel::setYield,
                    label = { Text(stringResource(Res.string.recipe_yield)) },
                    supportingText = { Text(stringResource(Res.string.recipe_yield_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = state.servingsText,
                    onValueChange = viewModel::setServings,
                    label = { Text(stringResource(Res.string.recipe_servings)) },
                    supportingText = {
                        // Com as doses escritas, diz-se já quanto pesa uma: é o número que
                        // torna «uma dose» registável sem pesar o prato.
                        val porDose = state.gramsPerServing
                        Text(
                            if (porDose == null) {
                                stringResource(Res.string.recipe_servings_hint)
                            } else {
                                stringResource(Res.string.recipe_serving_weight, porDose.roundToInt())
                            },
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.recipe_per100), style = MaterialTheme.typography.titleSmall)
                    val m = macroInitials()
                    Text(
                        "${state.nutrition.kcalPer100} ${stringResource(Res.string.common_kcal)} · " +
                            "${m.p} ${fmtG(state.nutrition.proteinPer100)} · " +
                            "${m.c} ${fmtG(state.nutrition.carbsPer100)} · " +
                            "${m.f} ${fmtG(state.nutrition.fatPer100)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SecondaryButton(
                    text = stringResource(Res.string.recipe_add_ingredient),
                    onClick = { viewModel.ensureRecipeThen(onAddIngredient) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.rows.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.recipe_empty_ingredients),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(state.rows, key = { it.ingredient.id }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    val food = row.food
                    Text(
                        food?.let { it.namePt.ifBlank { it.nameEn } } ?: row.ingredient.foodId,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                    )
                    // O ingrediente é sólido por definição — uma receita mede-se em massa —,
                    // por isso vai sempre pela unidade de massa e nunca pela de volume.
                    OutlinedTextField(
                        value = paraCampo(row.ingredient.grams, unidades),
                        onValueChange = { viewModel.updateGrams(row.ingredient, it, unidades) },
                        label = { Text(stringResource(portionUnitLabel(unidades, liquid = false))) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(96.dp),
                    )
                    IconButton(
                        onClick = {
                            apagar(
                                { viewModel.removeIngredient(row.ingredient) },
                                { viewModel.restoreIngredient(row.ingredient.id) },
                            )
                        },
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.common_delete))
                    }
                }
            }

            item {
                PrimaryButton(
                    text = stringResource(Res.string.common_save),
                    onClick = viewModel::save,
                    enabled = state.valid,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                )
            }

            if (recipeId != null) {
                item {
                    SecondaryButton(
                        text = stringResource(Res.string.recipe_delete),
                        onClick = { confirmarApagar = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl),
                    )
                }
            }
        }
    }

    if (confirmarApagar) {
        ConfirmDialog(
            title = stringResource(Res.string.recipe_delete_title),
            message = stringResource(Res.string.recipe_delete_body),
            confirmLabel = stringResource(Res.string.common_delete),
            dismissLabel = stringResource(Res.string.common_cancel),
            onConfirm = {
                confirmarApagar = false
                viewModel.delete()
            },
            onDismiss = { confirmarApagar = false },
        )
    }
}
