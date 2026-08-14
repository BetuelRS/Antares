package pt.antares.app.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * O contrato com as Edge Functions. Cada `@Serializable` aqui tem de bater certo com o
 * esquema do lado do servidor, em `supabase/functions`: mudar um nome de campo só de um
 * lado não dá erro de compilação, dá uma resposta que não se consegue ler.
 */

/** A contagem de utilizações, decidida no servidor. A app só a mostra. */
@Serializable
data class AiUsage(
    val used: Int,
    val limit: Int,

    // Distingue o período de experiência do plano normal, para o ecrã dizer o que muda
    // quando acabar em vez de o número mudar sem aviso.
    val trial: Boolean,
) {
    val remaining: Int get() = (limit - used).coerceAtLeast(0)
}

@Serializable
data class FoodAnalysisRequest(
    val mode: String,
    val text: String? = null,
    val imageBase64: String? = null,
    val imageMime: String? = null,
    val lang: String,

    val day: String,
)

@Serializable
data class AiFoodItem(
    val name: String,

    // Diz se o servidor encontrou o alimento numa tabela ou se o modelo o estimou. É a
    // diferença entre um valor analisado e um palpite, e o ecrã mostra-a.
    val matchedSource: String,
    val grams: Double,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val micros: Map<String, Double>? = null,
    val confidence: Double,

    val estimated: Boolean,

    // O que o modelo assumiu para chegar ao número — o tamanho de uma dose, o método de
    // cozedura. Mostra-se para a pessoa poder discordar em vez de aceitar às cegas.
    val assumption: String? = null,
) {

    // Abaixo de 60% de confiança, ou sendo estimativa, o ecrã marca o item para revisão em
    // vez de o gravar direto.
    val needsReview: Boolean get() = confidence < 0.6 || estimated
}

@Serializable
data class FoodAnalysis(
    val items: List<AiFoodItem> = emptyList(),
    val totalKcal: Int = 0,
    val warnings: List<String> = emptyList(),
    val usage: AiUsage,
)

// Tudo anulável: um rótulo real pode não declarar metade destes campos, e um zero
// assumido passaria por uma medição.
@Serializable
data class LabelPer100g(
    val kcal: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,

    val sugars: Double? = null,
    val satFat: Double? = null,
    val fiber: Double? = null,
    val sodiumMg: Double? = null,
)

@Serializable
data class LabelDraft(
    val name: String? = null,
    val servingG: Double? = null,
    val per100g: LabelPer100g = LabelPer100g(),

    val micros: Map<String, Double>? = null,
)

@Serializable
data class LabelAnalysis(
    val draft: LabelDraft,
    val warnings: List<String> = emptyList(),
    val usage: AiUsage,
)

@Serializable
data class ExerciseAnalysisRequest(
    val text: String,
    val weightKg: Double,
    val lang: String,
    val day: String,
)

@Serializable
data class ExerciseAnalysis(
    // O nome vem nos dois idiomas: o local para mostrar, o inglês para casar com o
    // catálogo de METs, que está em inglês.
    val activity: String,
    @SerialName("activityEn") val activityEn: String,
    // Nulo quando a descrição não diz quanto tempo durou; o ecrã pergunta em vez de supor.
    val durationMin: Double? = null,
    val met: Double,

    // As calorias podem vir calculadas ou não. Vindo nulas, a app calcula-as com o MET e o
    // peso — a conta é a mesma do [MetCalc], e o resultado não depende de haver rede.
    val kcal: Int? = null,
    val confidence: Double,
    val estimated: Boolean,
    val warnings: List<String> = emptyList(),
    val usage: AiUsage,
)

/**
 * Códigos que o servidor devolve quando a análise não deu o que se pedia. São códigos e
 * não frases para o ecrã os traduzir; o modelo nunca escreve texto que a app mostre.
 */
object AiWarnings {
    const val NOT_FOOD = "NOT_FOOD"

    const val VAGUE_ITEM = "VAGUE_ITEM"
    const val UNCLEAR_IMAGE = "UNCLEAR_IMAGE"
    const val NOT_LABEL = "NOT_LABEL"
    const val LABEL_INCOMPLETE = "LABEL_INCOMPLETE"

    const val LABEL_CONVERTED = "LABEL_CONVERTED"

    const val LABEL_INCONSISTENT = "LABEL_INCONSISTENT"

    const val LABEL_NO_SERVING_G = "LABEL_NO_SERVING_G"
    const val NOT_EXERCISE = "NOT_EXERCISE"
    const val NO_DURATION = "NO_DURATION"
}
