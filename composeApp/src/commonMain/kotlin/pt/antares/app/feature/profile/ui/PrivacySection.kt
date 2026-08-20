package pt.antares.app.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.ConfirmDialog
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.privacy.PrivacyViewModel
import pt.antares.app.feature.backup.BackupActions
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
internal fun PrivacySection(
    onCreateFood: ((String) -> Unit)? = null,
    viewModel: PrivacyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var confirming by remember { mutableStateOf(false) }
    var limparPesquisas by remember { mutableStateOf(false) }

    BackupActions(viewModel, state.busy, state.importDone)

    Text(
        stringResource(Res.string.privacy_misses_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = Spacing.md),
    )
    Text(
        stringResource(Res.string.privacy_misses_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.searchMisses.isEmpty()) {
        Text(
            stringResource(Res.string.privacy_misses_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        state.searchMisses.forEach { falha ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.privacy_misses_row, falha.query, falha.count),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                // A lista dizia o que faltava e não deixava fazer nada quanto a isso. O
                // botão abre a criação com o nome já lá, que era o passo que fazia desistir.
                onCreateFood?.let { criar ->
                    TextButton(onClick = { criar(falha.query) }) {
                        Text(stringResource(Res.string.privacy_misses_create))
                    }
                }
            }
        }
        // Apagar isto não é destrutivo para os dados, mas é irreversível para o trabalho:
        // a lista é o registo do que falta no catálogo, e quem a limpa por engano perde
        // meses de pesquisas falhadas que ninguém volta a repetir de propósito.
        SecondaryButton(
            text = stringResource(Res.string.privacy_misses_clear),
            onClick = { limparPesquisas = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Text(
        stringResource(Res.string.privacy_delete_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.md),
    )
    SecondaryButton(
        text = stringResource(Res.string.privacy_delete),
        onClick = { confirming = true },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.busy,
    )

    state.error?.let { message ->
        Text(
            stringResource(Res.string.privacy_error, message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (limparPesquisas) {
        ConfirmDialog(
            title = stringResource(Res.string.privacy_misses_clear_title),
            message = stringResource(Res.string.privacy_misses_clear_body),
            confirmLabel = stringResource(Res.string.privacy_misses_clear),
            dismissLabel = stringResource(Res.string.common_cancel),
            onConfirm = {
                limparPesquisas = false
                viewModel.clearSearchMisses()
            },
            onDismiss = { limparPesquisas = false },
        )
    }

    if (confirming) {
        ConfirmDialog(
            title = stringResource(Res.string.privacy_delete_confirm_title),

            message = stringResource(Res.string.privacy_delete_confirm_local),
            confirmLabel = stringResource(Res.string.privacy_delete_cta),
            dismissLabel = stringResource(Res.string.common_cancel),
            onConfirm = {
                confirming = false
                viewModel.deleteEverything()
            },
            onDismiss = { confirming = false },
        )
    }
}
