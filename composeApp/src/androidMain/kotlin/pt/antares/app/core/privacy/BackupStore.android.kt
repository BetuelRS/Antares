package pt.antares.app.core.privacy

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Dois caminhos para a mesma pasta, porque o Android mudou a meio.
 *
 * Do 10 para cima escreve-se pelo MediaStore, que dá acesso a `Documentos/` sem permissão
 * nenhuma e só deixa mexer nos ficheiros que a própria app criou. Do 9 para baixo não havia
 * MediaStore para ficheiros que não fossem media, e o caminho é direto — com a permissão de
 * escrita que o manifesto pede só até essa versão.
 *
 * **O que a app vê aqui não é o que está na pasta.** O MediaStore só deixa uma app ver e
 * apagar os ficheiros de que é dona, e a desinstalação apaga essa marca de dono — verificado
 * no emulador: depois de reinstalar, a linha do ficheiro fica com `owner_package_name` a
 * nulo. O ficheiro continua lá e continua a poder ser aberto pelo seletor, que é a promessa
 * que interessa; o que se perde é poder listá-lo e apagá-lo. Quem reinstalar começa uma série
 * nova e as antigas ficam na pasta até serem apagadas à mão.
 *
 * Não se resolve com permissões: um ZIP não é media, e por isso nem o `READ_MEDIA_*` o
 * mostraria. O que mostraria era o `MANAGE_EXTERNAL_STORAGE`, que a Play Store não aceita
 * para isto — e que daria à app acesso ao armazenamento inteiro para gerir cinco ficheiros.
 */
actual class BackupStore(
    private val context: Context,
    private val io: CoroutineDispatcher,
) {

    actual fun describe(): String = "$DOCUMENTOS/$PASTA"

    actual fun canWrite(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }

    actual suspend fun write(name: String, entries: Map<String, ByteArray>): String? =
        withContext(io) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    escreverPeloMediaStore(name, entries)
                } else {
                    escreverNoCaminho(name, entries)
                }
            }.getOrNull()
        }

    actual suspend fun list(): List<String> = withContext(io) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                listarPeloMediaStore().map { it.first }
            } else {
                pastaLegada().listFiles().orEmpty().map { it.name }
            }
        }.getOrElse { emptyList() }
            .filter { it.startsWith(PREFIXO) && it.endsWith(".zip") }
            .sorted()
    }

    actual suspend fun delete(name: String) {
        withContext(io) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    listarPeloMediaStore().firstOrNull { it.first == name }?.let { (_, id) ->
                        context.contentResolver.delete(
                            android.content.ContentUris.withAppendedId(colecao(), id),
                            null,
                            null,
                        )
                    }
                } else {
                    File(pastaLegada(), name).delete()
                }
            }
        }
    }

    private fun escreverPeloMediaStore(name: String, entries: Map<String, ByteArray>): String? {
        val valores = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$DOCUMENTOS/$PASTA")
            // Enquanto está a 1 o ficheiro não aparece a mais ninguém. Uma cópia
            // interrompida a meio nunca chega a ser vista como uma cópia inteira.
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(colecao(), valores) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { saida ->
            zipar(saida, entries)
        } ?: return null
        context.contentResolver.update(
            uri,
            ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
            null,
            null,
        )
        // O nome de volta do MediaStore e não o nome pedido: se já lá estivesse um ficheiro
        // com este nome, ele grava «… (1).zip» sem avisar, e devolver o nome pedido punha o
        // cartão a nomear um ficheiro que não existe.
        return nomeGravado(uri) ?: name
    }

    private fun nomeGravado(uri: android.net.Uri): String? =
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { if (it.moveToFirst()) it.getString(0) else null }

    private fun escreverNoCaminho(name: String, entries: Map<String, ByteArray>): String? {
        if (!canWrite()) return null
        val destino = File(pastaLegada().apply { mkdirs() }, name)
        destino.outputStream().buffered().use { saida -> zipar(saida, entries) }
        return name
    }

    private fun zipar(saida: java.io.OutputStream, entries: Map<String, ByteArray>) {
        ZipOutputStream(saida.buffered()).use { zip ->
            for ((nome, conteudo) in entries) {
                zip.putNextEntry(ZipEntry(nome))
                zip.write(conteudo)
                zip.closeEntry()
            }
        }
    }

    // Nome e identificador de cada cópia. O identificador é preciso para apagar: o
    // MediaStore só aceita apagar por URI, e não por nome.
    private fun listarPeloMediaStore(): List<Pair<String, Long>> {
        val colunas = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns._ID)
        val cursor = context.contentResolver.query(
            colecao(),
            colunas,
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
            arrayOf("$DOCUMENTOS/$PASTA%"),
            null,
        ) ?: return emptyList()
        return cursor.use {
            buildList {
                while (it.moveToNext()) add(it.getString(0) to it.getLong(1))
            }
        }
    }

    private fun colecao() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

    @Suppress("DEPRECATION")
    private fun pastaLegada(): File =
        File(Environment.getExternalStoragePublicDirectory(DOCUMENTOS), PASTA)

    private companion object {
        // Escrito à letra e não tirado do Environment: o MediaStore compara o
        // RELATIVE_PATH como texto, e é este texto que o teste da 2.1.0 procura.
        const val DOCUMENTOS = "Documents"
        const val PASTA = "Antares"
        const val PREFIXO = AutoBackup.PREFIXO
    }
}
