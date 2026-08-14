package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "coach_report",
    indices = [Index(value = ["weekStartEpochDay"], unique = true)],
)
/**
 * O relatório semanal. Um por semana — daí o índice único — e escrito uma vez: reabrir
 * uma semana antiga tem de mostrar o que ela dizia na altura, não o que diria hoje.
 */
data class CoachReportEntity(
    @PrimaryKey val id: String,

    val weekStartEpochDay: Long,

    // Listas de chaves de tradução, não frases. É isto que faz o relatório mudar de idioma
    // com a app e sair inteiro do telemóvel, sem nenhum texto gerado por AI.
    val winsJson: String,
    val observationsJson: String,
    val adjustmentsJson: String,
    val focus: String,

    // O [WeeklyAggregate] inteiro, serializado: os números que sustentam o relatório ficam
    // com ele, e apagar registos antigos não altera o que já foi dito.
    val aggregateJson: String,

    // A proposta do [AdaptiveTdee], quando essa semana teve uma.
    val proposedKcal: Int? = null,
    val previousKcal: Int? = null,
    val observedTdee: Int? = null,

    // Três estados: null é por responder, e é o que faz o pedido continuar a aparecer.
    val proposalAccepted: Boolean? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)
