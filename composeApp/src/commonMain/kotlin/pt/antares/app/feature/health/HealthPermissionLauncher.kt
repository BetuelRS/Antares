package pt.antares.app.feature.health

import androidx.compose.runtime.Composable

@Composable
expect fun rememberHealthPermissionRequest(
    permissions: Set<String>,
    onResult: () -> Unit,
): () -> Unit
