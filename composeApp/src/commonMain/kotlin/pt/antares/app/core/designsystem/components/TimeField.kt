package pt.antares.app.core.designsystem.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.util.MINUTES_PER_HOUR
import pt.antares.app.core.util.formatMinuteOfDay
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_cancel
import pt.antares.app.generated.resources.common_save

/**
 * Uma hora do dia que se toca para mudar.
 *
 * Existe porque as definições passaram a ter três: o início e o fim do silêncio, e a hora da
 * pesagem. Escritas à mão seriam três diálogos iguais, e o terceiro divergiria dos outros
 * dois no primeiro acerto.
 *
 * A hora vive em minutos desde a meia-noite — é a unidade em que a app guarda horas em todo
 * o lado, e a que não arrasta fuso horário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    minuteOfDay: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }

    SplitRow(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { aberto = true }
            .padding(vertical = Spacing.sm),
        leading = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        trailing = {
            Text(
                formatMinuteOfDay(minuteOfDay),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
    )

    if (aberto) {
        val estado = rememberTimePickerState(
            initialHour = minuteOfDay / MINUTES_PER_HOUR,
            initialMinute = minuteOfDay % MINUTES_PER_HOUR,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { aberto = false },
            title = { Text(label) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(Res.string.common_save),
                    onClick = {
                        onPick(estado.hour * MINUTES_PER_HOUR + estado.minute)
                        aberto = false
                    },
                )
            },
            dismissButton = {
                SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = { aberto = false })
            },
        )
    }
}
