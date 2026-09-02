package pt.antares.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.SessionStatus

@Serializable
@Entity(
    tableName = "exercise",
    indices = [Index("category"), Index("equipment")],
)
/**
 * O catálogo de exercícios, semeado no primeiro arranque. Os campos de texto vêm de uma
 * base pública e por isso são strings livres e não enumerações: uma categoria nova nos
 * dados não pode fazer a app rebentar ao ler.
 */
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val namePt: String,

    // Pesquisa sem tabela FTS, ao contrário dos alimentos: o catálogo é de umas centenas
    // de linhas e um LIKE sobre esta coluna chega.
    val searchText: String,
    val category: String,
    val force: String?,
    val mechanic: String?,
    val equipment: String?,
    val level: String,

    // Listas em JSON. Só os primários contam para o volume por músculo, no [MuscleVolume].
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val instructionsEnJson: String,
    val instructionsPtJson: String,
    // Nomes de ficheiro, não endereços completos: o endereço é montado com a base guardada
    // pelo [ExerciseSeeder], que assim pode mudar sem semear o catálogo outra vez. As imagens
    // são descarregadas quando se abre o exercício — o texto vê-se sem rede, as imagens não.
    val imagesJson: String,
    // Só os exercícios criados pelo utilizador podem ser apagados; os do catálogo ficam.
    val isCustom: Boolean = false,
    val verified: Boolean = false,
    val updatedAt: Long,
    val deleted: Boolean = false,

)

@Serializable
@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val note: String?,
    val position: Int,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "routine_item",
    indices = [Index("routineId"), Index("exerciseId")],
)
/**
 * Um exercício dentro de uma rotina, com os alvos planeados. Isto é o plano; o que se fez
 * de facto está no `workout_set`, e os dois não se misturam.
 */
data class RoutineItemEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val exerciseId: String,
    val targetSets: Int,
    // Intervalo e não número exato: o alvo real é subir de peso quando se chega ao topo.
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetWeightKg: Double?,
    val restSec: Int,
    val position: Int,
    // Exercícios com o mesmo grupo alternam sem descanso pelo meio. Null é o caso normal,
    // de série a série.
    val supersetGroup: Int?,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "workout_session",
    indices = [Index("status"), Index("startedAt")],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    // Nulo enquanto o treino decorre. É o que permite fechar a app a meio e voltar.
    val endedAt: Long?,
    // Nulo num treino livre. A rotina fica registada por referência, e por isso editá-la
    // depois não reescreve o que foi feito — as séries guardam os seus próprios números.
    val routineId: String?,
    val note: String?,
    val status: SessionStatus,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

// O dia da semana é a própria chave: uma rotina por dia, e mudar o dia substitui.
@Serializable
@Entity(tableName = "routine_schedule")
data class RoutineScheduleEntity(
    @PrimaryKey val dayOfWeek: Int,
    val routineId: String,
    val updatedAt: Long,

    @ColumnInfo(defaultValue = "0")
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "workout_set",
    indices = [Index("sessionId"), Index("exerciseId")],
)
/**
 * Uma série executada. É a linha mais fina da app e a que alimenta tudo o resto: volume,
 * recordes e 1RM. Os limites do que se aceita aqui estão no [SetLimits] — um valor
 * absurdo fica como recorde para sempre.
 */
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    // O exercício repete-se na série, e não só na rotina: um treino livre não tem rotina
    // nenhuma e as séries têm de saber a que exercício pertencem.
    val exerciseId: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    // Esforço percebido, de 1 a 10. Opcional: quase ninguém o preenche todas as séries.
    val rpe: Double?,
    // O aquecimento fica de fora do volume e dos recordes — ver [VolumeCalc].
    val isWarmup: Boolean = false,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "session_exercise_note",
    primaryKeys = ["sessionId", "exerciseId"],
    indices = [Index("sessionId")],
)
/**
 * A nota de um exercício **neste treino**: «ombro a doer», «máquina 2 ocupada».
 *
 * Não vive na rotina de propósito. A [RoutineItemEntity] diz de si própria que é o plano e
 * que *«o que se fez de facto está no `workout_set`»* — uma nota do dia escrita na rotina
 * mudava o plano de todas as semanas seguintes por causa de um ombro de terça-feira.
 *
 * A chave é o par sessão-exercício, e não um id gerado: há uma nota por exercício em cada
 * treino, e um `upsert` sobre a mesma chave é o que faz reescrevê-la em vez de a duplicar.
 */
data class SessionExerciseNoteEntity(
    val sessionId: String,
    val exerciseId: String,
    val note: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
)
