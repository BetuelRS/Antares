package pt.antares.app.feature.workout.ui

import org.jetbrains.compose.resources.StringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

private fun key(token: String): String = token.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_")

fun muscleLabel(token: String): StringResource = when (key(token)) {
    "abdominals" -> Res.string.ex_muscle_abdominals
    "abductors" -> Res.string.ex_muscle_abductors
    "adductors" -> Res.string.ex_muscle_adductors
    "biceps" -> Res.string.ex_muscle_biceps
    "calves" -> Res.string.ex_muscle_calves
    "chest" -> Res.string.ex_muscle_chest
    "forearms" -> Res.string.ex_muscle_forearms
    "glutes" -> Res.string.ex_muscle_glutes
    "hamstrings" -> Res.string.ex_muscle_hamstrings
    "lats" -> Res.string.ex_muscle_lats
    "lower_back" -> Res.string.ex_muscle_lower_back
    "middle_back" -> Res.string.ex_muscle_middle_back
    "neck" -> Res.string.ex_muscle_neck
    "quadriceps" -> Res.string.ex_muscle_quadriceps
    "shoulders" -> Res.string.ex_muscle_shoulders
    "traps" -> Res.string.ex_muscle_traps
    "triceps" -> Res.string.ex_muscle_triceps
    else -> Res.string.ex_muscle_other
}

fun equipmentLabel(token: String): StringResource = when (key(token)) {
    "body_only" -> Res.string.ex_equip_body_only
    "barbell" -> Res.string.ex_equip_barbell
    "dumbbell" -> Res.string.ex_equip_dumbbell
    "cable" -> Res.string.ex_equip_cable
    "machine" -> Res.string.ex_equip_machine
    "kettlebells" -> Res.string.ex_equip_kettlebells
    "bands" -> Res.string.ex_equip_bands
    "medicine_ball" -> Res.string.ex_equip_medicine_ball
    "exercise_ball" -> Res.string.ex_equip_exercise_ball
    "foam_roll" -> Res.string.ex_equip_foam_roll
    "e_z_curl_bar" -> Res.string.ex_equip_e_z_curl_bar
    else -> Res.string.ex_equip_other
}

fun levelLabel(token: String): StringResource = when (key(token)) {
    "beginner" -> Res.string.ex_level_beginner
    "intermediate" -> Res.string.ex_level_intermediate
    else -> Res.string.ex_level_expert
}

fun categoryLabel(token: String): StringResource = when (key(token)) {
    "strength" -> Res.string.ex_cat_strength
    "stretching" -> Res.string.ex_cat_stretching
    "plyometrics" -> Res.string.ex_cat_plyometrics
    "strongman" -> Res.string.ex_cat_strongman
    "powerlifting" -> Res.string.ex_cat_powerlifting
    "cardio" -> Res.string.ex_cat_cardio
    else -> Res.string.ex_cat_olympic_weightlifting
}
