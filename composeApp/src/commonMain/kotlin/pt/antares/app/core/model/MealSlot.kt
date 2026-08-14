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

        /**
         * A refeição que o campo já traz escolhida à hora a que se abre o ecrã. Os
         * intervalos deixam buracos de propósito: às quatro da tarde ou às duas da manhã o
         * mais provável é um lanche, e essas horas caem no `else`.
         */
        fun atHour(hour: Int): MealSlot = when (hour) {
            in 5..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 18..22 -> DINNER
            else -> SNACK
        }
    }
}

// Como o registo entrou no diário. Decide o que a app diz sobre a falta de
// micronutrientes — ver [MicroGap] — e nunca altera os números em si.
@Serializable
enum class LogOrigin { MANUAL, BARCODE, AI_TEXT, AI_PHOTO, AI_LABEL }

// De onde veio o alimento. `SEED` são os catálogos empacotados com a app, que se
// distinguem entre si pelo prefixo do identificador — ver [FoodProvenance].
@Serializable
enum class FoodSource { SEED, OFF, CUSTOM, AI_ESTIMATE }
