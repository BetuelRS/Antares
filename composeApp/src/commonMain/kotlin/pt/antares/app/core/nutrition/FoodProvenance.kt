package pt.antares.app.core.nutrition

import pt.antares.app.core.model.FoodSource

enum class FoodProvenance {

    CURATED,

    TCA,

    CIQUAL,

    USDA,

    OFF,

    AI,

    USER,

    UNKNOWN,
    ;

    companion object {

        fun of(source: FoodSource, id: String): FoodProvenance = when {
            source == FoodSource.OFF -> OFF
            source == FoodSource.AI_ESTIMATE -> AI
            source == FoodSource.CUSTOM -> USER
            id.startsWith("ptx") || id.startsWith("pt-") -> CURATED
            id.startsWith("tca-") -> TCA
            id.startsWith("ciqual-") -> CIQUAL
            id.startsWith("usda-") -> USDA
            else -> UNKNOWN
        }
    }
}
