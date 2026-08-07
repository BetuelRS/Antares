package pt.antares.app.core.crash

interface CrashStore {

    fun write(report: String)

    fun read(): String?

    fun clear()
}

object NoCrashStore : CrashStore {
    override fun write(report: String) = Unit
    override fun read(): String? = null
    override fun clear() = Unit
}
