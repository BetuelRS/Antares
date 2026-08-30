package pt.antares.app.core.database.entities

import androidx.room.ColumnInfo
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

    /**
     * O minuto do dia em que a atividade **começou**, de 0 a 1439 — a mesma unidade do
     * `FoodLogEntity.eatenAtMin`.
     *
     * É o início e não o fim porque é o início que as quatro origens sabem: a corrida tem
     * o instante do arranque, o treino tem o `startedAt` da sessão, a Health Connect tem o
     * `startMs`. Guardar o fim obrigava a inventá-lo em duas delas.
     *
     * Nulo é ausência e não meia-noite: um registo lançado num dia passado não tem hora
     * nenhuma para herdar, e escrever `00:00` era afirmar que se treinou de madrugada.
     */
    @ColumnInfo(defaultValue = "NULL") val startedAtMin: Int? = null,

    // Aponta ao treino ou à corrida que gerou esta linha, para apagar um apagar o outro.
    val refId: String?,
    val updatedAt: Long,
    val deleted: Boolean = false,
)
