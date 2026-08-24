package pt.antares.app.core.nutrition

import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.LogOrigin

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

        /**
         * A origem de um registo do diário, sem ir à tabela dos alimentos.
         *
         * Não precisa de coluna nova: o `LogOrigin` já diz por que porta o registo entrou, e
         * o prefixo do identificador diz de que tabela veio. **Ir buscá-la ao alimento seria
         * pior**, e não só por ser mais caro: o alimento pode ter sido fundido noutro, ou
         * apagado, e a origem de um registo é a que era no dia em que foi feito — como tudo
         * o resto que o diário guarda.
         *
         * Sem identificador — as calorias soltas que se somam à pressa — a resposta é
         * `UNKNOWN`, e é a certa: ninguém sabe de onde vieram.
         */
        fun doRegisto(origin: LogOrigin, foodId: String?): FoodProvenance = when {
            origin == LogOrigin.AI_TEXT || origin == LogOrigin.AI_PHOTO -> AI

            // A fotografia de um rótulo é a leitura de uma declaração, não um palpite: erra
            // o que o rótulo erra, e o rótulo tem tolerâncias legais.
            origin == LogOrigin.AI_LABEL -> OFF
            origin == LogOrigin.BARCODE -> OFF
            foodId == null -> UNKNOWN
            else -> of(FoodSource.SEED, foodId)
        }

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
