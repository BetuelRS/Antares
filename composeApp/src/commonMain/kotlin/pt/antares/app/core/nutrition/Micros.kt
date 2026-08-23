package pt.antares.app.core.nutrition

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

/**
 * Lê os micronutrientes de um alimento, guardados em JSON numa coluna de texto.
 *
 * Devolve um mapa vazio quando não há nada ou quando o texto não abre. **É a decisão certa
 * e está aqui uma vez só**: um alimento com micros ilegíveis tem de continuar a contar
 * calorias e macros, que estão em colunas próprias. E o buraco não fica escondido — os
 * ecrãs de micronutrientes dizem em que percentagem do que se comeu cada nutriente foi
 * medido, e um alimento saltado faz essa percentagem descer.
 *
 * **Um valor pode não ser um número.** Desde a v29 o catálogo escreve `"<0.1"` e
 * `"vestigios"` onde a fonte não deu um número mas disse alguma coisa — ver
 * [EstadoDeNutriente]. Esta função continua a devolver **só os números**, e é isso que faz
 * um vestígio não entrar nas somas do dia sem ninguém ter de se lembrar disso em cada conta.
 * Quem quiser os estados chama o [estadosDeJson].
 */
fun microsDeJson(texto: String?): Map<String, Double> =
    lerObjecto(texto)
        ?.mapNotNull { (chave, valor) ->
            (valor as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull?.let { chave to it }
        }
        ?.toMap()
        ?: emptyMap()

/**
 * O que se sabe sobre os nutrientes que não têm número: os que foram procurados e ficaram
 * abaixo do limite de deteção, e os que aparecem em vestígios.
 *
 * Um mapa vazio é o caso comum, e não uma falha: a esmagadora maioria dos alimentos não tem
 * nenhum. Dos 8 011 do catálogo, 2 138 têm pelo menos um.
 */
fun estadosDeJson(texto: String?): Map<String, EstadoDeNutriente> =
    lerObjecto(texto)
        ?.mapNotNull { (chave, valor) ->
            val bruto = (valor as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return@mapNotNull null
            EstadoDeNutriente.de(bruto)?.let { chave to it }
        }
        ?.toMap()
        ?: emptyMap()

/**
 * O objeto inteiro, sem julgar o que está lá dentro.
 *
 * O `ignoreUnknownKeys` importa: a lista de nutrientes cresce, e um catálogo semeado por uma
 * versão mais recente não pode deixar de abrir numa mais antiga. E ler para [JsonObject], em
 * vez de para um mapa de números, é o que impede um único valor de texto de fazer o alimento
 * inteiro deixar de ter micronutrientes — que é como isto falhava antes da v29.
 */
private fun lerObjecto(texto: String?): Map<String, kotlinx.serialization.json.JsonElement>? =
    texto?.let { runCatching { json.decodeFromString<JsonObject>(it) }.getOrNull() }

/** Escreve um mapa de números e um de estados no formato que o catálogo usa. */
fun microsParaJson(
    medidos: Map<String, Double>,
    estados: Map<String, EstadoDeNutriente> = emptyMap(),
): String? {
    if (medidos.isEmpty() && estados.isEmpty()) return null
    val objecto = buildMap<String, JsonPrimitive> {
        for ((k, v) in medidos) put(k, JsonPrimitive(v))
        for ((k, e) in estados) if (!containsKey(k)) put(k, JsonPrimitive(EstadoDeNutriente.escrever(e)))
    }
    return json.encodeToString(JsonObject.serializer(), JsonObject(objecto))
}
