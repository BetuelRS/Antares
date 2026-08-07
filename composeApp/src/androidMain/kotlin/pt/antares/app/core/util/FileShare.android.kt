package pt.antares.app.core.util

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
actual fun rememberFileSharer(): (filename: String, mimeType: String, content: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { filename, mimeType, content ->

            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(dir, filename)
            file.writeText(content)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(intent, filename).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
actual fun rememberZipSharer(): (zipName: String, entries: Map<String, ByteArray>) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { zipName, entries ->
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(dir, zipName)

            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                for ((name, content) in entries) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content)
                    zip.closeEntry()
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(intent, zipName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
