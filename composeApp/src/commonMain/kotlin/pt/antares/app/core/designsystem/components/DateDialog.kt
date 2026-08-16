package pt.antares.app.core.designsystem.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_cancel
import pt.antares.app.generated.resources.common_save

// Um dia em milissegundos. O `DatePicker` do Material fala em milissegundos UTC; a app fala
// em dias desde a época, e a conversão acontece só aqui.
private const val MS_POR_DIA = 86_400_000L

/**
 * Escolher um dia. Devolve dias desde a época, que é a unidade em que a app guarda datas.
 *
 * Existe por causa do ciclo, onde substitui dois botões que só sabiam marcar «hoje» — e
 * onde a mesma escolha aparece três vezes: início, fim, e a correção de um registo antigo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(
    title: String,
    initialEpochDay: Long = todayEpochDay(),
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val estado = rememberDatePickerState(initialSelectedDateMillis = initialEpochDay * MS_POR_DIA)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PrimaryButton(
                text = stringResource(Res.string.common_save),
                onClick = {
                    estado.selectedDateMillis?.let { onPick(it / MS_POR_DIA) }
                    onDismiss()
                },
            )
        },
        dismissButton = {
            SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss)
        },
    ) {
        DatePicker(state = estado, showModeToggle = false, title = { Text(title) }, headline = null)
    }
}
