package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.feature.running.domain.ActivityType
import pt.antares.app.feature.running.domain.RunStatus

@Serializable
@Entity(
    tableName = "run",
    indices = [Index("startedAt"), Index("status")],
)
/**
 * Uma corrida ou caminhada já terminada, com os resumos calculados uma vez e guardados.
 * A lista de atividades tem de abrir sem tocar nos milhares de pontos de GPS.
 */
data class RunEntity(
    @PrimaryKey val id: String,
    val type: ActivityType,
    val startedAt: Long,
    val endedAt: Long,
    val distanceM: Double,
    // Tempo em movimento e tempo total. As paragens contam para um e não para o outro,
    // e o ritmo médio sai do primeiro.
    val movingS: Long,
    val elapsedS: Long,
    val avgPaceSecPerKm: Int,
    val kcal: Int,
    val elevGainM: Double,
    // Percurso codificado à maneira dos mapas, para o desenhar sem ler os pontos todos.
    // Vazio numa corrida sem sinal, e é isso que deixa o detalhe com meio ecrã em branco.
    val polyline: String,
    val splitsJson: String,
    val name: String,
    val note: String,
    val status: RunStatus,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(
    tableName = "track_point",
    indices = [Index("runId")],
)
/**
 * Os pontos crus do GPS. Única tabela com chave gerada pela base e sem `deleted`: são
 * dezenas de milhar por corrida, ninguém apaga um ponto isolado, e vão-se embora com a
 * corrida a que pertencem.
 */
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val tMs: Long,
    val lat: Double,
    val lon: Double,
    val altM: Double?,
    // Raio de erro em metros, tal como o sistema o dá. Guarda-se para os pontos maus
    // poderem ser descartados depois, sem se perder o registo de que existiram.
    val accM: Double,
    val speedMps: Double?,
)
