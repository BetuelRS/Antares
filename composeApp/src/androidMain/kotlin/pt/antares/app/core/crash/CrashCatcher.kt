package pt.antares.app.core.crash

/**
 * Grava o último erro fatal num ficheiro local antes de a app morrer. Não substitui o
 * comportamento do sistema: encadeia-se ao que já lá estava, para o Android continuar a
 * fazer o que faz — mostrar o diálogo e terminar o processo.
 */
object CrashCatcher {

    fun install(
        store: CrashStore,
        versao: String,
        agora: () -> Long = { System.currentTimeMillis() },
    ) {
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, erro ->

            // Se a gravação falhar, o erro original tem de seguir na mesma: nada aqui pode
            // impedir o sistema de saber que a app rebentou.
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

            // Passa ao tratador anterior, que é quem mostra o diálogo e mata o processo.
            // Sem nenhum, relança-se: engolir o erro deixaria a app viva e partida.
            anterior?.uncaughtException(thread, erro)
                ?: throw erro
        }
    }
}
