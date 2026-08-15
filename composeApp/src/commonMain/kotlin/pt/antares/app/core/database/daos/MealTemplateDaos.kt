package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.MealTemplateEntity
import pt.antares.app.core.database.entities.MealTemplateItemEntity

@Dao
interface MealTemplateDao {

    @Upsert
    suspend fun upsert(template: MealTemplateEntity)

    @Query("UPDATE meal_template SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM meal_template WHERE deleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<MealTemplateEntity>>

    @Query("SELECT * FROM meal_template WHERE id = :id AND deleted = 0")
    suspend fun byId(id: String): MealTemplateEntity?

    @Query("SELECT * FROM meal_template WHERE deleted = 0")
    suspend fun exportRows(): List<MealTemplateEntity>
}

@Dao
interface MealTemplateItemDao {

    @Upsert
    suspend fun upsert(item: MealTemplateItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<MealTemplateItemEntity>)

    @Query("UPDATE meal_template_item SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM meal_template_item WHERE templateId = :templateId AND deleted = 0 ORDER BY updatedAt ASC")
    suspend fun forTemplate(templateId: String): List<MealTemplateItemEntity>

    @Query("SELECT * FROM meal_template_item WHERE deleted = 0")
    suspend fun exportRows(): List<MealTemplateItemEntity>
}
