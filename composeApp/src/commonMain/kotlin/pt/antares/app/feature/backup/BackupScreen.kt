package pt.antares.app.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.privacy.ImportMode
import pt.antares.app.core.privacy.PrivacyViewModel
import pt.antares.app.core.util.rememberBackupPicker
import pt.antares.app.core.util.rememberZipSharer
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.backup_title
import pt.antares.app.generated.resources.backup_why
import pt.antares.app.generated.resources.privacy_error
import pt.antares.app.generated.resources.privacy_export
import pt.antares.app.generated.resources.privacy_export_desc
import pt.antares.app.generated.resources.privacy_import
import pt.antares.app.generated.resources.privacy_import_desc
import pt.antares.app.generated.resources.privacy_import_done
import pt.antares.app.generated.resources.privacy_import_merge
import pt.antares.app.generated.resources.privacy_import_merge_desc
import pt.antares.app.generated.resources.privacy_import_replace
import pt.antares.app.generated.resources.privacy_import_replace_desc
import pt.antares.app.generated.resources.privacy_import_title

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.backup_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {

            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(Res.string.backup_why),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            BackupActions(viewModel, state.busy, state.importDone)

            state.error?.let { message ->
                Text(
                    stringResource(Res.string.privacy_error, message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun BackupActions(
    viewModel: PrivacyViewModel,
    busy: Boolean,
    importDone: Int?,
) {
    val shareZip = rememberZipSharer()
    var lidas by remember { mutableStateOf<Map<String, ByteArray>?>(null) }
    val escolher = rememberBackupPicker { entries ->

        if (entries.isNotEmpty()) lidas = entries
    }

    Text(
        stringResource(Res.string.privacy_export_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PrimaryButton(
        text = stringResource(Res.string.privacy_export),
        onClick = { viewModel.exportData { name, entries -> shareZip(name, entries) } },
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
    )

    Text(
        stringResource(Res.string.privacy_import_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.sm),
    )
    SecondaryButton(
        text = stringResource(Res.string.privacy_import),
        onClick = escolher,
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
    )
    importDone?.let { quantos ->
        Text(
            stringResource(Res.string.privacy_import_done, quantos.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }

    lidas?.let { entries ->
        AlertDialog(
            onDismissRequest = { lidas = null },
            title = { Text(stringResource(Res.string.privacy_import_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(Res.string.privacy_import_merge_desc))

                    Text(
                        stringResource(Res.string.privacy_import_replace_desc),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                SecondaryButton(
                    text = stringResource(Res.string.privacy_import_merge),
                    onClick = {
                        lidas = null
                        viewModel.importBackup(entries, ImportMode.MERGE)
                    },
                )
            },
            dismissButton = {
                SecondaryButton(
                    text = stringResource(Res.string.privacy_import_replace),
                    onClick = {
                        lidas = null
                        viewModel.importBackup(entries, ImportMode.REPLACE)
                    },
                )
            },
        )
    }
}
