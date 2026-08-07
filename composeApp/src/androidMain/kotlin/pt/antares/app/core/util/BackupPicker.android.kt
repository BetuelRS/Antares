package pt.antares.app.core.util

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import pt.antares.app.core.privacy.BackupFiles
import java.util.zip.ZipInputStream

@Composable
actual fun rememberBackupPicker(
    onPicked: (entries: Map<String, ByteArray>) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val entradas = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()

                if (pareceZip(bytes)) lerZip(bytes) else mapOf(BackupFiles.DATA to bytes)
            }
        }.getOrNull().orEmpty()
        onPicked(entradas)
    }

    return remember(launcher) {
        {

            launcher.launch(arrayOf("*/*"))
        }
    }
}

private fun pareceZip(bytes: ByteArray): Boolean =
    bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

private fun lerZip(bytes: ByteArray): Map<String, ByteArray> {
    val out = LinkedHashMap<String, ByteArray>()
    runCatching {
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entrada = zip.nextEntry ?: break
                if (!entrada.isDirectory) out[entrada.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
    }
    return out
}
