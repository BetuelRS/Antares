package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.database.entities.MealTemplateItemEntity

/**
 * O que uma linha da lista precisa de dizer sobre um modelo, sem carregar os itens dele.
 *
 * A contagem e as calorias vêm de uma agregação em SQL e não de contar em Kotlin: a lista
 * mostra todos os modelos de uma vez, e ler os itens de cada um seria uma consulta por
 * linha — o padrão que enche a lista de leituras enquanto ela rola.
 */
data class ResumoDeModelo(
    val id: String,
    val itens: Int,
    val kcal: Int,
)

@Dao
interface MealTemplateDao {

    @Upsert
    suspend fun upsert(template: MealTemplateEntity)

    @Query("UPDATE meal_template SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE meal_template SET deleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("SELECT * FROM meal_template WHERE deleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<MealTemplateEntity>>

    @Query("SELECT * FROM meal_template WHERE id = :id AND deleted = 0")
    suspend fun byId(id: String): MealTemplateEntity?

    @Query("SELECT * FROM meal_template WHERE deleted = 0")
    suspend fun exportRows(): List<MealTemplateEntity>

    /**
     * Quantos itens e quantas calorias tem cada modelo.
     *
     * Só os modelos que têm itens aparecem aqui — a junção interna trata disso, e um modelo
     * vazio não tem nada para contar. Quem lê trata a ausência como zero.
     */
    @Query(
        """
        SELECT t.id AS id, COUNT(i.id) AS itens, CAST(SUM(i.kcalSnapshot) AS INTEGER) AS kcal
        FROM meal_template t
        JOIN meal_template_item i ON i.templateId = t.id AND i.deleted = 0
        WHERE t.deleted = 0
        GROUP BY t.id
        """,
    )
    fun observeResumos(): Flow<List<ResumoDeModelo>>
}

@Dao
interface MealTemplateItemDao {

    @Upsert
    suspend fun upsert(item: MealTemplateItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<MealTemplateItemEntity>)

    @Query("UPDATE meal_template_item SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE meal_template_item SET deleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("SELECT * FROM meal_template_item WHERE templateId = :templateId AND deleted = 0 ORDER BY updatedAt ASC")
    suspend fun forTemplate(templateId: String): List<MealTemplateItemEntity>

    // Sem filtrar as apagadas, e é essa a razão de existir: desfazer o apagamento de um
    // modelo tem de encontrar as linhas que acabaram de ser marcadas.
    @Query("SELECT * FROM meal_template_item WHERE templateId = :templateId ORDER BY updatedAt ASC")
    suspend fun forTemplateForWrite(templateId: String): List<MealTemplateItemEntity>

    @Query("SELECT * FROM meal_template_item WHERE deleted = 0")
    suspend fun exportRows(): List<MealTemplateItemEntity>
}
