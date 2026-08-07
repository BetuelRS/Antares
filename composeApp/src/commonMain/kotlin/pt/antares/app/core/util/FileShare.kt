package pt.antares.app.core.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFileSharer(): (filename: String, mimeType: String, content: String) -> Unit

@Composable
expect fun rememberZipSharer(): (zipName: String, entries: Map<String, ByteArray>) -> Unit
