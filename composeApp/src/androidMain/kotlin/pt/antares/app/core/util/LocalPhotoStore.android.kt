package pt.antares.app.core.util

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/**
 * As fotos de progresso em ficheiros, e nunca na base de dados: são imagens de dezenas de
 * quilobytes cada, e a base ficaria enorme e lenta a copiar.
 *
 * Vivem no armazenamento privado da app — não na galeria — e por isso nenhuma outra app as
 * vê e desaparecem com a desinstalação.
 */
actual class LocalPhotoStore(
    private val context: Context,
    private val io: CoroutineDispatcher,
) {

    private fun dir(): File = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    actual suspend fun save(id: String, base64Jpeg: String): String? = withContext(io) {
        runCatching {
            val bytes = Base64.decode(base64Jpeg, Base64.DEFAULT)
            val destino = File(dir(), "$id.jpg")
            destino.writeBytes(bytes)
            destino.absolutePath
        }.getOrNull()
    }

    actual suspend fun delete(path: String) {
        withContext(io) {

            runCatching {
                // Só apaga dentro da própria pasta. O caminho vem de uma linha da base, e
                // sem esta verificação um valor adulterado — por exemplo, vindo de uma
                // cópia de segurança de fora — apagaria ficheiros noutro sítio.
                val ficheiro = File(path)
                if (ficheiro.parentFile?.absolutePath == dir().absolutePath) ficheiro.delete()
            }
        }
    }

    actual suspend fun deleteAll() {
        withContext(io) { runCatching { dir().deleteRecursively() } }
    }

    actual suspend fun readBytes(path: String): ByteArray? = withContext(io) {
        runCatching { File(path).takeIf { it.exists() }?.readBytes() }.getOrNull()
    }

    actual suspend fun writeBytes(id: String, bytes: ByteArray): String? = withContext(io) {
        runCatching {
            val destino = File(dir(), id + ".jpg")
            destino.writeBytes(bytes)
            destino.absolutePath
        }.getOrNull()
    }

    actual suspend fun exists(path: String): Boolean =
        withContext(io) { runCatching { File(path).exists() }.getOrDefault(false) }

    companion object {
        const val DIR_NAME = "progress_photos"
    }
}
