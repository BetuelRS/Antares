package pt.antares.app.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pt.antares.app.core.calc.WeeklyAggregate

@Serializable
data class AiUsage(
    val used: Int,
    val limit: Int,

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

    val matchedSource: String,
    val grams: Double,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val micros: Map<String, Double>? = null,
    val confidence: Double,

    val estimated: Boolean,

    val assumption: String? = null,
) {

    val needsReview: Boolean get() = confidence < 0.6 || estimated
}

@Serializable
data class FoodAnalysis(
    val items: List<AiFoodItem> = emptyList(),
    val totalKcal: Int = 0,
    val warnings: List<String> = emptyList(),
    val usage: AiUsage,
)

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
    val activity: String,
    @SerialName("activityEn") val activityEn: String,
    val durationMin: Double? = null,
    val met: Double,

    val kcal: Int? = null,
    val confidence: Double,
    val estimated: Boolean,
    val warnings: List<String> = emptyList(),
    val usage: AiUsage,
)

@Serializable
data class CoachRequest(
    val aggregate: WeeklyAggregate,

    val adaptive: CoachAdaptive? = null,
    val lang: String,
    val day: String,
)

@Serializable
data class CoachAdaptive(
    val newTargetKcal: Int,
    val previousTargetKcal: Int,
    val observedTdee: Int,
)

@Serializable
data class CoachAnalysis(
    val wins: List<String> = emptyList(),
    val observations: List<String> = emptyList(),
    val adjustments: List<String> = emptyList(),
    val focus: String = "",
    val usage: AiUsage,
)

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
