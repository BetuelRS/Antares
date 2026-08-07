package pt.antares.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MealSlot {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    ;

    companion object {

        fun atHour(hour: Int): MealSlot = when (hour) {
            in 5..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 18..22 -> DINNER
            else -> SNACK
        }
    }
}

@Serializable
enum class LogOrigin { MANUAL, BARCODE, AI_TEXT, AI_PHOTO, AI_LABEL }

@Serializable
enum class FoodSource { SEED, OFF, CUSTOM, AI_ESTIMATE }
