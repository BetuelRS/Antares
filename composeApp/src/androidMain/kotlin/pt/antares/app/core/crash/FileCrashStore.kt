package pt.antares.app.core.crash

import android.content.Context
import java.io.File

class FileCrashStore(context: Context) : CrashStore {

    private val ficheiro = File(context.filesDir, FICHEIRO)

    override fun write(report: String) {
        runCatching { ficheiro.writeText(report) }
    }

    override fun read(): String? =
        runCatching { if (ficheiro.exists()) ficheiro.readText() else null }.getOrNull()

    override fun clear() {
        runCatching { ficheiro.delete() }
    }

    private companion object {
        const val FICHEIRO = "ultimo_crash.txt"
    }
}
