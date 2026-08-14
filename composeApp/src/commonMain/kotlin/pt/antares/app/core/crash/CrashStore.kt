package pt.antares.app.core.crash

/**
 * Guarda o último relatório de erro num ficheiro local, para o ecrã "Sobre" o mostrar à
 * pessoa. Não sai do telemóvel: não há serviço de recolha de erros, e é ela que decide se
 * o quer partilhar.
 */
interface CrashStore {

    // Um só relatório de cada vez: escrever substitui.
    fun write(report: String)

    fun read(): String?

    fun clear()
}

object NoCrashStore : CrashStore {
    override fun write(report: String) = Unit
    override fun read(): String? = null
    override fun clear() = Unit
}
