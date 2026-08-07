package pt.antares.app.feature.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.health.HealthAvailability
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.health_connect_title
import pt.antares.app.generated.resources.health_import_now
import pt.antares.app.generated.resources.health_imported_nothing
import pt.antares.app.generated.resources.health_imported_sessions
import pt.antares.app.generated.resources.health_imported_skipped
import pt.antares.app.generated.resources.health_imported_weights
import pt.antares.app.generated.resources.health_intro
import pt.antares.app.generated.resources.health_linked
import pt.antares.app.generated.resources.health_link_button
import pt.antares.app.generated.resources.health_not_supported
import pt.antares.app.generated.resources.health_update_required
import pt.antares.app.generated.resources.health_what_we_read

@Composable
fun HealthPermissionsScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    val request = rememberHealthPermissionRequest(
        permissions = viewModel.permissions,
        onResult = viewModel::refresh,
    )

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.health_connect_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))

                state.availability == HealthAvailability.NOT_SUPPORTED ->
                    Text(stringResource(Res.string.health_not_supported), style = MaterialTheme.typography.bodyLarge)

                state.availability == HealthAvailability.PROVIDER_UPDATE_REQUIRED ->
                    Text(stringResource(Res.string.health_update_required), style = MaterialTheme.typography.bodyLarge)

                else -> Connected(state, viewModel, request)
            }
        }
    }
}

@Composable
private fun Connected(
    state: HealthState,
    viewModel: HealthViewModel,
    request: () -> Unit,
) {
    Text(stringResource(Res.string.health_intro), style = MaterialTheme.typography.bodyLarge)

    AntaresCard {
        Text(stringResource(Res.string.health_what_we_read), style = MaterialTheme.typography.bodyMedium)
    }

    if (!state.granted) {
        PrimaryButton(
            text = stringResource(Res.string.health_link_button),
            onClick = request,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    Text(stringResource(Res.string.health_linked), style = MaterialTheme.typography.bodyMedium)

    val last = state.lastImport
    if (last != null && !state.importing) {
        val message = when {
            last.isEmpty -> stringResource(Res.string.health_imported_nothing)
            else -> buildString {
                if (last.weights > 0) append(stringResource(Res.string.health_imported_weights, last.weights))
                if (last.sessions > 0) {
                    if (isNotEmpty()) append(" · ")
                    append(stringResource(Res.string.health_imported_sessions, last.sessions))
                }
                if (last.skippedDuplicates > 0) {
                    if (isNotEmpty()) append(" · ")
                    append(stringResource(Res.string.health_imported_skipped, last.skippedDuplicates))
                }
            }
        }
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }

    SecondaryButton(
        text = stringResource(Res.string.health_import_now),
        onClick = viewModel::importNow,
        enabled = !state.importing,
        modifier = Modifier.fillMaxWidth(),
    )
}
