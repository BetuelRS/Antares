package pt.antares.app.core.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberBackupPicker(
    onPicked: (entries: Map<String, ByteArray>) -> Unit,
): () -> Unit
