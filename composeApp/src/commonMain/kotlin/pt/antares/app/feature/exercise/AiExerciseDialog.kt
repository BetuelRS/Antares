package pt.antares.app.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.ai.AiWarnings
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.util.AppError
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.ai_analyze
import pt.antares.app.generated.resources.ai_analyzing
import pt.antares.app.generated.resources.ai_cancel
import pt.antares.app.generated.resources.ai_confirm
import pt.antares.app.generated.resources.ai_disclaimer
import pt.antares.app.generated.resources.ai_error_generic
import pt.antares.app.generated.resources.ai_error_offline
import pt.antares.app.generated.resources.ai_exercise_hint
import pt.antares.app.generated.resources.ai_exercise_no_duration
import pt.antares.app.generated.resources.ai_title_exercise
import pt.antares.app.generated.resources.ai_paused
import pt.antares.app.generated.resources.ai_trial_over
import kotlin.math.roundToInt

@Composable
fun AiExerciseDialog(
    state: AiExerciseState,
    onText: (String) -> Unit,
    onAnalyze: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.ai_title_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                when {
                    state.busy -> {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.ai_analyzing))
                    }

                    state.error != null -> Text(
                        text = when (state.error) {
                            AppError.Network -> stringResource(Res.string.ai_error_offline)
                            AppError.QuotaExceeded -> stringResource(Res.string.ai_trial_over)
                            AppError.AiPaused -> stringResource(Res.string.ai_paused)
                            else -> stringResource(Res.string.ai_error_generic)
                        },
                        color = MaterialTheme.colorScheme.error,
                    )

                    state.draft != null -> {
                        val d = state.draft
                        Text(d.activity, style = MaterialTheme.typography.titleMedium)
                        if (d.kcal != null) {
                            Text(
                                "${d.durationMin?.roundToInt() ?: 0} min · ${d.kcal} kcal · MET ${d.met}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        if (d.warnings.contains(AiWarnings.NO_DURATION) || d.kcal == null) {
                            Text(
                                stringResource(Res.string.ai_exercise_no_duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    else -> OutlinedTextField(
                        value = state.text,
                        onValueChange = onText,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.ai_exercise_hint)) },
                    )
                }

                Text(
                    stringResource(Res.string.ai_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (state.draft != null) {
                TextButton(onClick = onConfirm, enabled = state.canConfirm) {
                    Text(stringResource(Res.string.ai_confirm))
                }
            } else {
                TextButton(onClick = onAnalyze, enabled = state.text.isNotBlank() && !state.busy) {
                    Text(stringResource(Res.string.ai_analyze))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ai_cancel)) }
        },
    )
}
