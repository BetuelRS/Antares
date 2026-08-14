package pt.antares.app.core.nutrition

import pt.antares.app.core.model.FoodSource

/**
 * De onde vêm os números de um alimento, para o ecrã poder dizê-lo. A app mostra sempre a
 * origem porque a diferença é real: uma tabela nacional analisou o alimento, uma
 * estimativa de AI adivinhou-o.
 */
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

        // A origem decide-se primeiro pela coluna `source` e só depois pelo prefixo do
        // identificador: as tabelas semeadas partilham a mesma origem e distinguem-se pelo
        // nome com que foram importadas.
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
