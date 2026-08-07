package pt.antares.app.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pt.antares.app.core.database.entities.ExerciseEntity

data class ExerciseNameRow(val id: String, val nameEn: String, val namePt: String)

@Dao
interface ExerciseLibraryDao {

    @Upsert
    suspend fun upsertAll(items: List<ExerciseEntity>)

    @Upsert
    suspend fun upsert(item: ExerciseEntity)

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int

    @Query("SELECT * FROM exercise WHERE id = :id AND deleted = 0")
    suspend fun byId(id: String): ExerciseEntity?

    @Query("SELECT id, nameEn, namePt FROM exercise WHERE id IN (:ids)")
    suspend fun namesByIds(ids: List<String>): List<ExerciseNameRow>

    @Query("SELECT * FROM exercise WHERE deleted = 0 ORDER BY nameEn COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT * FROM exercise
        WHERE deleted = 0
          AND (:query = '' OR searchText LIKE '%' || :query || '%')
          AND (:muscle IS NULL OR primaryMuscles LIKE '%|' || :muscle || '|%'
               OR secondaryMuscles LIKE '%|' || :muscle || '|%')
          AND (:equipment IS NULL OR equipment = :equipment)
          AND (:level IS NULL OR level = :level)
        ORDER BY nameEn COLLATE NOCASE
        """,
    )
    fun observeFiltered(
        query: String,
        muscle: String?,
        equipment: String?,
        level: String?,
    ): Flow<List<ExerciseEntity>>

    @Query("UPDATE exercise SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id AND isCustom = 1")
    suspend fun softDeleteCustom(id: String, now: Long)

    @Query("UPDATE exercise SET namePt = nameEn WHERE isCustom = 0")
    suspend fun resetSeedNamesToEnglish(): Int

    @Query("UPDATE exercise SET namePt = :namePt WHERE nameEn = :nameEn AND isCustom = 0")
    suspend fun setNamePtByNameEn(nameEn: String, namePt: String)

    @Query(
        "SELECT * FROM exercise WHERE deleted = 0 AND searchText LIKE '%' || :query || '%' " +
            "ORDER BY LENGTH(nameEn) LIMIT 1",
    )
    suspend fun findFirstBySearch(query: String): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE deleted = 0 AND isCustom = 1")
    suspend fun exportRows(): List<ExerciseEntity>
}
