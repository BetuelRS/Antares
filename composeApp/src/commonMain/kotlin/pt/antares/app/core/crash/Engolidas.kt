package pt.antares.app.core.crash

/**
 * Falhas que a app **decide engolir** para continuar a funcionar, e que mesmo assim têm de
 * deixar rasto.
 *
 * A diferença para um `runCatching` legítimo está no que a pessoa vê a seguir. Falhar a ler
 * um alimento com micronutrientes corrompidos tira uma linha de um total, e nota-se; falhar
 * a ler o ficheiro do catálogo deixa a app **sem alimentos nenhuns**, com um ecrã de
 * pesquisa vazio e sem nada que explique porquê. É este segundo caso que vem aqui parar.
 *
 * Vai para o mesmo sítio que o último erro fatal, que é o único que o ecrã «último erro»
 * mostra. Escrever substitui o que lá estava: o último a acontecer é o que interessa.
 */
fun CrashStore.registarEngolida(onde: String, erro: Throwable, quando: Long) {
    write(
        CrashReport.format(
            versao = ENGOLIDA,
            quando = quando,
            thread = onde,
            tipo = erro::class.qualifiedName ?: erro::class.simpleName.orEmpty(),
            mensagem = erro.message,
            // A app não morreu, portanto não há pilha do sistema para mostrar. O sítio já
            // vai no campo de cima, e é o que permite reproduzir.
            stack = emptyList(),
            causa = erro.cause?.let { "${it::class.simpleName}: ${it.message ?: "(sem mensagem)"}" },
        ),
    )
}

/**
 * Marca a primeira linha do relatório. Quem abrir o ecrã tem de perceber de imediato que a
 * app não rebentou — continuou a andar sem uma parte.
 */
const val ENGOLIDA = "— a app continuou, mas sem esta parte —"
