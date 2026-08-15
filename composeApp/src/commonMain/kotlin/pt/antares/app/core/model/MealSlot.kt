package pt.antares.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MealSlot(val typicalHours: IntRange) {
    BREAKFAST(5..10),
    LUNCH(11..15),
    DINNER(18..22),

    // O lanche é o que sobra entre as outras, e por isso o intervalo dele não classifica
    // nada: são só as horas da tarde em que quase sempre acontece, para quando é preciso
    // **propor** uma hora em vez de a ler.
    SNACK(16..17),
    ;

    companion object {

        /**
         * A refeição que o campo já traz escolhida à hora a que se abre o ecrã. Os
         * intervalos deixam buracos de propósito: às quatro da tarde ou às duas da manhã o
         * mais provável é um lanche, e essas horas caem no `SNACK`.
         */
        fun atHour(hour: Int): MealSlot =
            entries.firstOrNull { it != SNACK && hour in it.typicalHours } ?: SNACK
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
