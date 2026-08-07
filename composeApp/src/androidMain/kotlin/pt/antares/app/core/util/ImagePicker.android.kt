package pt.antares.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

actual class ImagePickerController(
    private val onTakePhoto: () -> Unit,
    private val onPickGallery: () -> Unit,
) {
    actual fun takePhoto() = onTakePhoto()
    actual fun pickFromGallery() = onPickGallery()
}

@Composable
actual fun rememberImagePicker(
    maxDimen: Int,
    onImage: (PickedImage) -> Unit,
): ImagePickerController {
    val context = LocalContext.current

    val photoFile = remember { tempPhotoFile(context) }
    val photoUri = remember(photoFile) {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (!ok) return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(photoUri)?.use { stream ->
            compressToBase64(stream.readBytes(), maxDimen)?.let(onImage)
        }
        photoFile.delete()
    }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            compressToBase64(stream.readBytes(), maxDimen)?.let(onImage)
        }
    }

    return remember(camera, gallery) {
        ImagePickerController(
            onTakePhoto = { camera.launch(photoUri) },
            onPickGallery = {
                gallery.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

private fun tempPhotoFile(context: Context): File {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    return File(dir, "ai-photo.jpg")
}

internal fun compressToBase64(bytes: ByteArray, maxDimen: Int = MAX_IMAGE_DIMEN): PickedImage? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val longest = max(bounds.outWidth, bounds.outHeight)
    if (longest <= 0) return null

    var sample = 1
    while (longest / sample > maxDimen * 2) sample *= 2

    val decoded = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return null

    val scaled = scaleToMax(decoded, maxDimen)

    var quality = 85
    var jpeg: ByteArray
    do {
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        jpeg = out.toByteArray()
        quality -= 15
    } while (jpeg.size > MAX_IMAGE_BYTES && quality >= 40)

    scaled.recycle()
    if (decoded !== scaled) decoded.recycle()

    return PickedImage(
        base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP),
        mime = "image/jpeg",
    )
}

private fun scaleToMax(src: Bitmap, maxDimen: Int): Bitmap {
    val longest = max(src.width, src.height)
    if (longest <= maxDimen) return src
    val ratio = maxDimen.toFloat() / longest
    return Bitmap.createScaledBitmap(
        src,
        (src.width * ratio).toInt().coerceAtLeast(1),
        (src.height * ratio).toInt().coerceAtLeast(1),
        true,
    )
}
