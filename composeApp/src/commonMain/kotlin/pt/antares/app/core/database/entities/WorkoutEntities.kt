package pt.antares.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import pt.antares.app.core.model.RegraDeProgressao
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

/**
 * Uma rotina, e **a regra por que ela sobe de peso**.
 *
 * A regra e o incremento vivem aqui e não na linha de cada exercício: a pergunta que a
 * `estudo/areas/09` fez ao catálogo não se põe: uma rotina é da pessoa de uma ponta à outra,
 * e não há catálogo nenhum que a substitua por baixo.
 */
@Serializable
@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val note: String?,
    val position: Int,

    // Guardada pelo nome, e com omissão: uma rotina que já existia não começa a subir de peso
    // por a app ter actualizado. A regra do `docs/referencia/base-de-dados.md` — uma coluna
    // nova nasce anulável ou com omissão — é o que faz esta migração ser automática.
    @ColumnInfo(defaultValue = "NENHUMA")
    val progressao: RegraDeProgressao = RegraDeProgressao.NENHUMA,

    // Em quilos, como tudo o que é carga nesta base. Nulo quer dizer **o degrau da unidade da
    // pessoa** — 2,5 kg ou 5 lb —, e não zero: quem nunca mexeu nisto não tem número escolhido,
    // e gravar o de hoje congelava-o se a pessoa mudasse de unidade amanhã.
    val incrementoKg: Double? = null,

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
    // A carga total da série, em quilos, como sempre foi. **Não muda de significado com o
    // peso do corpo**: numa dominada com cinto, o total é o corpo mais o cinto, e é esse
    // número que o volume, o 1RM e os recordes leem — nenhum deles precisa de saber de onde
    // ele veio. A `propostas/00` avisa que dois significados de `weightKg` seriam para
    // sempre; é por isso que ele só tem um.
    val weightKg: Double,
    // Quanto da carga veio do corpo. Nulo quer dizer «nenhum», que é o que era verdade em
    // toda a base antes da v39 — e continua a ser em cada série de barra ou de haltere.
    val bodyweightKg: Double? = null,
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

@Serializable
@Entity(tableName = "exercise_load")
/**
 * Quanto do peso do corpo conta como carga **neste** exercício, escolhido pela pessoa.
 *
 * Tabela própria e não coluna na `exercise` pela razão que a [FoodMarkEntity] já pagou uma
 * vez: o que é da pessoa não vive dentro de uma linha de catálogo. O catálogo de exercícios
 * ainda não é substituído, mas o dos alimentos é, e a lição custou os favoritos de quem
 * restaurou uma cópia.
 *
 * Por omissão não há linha nenhuma, e a ausência quer dizer **100 %** — o peso todo. Quem
 * puser uma flexão a 65 % ganha uma linha; quem nunca mexer nisto não ganha nenhuma.
 */
data class ExerciseLoadEntity(
    @PrimaryKey val exerciseId: String,
    // Percentagem inteira: meias percentagens numa estimativa destas são precisão a fingir.
    val bodyweightPercent: Int,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
@Entity(tableName = "exercise_marca")
/**
 * Que exercícios a pessoa marcou. Tabela própria pela mesma razão da [ExerciseLoadEntity] e
 * da [FoodMarkEntity]: o que é da pessoa não vive dentro de uma linha de catálogo.
 *
 * **Só guarda o favorito, e é de propósito.** A outra metade do que a
 * `estudo/areas/09-treino-biblioteca.md` pede — os usados recentemente, por frequência — já
 * está na base, na `workout_set` com a sessão a que pertence, e é de lá que sai. Escrevê-la
 * aqui outra vez era o «mesmo facto em dois sítios» que o `estudo/dados/04` §3 descreve, e
 * que custou ao sódio e à fibra uma migração para o desfazer.
 *
 * **A linha é o facto, e não há coluna nenhuma a repeti-lo.** Desmarcar apaga em vez de
 * gravar um `false`, como a [ExerciseLoadEntity] faz com os 100 % — e uma coluna que nunca
 * é falsa seria a segunda maneira de dizer o que a ausência já diz, que é exactamente a
 * crítica que estas duas tabelas fazem à lápide.
 */
data class ExerciseMarkEntity(
    @PrimaryKey val exerciseId: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
)
