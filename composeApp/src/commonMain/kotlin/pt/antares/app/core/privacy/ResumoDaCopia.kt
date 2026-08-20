package pt.antares.app.core.privacy

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * O que se sabe de um ficheiro de cópia antes de o abrir: quando foi feito, por que versão,
 * e quantas linhas traz de cada tabela.
 */
data class ResumoDaCopia(
    val exportadoEm: String?,
    val versaoApp: String?,
    val contagens: Map<String, Int>,
) {
    val total: Int get() = contagens.values.sum()
}

/**
 * Lê o cabeçalho de uma cópia sem a importar.
 *
 * Existe porque substituir os dados é irreversível e era feito às cegas: a app perguntava
 * «juntar ou substituir?» sobre um ficheiro de que não dizia nada. Escolher substituir com
 * uma cópia de há dois anos apaga dois anos, e nada no ecrã o deixava ver.
 */
object LeitorDeResumo {

    // Tolerante de propósito: o ficheiro vem de fora e pode ser qualquer coisa. Um resumo
    // que falha devolve nulo e o diálogo diz que não sabe — não impede a importação, que
    // tem as suas próprias defesas no [BackupImporter].
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun ler(conteudo: String): ResumoDaCopia? = runCatching {
        val raiz = json.parseToJsonElement(conteudo) as? JsonObject ?: return null
        val contagens = LinkedHashMap<String, Int>()
        for ((chave, valor) in raiz) {
            if (valor is JsonArray) contagens[chave] = valor.size
        }
        ResumoDaCopia(
            exportadoEm = raiz["exportadoEm"]?.jsonPrimitive?.contentOrNull(),
            versaoApp = raiz["versaoApp"]?.jsonPrimitive?.contentOrNull(),
            contagens = contagens,
        )
    }.getOrNull()

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
        content.takeIf { it.isNotBlank() && it != "null" }
}
