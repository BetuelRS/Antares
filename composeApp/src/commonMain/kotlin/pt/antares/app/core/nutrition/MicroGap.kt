package pt.antares.app.core.nutrition

import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.LogOrigin

/**
 * Porque é que um alimento não tem micronutrientes. Existe para o ecrã explicar o vazio em
 * vez de o deixar por dizer: o rótulo de uma embalagem não é obrigado a declará-los, uma
 * estimativa de AI não os sabe, e um alimento criado à mão só tem o que se escreveu.
 */
enum class MicroGap {

    NONE,

    PACKAGED_LABEL,

    AI_ESTIMATE,

    USER_CREATED,

    NOT_MEASURED,

    RECIPE_INGREDIENTS,
    ;

    companion object {

        // Ter micros ganha a tudo o resto: a origem só interessa para explicar a falta.
        fun ofLog(origin: LogOrigin, hasMicros: Boolean): MicroGap = when {
            hasMicros -> NONE
            origin == LogOrigin.BARCODE -> PACKAGED_LABEL
            origin == LogOrigin.AI_TEXT ||
                origin == LogOrigin.AI_PHOTO ||
                origin == LogOrigin.AI_LABEL -> AI_ESTIMATE
            else -> NOT_MEASURED
        }

        fun of(source: FoodSource, hasMicros: Boolean): MicroGap = when {
            hasMicros -> NONE
            source == FoodSource.OFF -> PACKAGED_LABEL
            source == FoodSource.AI_ESTIMATE -> AI_ESTIMATE
            source == FoodSource.CUSTOM -> USER_CREATED
            else -> NOT_MEASURED
        }
    }
}
