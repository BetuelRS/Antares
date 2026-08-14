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
