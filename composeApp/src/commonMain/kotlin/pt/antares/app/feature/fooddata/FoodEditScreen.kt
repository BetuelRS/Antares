package pt.antares.app.feature.fooddata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.PickedImage
import pt.antares.app.core.util.MAX_LABEL_DIMEN
import pt.antares.app.core.util.rememberImagePicker
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun FoodEditScreen(
    foodId: String?,
    barcode: String?,
    initialName: String? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    // Nulo quando o ecrã foi aberto de um sítio que não sabe a refeição nem o dia. Nesse
    // caso o aviso de duplicados continua a aparecer, só não tem para onde levar.
    onUseExisting: ((String) -> Unit)? = null,
    viewModel: FoodEditViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(foodId, barcode) { viewModel.start(foodId, barcode, initialName) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(
                    if (foodId == null) Res.string.food_edit_title_new else Res.string.food_edit_title_edit,
                ),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            NumField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = stringResource(Res.string.food_edit_name),
                numeric = false,
            )

            if (state.duplicados.isNotEmpty()) {
                DuplicadosCard(state.duplicados, onUseExisting)
            }

            LabelScanRow(
                reading = state.readingLabel,
                incomplete = state.labelIncomplete,
                needsCheck = state.labelNeedsCheck,
                error = state.labelError,
                onImage = { img -> viewModel.readLabel(img.base64, img.mime) },
            )

            SectionHeader(title = stringResource(Res.string.food_edit_per_100g))
            NumField(state.kcal, viewModel::setKcal, stringResource(Res.string.food_edit_kcal))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                NumField(state.protein, viewModel::setProtein, stringResource(Res.string.food_edit_protein), Modifier.weight(1f))
                NumField(state.carbs, viewModel::setCarbs, stringResource(Res.string.food_edit_carbs), Modifier.weight(1f))
                NumField(state.fat, viewModel::setFat, stringResource(Res.string.food_edit_fat), Modifier.weight(1f))
            }

            if (state.kcalMismatch) {
                Text(
                    stringResource(Res.string.food_edit_kcal_mismatch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            SectionHeader(title = stringResource(Res.string.food_edit_section_optional))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                NumField(state.sugars, viewModel::setSugars, stringResource(Res.string.food_edit_sugars), Modifier.weight(1f))
                NumField(state.satFat, viewModel::setSatFat, stringResource(Res.string.food_edit_satfat), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                NumField(state.fiber, viewModel::setFiber, stringResource(Res.string.food_edit_fiber), Modifier.weight(1f))
                NumField(state.sodium, viewModel::setSodium, stringResource(Res.string.food_edit_sodium), Modifier.weight(1f))
            }

            SectionHeader(title = stringResource(Res.string.food_edit_section_serving))
            NumField(
                value = state.servingName,
                onValueChange = viewModel::setServingName,
                label = stringResource(Res.string.food_edit_serving_name),
                numeric = false,
            )
            NumField(state.servingGrams, viewModel::setServingGrams, stringResource(Res.string.food_edit_serving_grams))

            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = viewModel::save,
                enabled = state.valid,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
            )
        }
    }
}

/**
 * Avisa que já existe algo parecido. Não bloqueia nada: o botão de guardar continua onde
 * estava e com as mesmas condições — há bacalhaus diferentes, e quem escreveu o nome é quem
 * sabe se é o mesmo alimento.
 */
@Composable
private fun DuplicadosCard(duplicados: List<FoodEntity>, onUseExisting: ((String) -> Unit)?) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.food_edit_duplicates_title),
            style = MaterialTheme.typography.titleSmall,
        )
        duplicados.forEach { food ->
            val nome = food.namePt.ifBlank { food.nameEn }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm)
                    .then(
                        if (onUseExisting == null) Modifier
                        else Modifier.clickable(role = Role.Button) { onUseExisting(food.id) },
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(nome, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    stringResource(Res.string.food_edit_duplicates_kcal, food.kcal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LabelScanRow(
    reading: Boolean,
    incomplete: Boolean,
    needsCheck: Boolean,
    error: AppError?,
    onImage: (PickedImage) -> Unit,
) {

    val picker = rememberImagePicker(maxDimen = MAX_LABEL_DIMEN, onImage = onImage)

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.label_scan_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(Res.string.label_scan_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )

        if (reading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Spacing.sm),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(Res.string.label_scan_reading),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(top = Spacing.sm),
            ) {
                SecondaryButton(
                    text = stringResource(Res.string.ai_camera),
                    onClick = { picker.takePhoto() },
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(Res.string.ai_gallery),
                    onClick = { picker.pickFromGallery() },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val warning = when {
            error is AppError.Network -> stringResource(Res.string.ai_error_offline)
            error is AppError.QuotaExceeded -> stringResource(Res.string.ai_trial_over)
            error is AppError.AiPaused -> stringResource(Res.string.ai_paused)
            error is AppError.Unauthorized -> stringResource(Res.string.ai_error_generic)
            error != null -> stringResource(Res.string.label_scan_error)
            incomplete -> stringResource(Res.string.label_scan_incomplete)

            needsCheck -> stringResource(Res.string.label_scan_check)
            else -> null
        }
        warning?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun NumField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = if (modifier == Modifier) Modifier.fillMaxWidth() else modifier,
    )
}
