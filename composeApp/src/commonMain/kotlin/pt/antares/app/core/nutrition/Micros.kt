package pt.antares.app.core.nutrition

import kotlinx.serialization.json.Json

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
 * O `ignoreUnknownKeys` importa: a lista de nutrientes cresce, e um catálogo semeado por
 * uma versão mais recente não pode deixar de abrir numa mais antiga.
 */
fun microsDeJson(texto: String?): Map<String, Double> =
    texto?.let { runCatching { json.decodeFromString<Map<String, Double>>(it) }.getOrNull() }
        ?: emptyMap()
