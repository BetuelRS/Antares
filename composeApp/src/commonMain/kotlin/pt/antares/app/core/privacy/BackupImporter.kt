package pt.antares.app.core.privacy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Como tratar o que já está no telemóvel. `MERGE` mantém os registos locais mais recentes;
 * `REPLACE` apaga tudo antes de escrever.
 */
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

        // Estes dois campos são a assinatura de uma cópia do Antares. Verificam-se antes de
        // qualquer escrita porque o modo `REPLACE` apaga tudo — um JSON qualquer não pode
        // chegar para destruir os dados de alguém.
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

        // Linha que não desserializa é saltada: uma cópia de uma versão antiga da app não
        // pode ficar irrecuperável por causa de um campo que entretanto mudou.
        val doBackup = bruto.mapNotNull { runCatching { json.decodeFromJsonElement(source.serializer, it) }.getOrNull() }
        if (doBackup.isEmpty()) return 0

        val aEscrever = when (modo) {

            ImportMode.REPLACE -> doBackup
            ImportMode.MERGE -> {

                // Ganha o mais recente por `updatedAt`. Empate fica com o local: em caso de
                // dúvida, o que está no telemóvel é o que a pessoa acabou de ver.
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

    /**
     * A identidade de uma linha, procurada por JSON porque as tabelas não partilham
     * interface nenhuma. A ordem dos campos é a das chaves primárias reais: `id` na
     * maioria, o dia da semana no calendário de rotinas, o dia nas tabelas de um registo
     * por dia, e o texto na tabela de pesquisas falhadas.
     */
    private fun <T : Any> idDe(valor: T, source: ExportSource<T>): String {
        val obj = comoJson(valor, source)
        for (campo in listOf("id", "dayOfWeek", "epochDay", "startEpochDay", "query")) {
            obj[campo]?.jsonPrimitive?.content?.let { if (it.isNotEmpty()) return it }
        }

        // Sem nenhum destes campos, a linha inteira serve de identidade: duas linhas
        // idênticas são a mesma coisa, e é o comportamento certo para tabelas sem chave.
        return obj.toString()
    }

    // Sem `updatedAt` conta como zero, e por isso perde sempre contra o local: as tabelas
    // que não o têm não sabem dizer qual das versões é mais nova.
    private fun <T : Any> updatedAtDe(valor: T, source: ExportSource<T>): Long =
        comoJson(valor, source)["updatedAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
}
