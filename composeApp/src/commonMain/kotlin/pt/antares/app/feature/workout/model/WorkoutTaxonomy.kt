package pt.antares.app.feature.workout.model

object WorkoutTaxonomy {
    val muscles = listOf(
        "abdominals", "abductors", "adductors", "biceps", "calves", "chest",
        "forearms", "glutes", "hamstrings", "lats", "lower back", "middle back",
        "neck", "quadriceps", "shoulders", "traps", "triceps",
    )

    val equipment = listOf(
        "body only", "barbell", "dumbbell", "cable", "machine", "kettlebells",
        "bands", "medicine ball", "exercise ball", "foam roll", "e-z curl bar", "other",
    )

    val levels = listOf("beginner", "intermediate", "expert")

    val categories = listOf(
        "strength", "stretching", "plyometrics", "strongman", "powerlifting",
        "cardio", "olympic weightlifting",
    )
}
