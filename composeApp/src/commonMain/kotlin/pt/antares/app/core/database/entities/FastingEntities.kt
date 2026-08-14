package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.FastingStatus

@Serializable
@Entity(tableName = "fasting_protocol")
data class FastingProtocolEntity(
    @PrimaryKey val id: String,
    val name: String,
    val fastingHours: Int,
    val isCustom: Boolean = false,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = false,
)

@Serializable
@Entity(
    tableName = "fasting_session",
    indices = [Index("status"), Index("startedAt")],
)
data class FastingSessionEntity(
    @PrimaryKey val id: String,
    val protocolId: String,
    val startedAt: Long,
    // A hora-alvo fica congelada no início: mudar o protocolo depois não redefine o que
    // era completar um jejum já a decorrer.
    val targetEndAt: Long,
    // Nulo enquanto decorre. As estatísticas ignoram sessões abertas, porque ainda não
    // têm duração — só uma contagem a subir.
    val endedAt: Long?,
    val status: FastingStatus,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)
