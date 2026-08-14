package pt.antares.app.core.crash

/**
 * Dá formato ao relatório de erro. Só metadados técnicos: nada aqui recolhe o que a pessoa
 * escreveu ou comeu, e o texto é para ela ler antes de decidir se o envia.
 */
object CrashReport {

    // As primeiras quarenta linhas chegam para identificar a origem, e o relatório continua
    // a caber num ecrã que se consegue ler.
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

    /**
     * A primeira linha que é da app. O topo da pilha é quase sempre uma biblioteca ou o
     * sistema; o que interessa é onde o código do Antares estava quando rebentou.
     */
    fun culpado(stack: List<String>, pacote: String = "pt.antares.app"): String? =
        stack.firstOrNull { it.startsWith(pacote) }
}
