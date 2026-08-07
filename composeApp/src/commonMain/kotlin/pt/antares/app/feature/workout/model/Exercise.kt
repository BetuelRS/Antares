package pt.antares.app.feature.workout.model

data class Exercise(
    val id: String,
    val nameEn: String,
    val namePt: String,
    val category: String,
    val force: String?,
    val mechanic: String?,
    val equipment: String?,
    val level: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val instructionsEn: List<String>,
    val instructionsPt: List<String>,
    val imageUrls: List<String>,
    val isCustom: Boolean,
    val verified: Boolean,
) {

    val displayName: String get() = namePt.ifBlank { nameEn }

    val instructions: List<String> get() = instructionsPt.ifEmpty { instructionsEn }

    val instructionsUntranslated: Boolean get() = instructionsPt.isEmpty() && instructionsEn.isNotEmpty()
}
