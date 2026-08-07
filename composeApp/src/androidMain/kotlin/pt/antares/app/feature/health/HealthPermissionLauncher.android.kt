package pt.antares.app.feature.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.health.connect.client.PermissionController

@Composable
actual fun rememberHealthPermissionRequest(
    permissions: Set<String>,
    onResult: () -> Unit,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { _ -> onResult() }

    return { launcher.launch(permissions) }
}
