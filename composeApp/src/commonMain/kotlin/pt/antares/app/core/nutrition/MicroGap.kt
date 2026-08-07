package pt.antares.app.core.nutrition

import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.LogOrigin

enum class MicroGap {

    NONE,

    PACKAGED_LABEL,

    AI_ESTIMATE,

    USER_CREATED,

    NOT_MEASURED,

    RECIPE_INGREDIENTS,
    ;

    companion object {

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
