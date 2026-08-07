package pt.antares.app.feature.barcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface CameraPermissionState {
    val granted: Boolean
    fun request()
}

@Composable
expect fun rememberCameraPermissionState(): CameraPermissionState

@Composable
expect fun BarcodeCameraPreview(
    torchEnabled: Boolean,
    onBarcode: (String) -> Unit,
    modifier: Modifier,
)
