package pt.antares.app.core.util

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/**
 * As imagens em ficheiros, e nunca na base de dados: são dezenas de quilobytes cada, e a
 * base ficaria enorme e lenta a copiar.
 *
 * Vivem no armazenamento privado da app — não na galeria — e por isso nenhuma outra app as
 * vê e desaparecem com a desinstalação.
 */
actual class LocalPhotoStore(
    private val context: Context,
    private val io: CoroutineDispatcher,

    // A pasta entra por parâmetro para as fotos de progresso e as dos pratos não se
    // misturarem: o [deleteAll] apaga a pasta toda, e partilhá-la faria com que limpar
    // umas apagasse as outras.
    private val pasta: String = DIR_NAME,
) {

    private fun dir(): File = File(context.filesDir, pasta).apply { mkdirs() }

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

    // Nulo é um estado que o ecrã sabe mostrar: a foto pode ter sido apagada por fora, ou
    // ter ficado num cartão que já não está lá. Registar isto encheria o último erro com
    // ficheiros que a app não controla.
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

    actual suspend fun listAll(): List<String> = withContext(io) {
        runCatching { dir().listFiles()?.map { it.absolutePath }.orEmpty() }.getOrDefault(emptyList())
    }

    actual suspend fun exists(path: String): Boolean =
        withContext(io) { runCatching { File(path).exists() }.getOrDefault(false) }

    companion object {
        const val DIR_NAME = "progress_photos"

        /** As fotos dos pratos, separadas das de progresso pela razão escrita na classe. */
        const val DIR_REFEICOES = "meal_photos"
    }
}
