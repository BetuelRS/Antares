package pt.antares.app.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.ai.AiFoodItem
import pt.antares.app.core.ai.AiWarnings
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.macroInitials
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.rememberImagePicker
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.ai_analyze
import pt.antares.app.generated.resources.ai_analyzing
import pt.antares.app.generated.resources.ai_camera
import pt.antares.app.generated.resources.ai_cancel
import pt.antares.app.generated.resources.ai_check
import pt.antares.app.generated.resources.ai_confirm
import pt.antares.app.generated.resources.ai_disclaimer
import pt.antares.app.generated.resources.ai_error_generic
import pt.antares.app.generated.resources.ai_error_offline
import pt.antares.app.generated.resources.ai_gallery
import pt.antares.app.generated.resources.ai_hint
import pt.antares.app.generated.resources.ai_not_food
import pt.antares.app.generated.resources.ai_quota_banner
import pt.antares.app.generated.resources.ai_paused
import pt.antares.app.generated.resources.ai_quota_over
import pt.antares.app.generated.resources.ai_remove
import pt.antares.app.generated.resources.ai_review_hint
import pt.antares.app.generated.resources.ai_review_title
import pt.antares.app.generated.resources.ai_source_estimated
import pt.antares.app.generated.resources.ai_title_photo
import pt.antares.app.generated.resources.ai_title_text
import pt.antares.app.generated.resources.ai_total
import pt.antares.app.generated.resources.ai_trial_banner
import pt.antares.app.generated.resources.ai_trial_over
import pt.antares.app.generated.resources.ai_too_short
import pt.antares.app.generated.resources.ai_unclear_image
import pt.antares.app.generated.resources.ai_vague_item
import kotlin.math.roundToInt

enum class AiMode { TEXT, PHOTO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFoodSheet(
    mode: AiMode,
    mealSlot: MealSlot,
    epochDay: Long,
    onDismiss: () -> Unit,
    viewModel: AiViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val picker = rememberImagePicker { image ->
        viewModel.analyzePhoto(image.base64, image.mime)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.reset()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            state.usage?.let { QuotaBanner(it.remaining, it.limit, it.trial) }

            when (state.phase) {
                AiPhase.INPUT -> InputStep(
                    mode = mode,
                    text = state.text,
                    onTextChange = viewModel::onTextChange,
                    onAnalyze = viewModel::analyzeText,
                    onCamera = picker::takePhoto,
                    onGallery = picker::pickFromGallery,
                )

                AiPhase.ANALYZING -> AnalyzingStep(onCancel = viewModel::cancel)

                AiPhase.REVIEW -> ReviewStep(
                    state = state,
                    onGrams = viewModel::setGrams,
                    onRemove = viewModel::removeItem,
                    onConfirm = { viewModel.confirm(mealSlot, epochDay) },
                )

                AiPhase.ERROR -> ErrorStep(
                    error = state.error,
                    inputError = state.inputError,

                    trial = state.usage?.trial ?: true,
                    onRetry = viewModel::reset,
                )
            }

            Text(
                stringResource(Res.string.ai_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuotaBanner(remaining: Int, limit: Int, trial: Boolean) {
    Text(
        text = if (trial) {
            stringResource(Res.string.ai_trial_banner, remaining, limit)
        } else {
            stringResource(Res.string.ai_quota_banner, remaining)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InputStep(
    mode: AiMode,
    text: String,
    onTextChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    Text(
        stringResource(if (mode == AiMode.PHOTO) Res.string.ai_title_photo else Res.string.ai_title_text),
        style = MaterialTheme.typography.titleMedium,
    )

    if (mode == AiMode.TEXT) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.ai_hint)) },
            minLines = 2,
        )
        PrimaryButton(
            text = stringResource(Res.string.ai_analyze),
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank(),
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PrimaryButton(
                text = stringResource(Res.string.ai_camera),
                onClick = onCamera,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = stringResource(Res.string.ai_gallery),
                onClick = onGallery,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AnalyzingStep(onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier)
        Text(stringResource(Res.string.ai_analyzing), style = MaterialTheme.typography.bodyLarge)
    }

    TextButton(onClick = onCancel) { Text(stringResource(Res.string.ai_cancel)) }
}

@Composable
private fun ReviewStep(
    state: AiState,
    onGrams: (Int, Double) -> Unit,
    onRemove: (Int) -> Unit,
    onConfirm: () -> Unit,
) {
    if (state.notFood) {
        Text(stringResource(Res.string.ai_not_food), style = MaterialTheme.typography.bodyLarge)
        return
    }

    Text(stringResource(Res.string.ai_review_title), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(Res.string.ai_review_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (state.warnings.contains(AiWarnings.UNCLEAR_IMAGE)) {
        Text(
            stringResource(Res.string.ai_unclear_image),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (state.warnings.contains(AiWarnings.VAGUE_ITEM)) {
        Text(
            stringResource(Res.string.ai_vague_item),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }

    LazyColumn(
        modifier = Modifier.heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        itemsIndexed(state.items, key = { i, item -> "$i-${item.name}" }) { index, item ->
            ItemRow(
                item = item,
                onGrams = { onGrams(index, it) },
                onRemove = { onRemove(index) },
            )
        }
    }

    Text(
        stringResource(Res.string.ai_total, state.totalKcal),
        style = MaterialTheme.typography.titleMedium,
    )
    PrimaryButton(
        text = stringResource(Res.string.ai_confirm),
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canConfirm,
    )
}

@Composable
private fun ItemRow(item: AiFoodItem, onGrams: (Double) -> Unit, onRemove: () -> Unit) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${item.grams.roundToInt()} g · ${item.kcal} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val m = macroInitials()
                Text(
                    "${m.p} ${item.protein.roundToInt()} g · " +
                        "${m.c} ${item.carbs.roundToInt()} g · " +
                        "${m.f} ${item.fat.roundToInt()} g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                item.assumption?.let { note ->
                    Text(
                        note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                if (item.needsReview) {
                    Text(
                        text = if (item.estimated) {
                            stringResource(Res.string.ai_source_estimated)
                        } else {
                            stringResource(Res.string.ai_check)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            TextButton(onClick = { onGrams((item.grams - 10).coerceAtLeast(1.0)) }) { Text("−10") }
            TextButton(onClick = { onGrams(item.grams + 10) }) { Text("+10") }
            TextButton(onClick = onRemove) { Text(stringResource(Res.string.ai_remove)) }
        }
    }
}

@Composable
private fun ErrorStep(error: AppError?, inputError: Boolean = false, trial: Boolean, onRetry: () -> Unit) {
    val message = when {

        inputError -> stringResource(Res.string.ai_too_short)
        error == AppError.Network -> stringResource(Res.string.ai_error_offline)

        error == AppError.QuotaExceeded -> if (trial) {
            stringResource(Res.string.ai_trial_over)
        } else {
            stringResource(Res.string.ai_quota_over)
        }

        error == AppError.AiPaused -> stringResource(Res.string.ai_paused)
        else -> stringResource(Res.string.ai_error_generic)
    }
    Text(message, style = MaterialTheme.typography.bodyLarge)
    SecondaryButton(
        text = stringResource(Res.string.ai_cancel),
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
    )
}
