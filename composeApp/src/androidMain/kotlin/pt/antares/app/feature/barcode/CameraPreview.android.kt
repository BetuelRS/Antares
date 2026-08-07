package pt.antares.app.feature.barcode

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalGetImage::class)
@Composable
actual fun BarcodeCameraPreview(
    torchEnabled: Boolean,
    onBarcode: (String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val onBarcodeLatest = rememberUpdatedState(onBarcode)

    val controller = remember { LifecycleCameraController(context) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,

                    Barcode.FORMAT_ITF,
                )
                .build(),
        )
    }

    DisposableEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        controller.setImageAnalysisAnalyzer(
            executor,
            ImageAnalysis.Analyzer { proxy ->
                val media = proxy.image
                if (media == null) {
                    proxy.close()
                    return@Analyzer
                }
                val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(input)
                    .addOnSuccessListener { codes ->

                        codes
                            .filter { pt.antares.app.core.fooddata.Barcode.normalize(it.rawValue) != null }
                            .maxByOrNull { it.boundingBox?.let { b -> b.width() * b.height() } ?: 0 }
                            ?.rawValue
                            ?.let { onBarcodeLatest.value(it) }
                    }
                    .addOnCompleteListener { proxy.close() }
            },
        )
        context.findActivity()?.let { controller.bindToLifecycle(it) }
        onDispose {
            controller.unbind()
            scanner.close()
        }
    }

    LaunchedEffect(torchEnabled) { controller.enableTorch(torchEnabled) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                this.controller = controller
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
    )
}

private fun Context.findActivity(): ComponentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
