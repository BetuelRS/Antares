package pt.antares.app.core.privacy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class ImportMode {

    MERGE,

    REPLACE,
}

sealed interface ImportResult {

    data class Done(val porTabela: Map<String, Int>) : ImportResult {
        val total: Int get() = porTabela.values.sum()
    }

    data class NotABackup(val porque: String) : ImportResult

    data class Failed(val message: String) : ImportResult
}

class BackupImporter(
    private val sources: List<ExportSource<*>>,
    private val io: CoroutineDispatcher,

    private val wipe: suspend () -> Unit,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun import(conteudo: String, modo: ImportMode): ImportResult = withContext(io) {
        val raiz = runCatching { json.parseToJsonElement(conteudo).jsonObject }.getOrNull()
            ?: return@withContext ImportResult.NotABackup("não é JSON")

        if (!raiz.containsKey("exportadoEm") || !raiz.containsKey("versaoApp")) {
            return@withContext ImportResult.NotABackup("faltam os campos de um backup do Antares")
        }

        try {
            if (modo == ImportMode.REPLACE) wipe()

            val recibo = LinkedHashMap<String, Int>()
            for (source in sources) {
                val bruto = raiz[source.name] as? JsonArray ?: continue
                val escritas = aplicar(source, bruto, modo)
                if (escritas > 0) recibo[source.name] = escritas
            }
            ImportResult.Done(recibo)
        } catch (e: Throwable) {
            ImportResult.Failed(e.message ?: "falhou a importar")
        }
    }

    private suspend fun <T : Any> aplicar(
        source: ExportSource<T>,
        bruto: JsonArray,
        modo: ImportMode,
    ): Int {
        val restore = source.restore ?: return 0

        val doBackup = bruto.mapNotNull { runCatching { json.decodeFromJsonElement(source.serializer, it) }.getOrNull() }
        if (doBackup.isEmpty()) return 0

        val aEscrever = when (modo) {

            ImportMode.REPLACE -> doBackup
            ImportMode.MERGE -> {

                val locais = source.rows().associateBy({ idDe(it, source) }, { updatedAtDe(it, source) })
                doBackup.filter { linha ->
                    val local = locais[idDe(linha, source)]

                    local == null || updatedAtDe(linha, source) > local
                }
            }
        }
        if (aEscrever.isEmpty()) return 0
        restore(aEscrever)
        return aEscrever.size
    }

    private fun <T : Any> comoJson(valor: T, source: ExportSource<T>): JsonObject =
        json.encodeToJsonElement(source.serializer, valor).jsonObject

    private fun <T : Any> idDe(valor: T, source: ExportSource<T>): String {
        val obj = comoJson(valor, source)
        for (campo in listOf("id", "dayOfWeek", "epochDay", "startEpochDay", "query")) {
            obj[campo]?.jsonPrimitive?.content?.let { if (it.isNotEmpty()) return it }
        }

        return obj.toString()
    }

    private fun <T : Any> updatedAtDe(valor: T, source: ExportSource<T>): Long =
        comoJson(valor, source)["updatedAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
}
