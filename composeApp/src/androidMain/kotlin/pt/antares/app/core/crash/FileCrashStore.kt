package pt.antares.app.core.crash

import android.content.Context
import java.io.File

class FileCrashStore(context: Context) : CrashStore {

    private val ficheiro = File(context.filesDir, FICHEIRO)

    override fun write(report: String) {
        runCatching { ficheiro.writeText(report) }
    }

    // Nada aqui pode registar a própria falha: o sítio onde se registaria é este ficheiro.
    // Um erro a ler ou a escrever aparece como se não houvesse relatório nenhum.
    override fun read(): String? =
        runCatching { if (ficheiro.exists()) ficheiro.readText() else null }.getOrNull()

    override fun clear() {
        runCatching { ficheiro.delete() }
    }

    private companion object {
        const val FICHEIRO = "ultimo_crash.txt"
    }
}
