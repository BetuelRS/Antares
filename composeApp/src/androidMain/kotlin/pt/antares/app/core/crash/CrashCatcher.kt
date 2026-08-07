package pt.antares.app.core.crash

object CrashCatcher {

    fun install(
        store: CrashStore,
        versao: String,
        agora: () -> Long = { System.currentTimeMillis() },
    ) {
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, erro ->

            runCatching {
                store.write(
                    CrashReport.format(
                        versao = versao,
                        quando = agora(),
                        thread = thread.name,
                        tipo = erro::class.qualifiedName ?: erro::class.simpleName.orEmpty(),
                        mensagem = erro.message,
                        stack = erro.stackTrace.map { it.toString() },
                        causa = erro.cause?.let { c ->
                            "${c::class.simpleName}: ${c.message ?: "(sem mensagem)"}"
                        },
                    ),
                )
            }

            anterior?.uncaughtException(thread, erro)
                ?: throw erro
        }
    }
}
