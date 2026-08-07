package pt.antares.app.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
expect fun rememberImageSharer(): (filename: String, image: ImageBitmap) -> Unit
