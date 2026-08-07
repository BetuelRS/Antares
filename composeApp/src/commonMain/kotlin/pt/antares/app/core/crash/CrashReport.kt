package pt.antares.app.core.crash

object CrashReport {

    const val MAX_FRAMES = 40

    fun format(
        versao: String,
        quando: Long,
        thread: String,
        tipo: String,
        mensagem: String?,
        stack: List<String>,
        causa: String? = null,
    ): String = buildString {
        appendLine("Antares $versao")
        appendLine("quando: $quando")
        appendLine("thread: $thread")
        appendLine("erro: $tipo")

        appendLine("mensagem: ${mensagem ?: "(nenhuma)"}")
        causa?.let { appendLine("causa: $it") }
        appendLine("---")

        stack.take(MAX_FRAMES).forEach { appendLine("  at $it") }
        if (stack.size > MAX_FRAMES) {
            appendLine("  ... mais ${stack.size - MAX_FRAMES} linhas")
        }
    }

    fun culpado(stack: List<String>, pacote: String = "pt.antares.app"): String? =
        stack.firstOrNull { it.startsWith(pacote) }
}
