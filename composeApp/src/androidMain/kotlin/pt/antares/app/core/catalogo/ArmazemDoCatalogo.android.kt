package pt.antares.app.core.catalogo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Os três ficheiros numa pasta do armazenamento privado da app.
 *
 * Recebe a pasta e não o `Context` de propósito: é o que deixa os testes-guarda correrem
 * contra uma pasta temporária de verdade, com trocas e ficheiros cortados a meio, em vez de
 * contra uma imitação que concorda com o que se espera dela.
 */
actual class ArmazemDoCatalogo(
    private val pasta: File,
    private val io: CoroutineDispatcher,
) {

    private val instalado get() = File(pasta, NOME)
    private val provisorio get() = File(pasta, "$NOME.a-descer")
    private val anterior get() = File(pasta, "$NOME.anterior")

    actual fun caminho(): String = instalado.absolutePath

    actual suspend fun ler(): ByteArray? = withContext(io) {

        // A reparação faz-se aqui, e não num arranque à parte: este é o único sítio por onde
        // o catálogo instalado é lido, e portanto o único onde a falta dele importa.
        if (!instalado.exists() && anterior.exists()) anterior.renameTo(instalado)

        if (!instalado.exists()) null else runCatching { instalado.readBytes() }.getOrNull()
    }

    actual suspend fun guardarProvisorio(bytes: ByteArray): Boolean = withContext(io) {
        runCatching {
            pasta.mkdirs()
            provisorio.writeBytes(bytes)
            true
        }.getOrElse {

            // Um ficheiro meio escrito é pior do que nenhum: fica com o tamanho errado e
            // sem ninguém a saber disso.
            provisorio.delete()
            false
        }
    }

    actual suspend fun trocar(): Boolean = withContext(io) {
        if (!provisorio.exists()) return@withContext false

        if (instalado.exists()) {
            anterior.delete()
            if (!instalado.renameTo(anterior)) return@withContext false
        }

        if (provisorio.renameTo(instalado)) {
            true
        } else {

            // Nada de meio caminho: se o novo não entrou, o velho volta ao lugar antes de
            // esta função responder.
            if (anterior.exists()) anterior.renameTo(instalado)
            false
        }
    }

    actual suspend fun descartarProvisorio() {
        withContext(io) { provisorio.delete() }
    }

    actual suspend fun esquecerAnterior() {
        withContext(io) { anterior.delete() }
    }

    private companion object {
        // O mesmo nome que o ficheiro tem dentro do APK, para quem for lá ver reconhecer o
        // que está a olhar.
        const val NOME = "catalogo.json"
    }
}
