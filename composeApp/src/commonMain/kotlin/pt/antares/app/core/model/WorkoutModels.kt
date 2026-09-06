package pt.antares.app.core.model

import kotlinx.serialization.Serializable

// `DISCARDED` é o treino começado e abandonado. Fica na base em vez de ser apagado, mas
// não entra em volume nem em recordes — todas as agregações filtram por `DONE`.
@Serializable
enum class SessionStatus { ACTIVE, DONE, DISCARDED }

// `BROKEN` é o jejum interrompido antes da hora. Conta na média de duração e no histórico,
// porque foi tempo mesmo passado sem comer, mas não conta na sequência.
@Serializable
enum class FastingStatus { ACTIVE, COMPLETED, BROKEN }

/**
 * A regra por que uma rotina sobe de peso. Vive aqui, ao lado do [SessionStatus], porque é
 * uma coluna da base: o motor que a lê está no `core/calc/Progressao.kt`.
 *
 * Guardada pelo **nome** e não pelo número — o Room grava o `name` de uma enumeração —, e é
 * o que faz uma regra nova no meio da lista não mudar o significado das rotinas gravadas.
 */
@Serializable
enum class RegraDeProgressao {
    /** O que todas as rotinas de hoje têm, e a omissão de todas as que nascerem. */
    NENHUMA,

    /** Completou o topo do intervalo em todas as séries → sobe o peso. As repetições ficam. */
    LINEAR,

    /** Sobe as repetições até ao máximo primeiro; só depois o peso, e volta ao mínimo. */
    DUPLA,
}
