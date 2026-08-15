package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.ExerciseOrigin

@Serializable
@Entity(
    tableName = "exercise_log",
    indices = [Index("epochDay")],
)
/**
 * Atividade que gastou calorias, venha de onde vier: escrita à mão, um treino de pesos ou
 * uma corrida. É a tabela que o orçamento do dia lê, para não ter de somar três origens.
 */
data class ExerciseLogEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val origin: ExerciseOrigin,
    val label: String,
    // O MET fica copiado além da referência à tabela: rever a tabela não pode mexer nas
    // calorias já contadas num dia fechado.
    val metId: String?,
    val met: Double?,
    val durationMin: Int,
    val kcal: Int,

    // Aponta ao treino ou à corrida que gerou esta linha, para apagar um apagar o outro.
    val refId: String?,
    val updatedAt: Long,
    val deleted: Boolean = false,
)
