package pt.antares.app.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "goal_history",
    indices = [Index(value = ["setOnEpochDay"])],
)
/**
 * Cada objetivo de peso que a pessoa definiu. Acumula em vez de substituir: o objetivo
 * anterior é o que dá sentido ao caminho já feito.
 */
data class GoalHistoryEntity(
    @PrimaryKey val id: String,
    val targetKg: Double,
    val setOnEpochDay: Long,

    // O peso no dia em que o objetivo foi posto, congelado aqui: sem ele, apagar pesagens
    // antigas mudaria a distância de um objetivo já cumprido.
    val startWeightKg: Double? = null,

    // Nulo enquanto não for atingido. Uma vez preenchido não volta atrás.
    val reachedOnEpochDay: Long? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

/**
 * Pesquisas que não devolveram nada. Ficam no telemóvel e não saem dele: servem para o
 * dono ver que alimentos faltam ao catálogo, através do ecrã de administração.
 */
@Serializable
@Entity(tableName = "search_miss", indices = [Index(value = ["query"], unique = true)])
data class SearchMissEntity(
    // O próprio texto é a chave: a mesma pesquisa repetida incrementa a contagem em vez
    // de criar linhas.
    @PrimaryKey val query: String,
    val count: Int = 1,
    val lastSeenEpochDay: Long,
)

@Serializable
@Entity(tableName = "progress_photo", indices = [Index(value = ["epochDay"])])
data class ProgressPhotoEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,

    // Caminho no armazenamento privado da app: a imagem nunca entra na base nem sai do
    // telemóvel. Apagar a linha não apaga o ficheiro, e o ecrã trata dos dois.
    val localPath: String,

    // O peso do dia congelado com a foto, para a comparação lado a lado não depender de
    // ir procurar a pesagem mais próxima anos depois.
    val weightKgSnapshot: Double? = null,
    val note: String? = null,
    val createdAt: Long,
)

// Único por dia de início: registar o mesmo ciclo duas vezes partiria a mediana do
// [CycleCalc], que trata cada linha como um ciclo distinto.
@Serializable
@Entity(tableName = "cycle_log", indices = [Index(value = ["startEpochDay"], unique = true)])
data class CycleEntity(
    @PrimaryKey val id: String,
    val startEpochDay: Long,

    // Nulo enquanto o período decorre; é o que permite registar o início sem saber o fim.
    val endEpochDay: Long? = null,
    val note: String? = null,
    val createdAt: Long,
)
