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

    /**
     * Todos os filtros do catálogo numa consulta só. Cada linha começa por deixar passar
     * tudo quando o filtro vem vazio, o que evita quatro consultas diferentes.
     *
     * As barras à volta do músculo são o que impede `abs` de encontrar `abductors`: os
     * músculos estão numa string separada por barras, e sem as delimitar o LIKE apanharia
     * qualquer parte de qualquer nome.
     */
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

    // O `isCustom = 1` na condição é a defesa: sem ele, um identificador enganado apagava
    // um exercício do catálogo que a app volta a precisar.
    @Query("UPDATE exercise SET deleted = 1, updatedAt = :now WHERE id = :id AND isCustom = 1")
    suspend fun softDeleteCustom(id: String, now: Long)

    @Query("UPDATE exercise SET deleted = 0, updatedAt = :now WHERE id = :id AND isCustom = 1")
    suspend fun restoreCustom(id: String, now: Long)

    // Repõe os nomes ingleses antes de reaplicar as traduções, para as que saíram da lista
    // não ficarem com o nome antigo. Não toca nos exercícios criados pelo utilizador.
    @Query("UPDATE exercise SET namePt = nameEn WHERE isCustom = 0")
    suspend fun resetSeedNamesToEnglish(): Int

    @Query("UPDATE exercise SET namePt = :namePt WHERE nameEn = :nameEn AND isCustom = 0")
    suspend fun setNamePtByNameEn(nameEn: String, namePt: String)

    // Ordenar pelo nome mais curto devolve o exercício genérico e não uma variante: quem
    // procura "supino" quer o supino, não o supino inclinado com halteres.
    @Query(
        "SELECT * FROM exercise WHERE deleted = 0 AND searchText LIKE '%' || :query || '%' " +
            "ORDER BY LENGTH(nameEn) LIMIT 1",
    )
    suspend fun findFirstBySearch(query: String): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE deleted = 0 AND isCustom = 1")
    suspend fun exportRows(): List<ExerciseEntity>
}
