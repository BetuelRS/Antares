package pt.antares.app.core.util

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberImageSharer(): (filename: String, image: ImageBitmap) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { filename, image ->
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(dir, filename)

            file.outputStream().use { out ->
                image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(intent, filename).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
