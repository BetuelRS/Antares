package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.feature.workout.model.Exercise

class ExerciseLibraryRepository(
    private val dao: ExerciseLibraryDao,
    private val seeder: ExerciseSeeder,
    private val io: CoroutineDispatcher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private fun ExerciseEntity.toModel(): Exercise {
        val base = seeder.imageBaseUrl
        return Exercise(
            id = id,
            nameEn = nameEn,
            namePt = namePt,
            category = category,
            force = force,
            mechanic = mechanic,
            equipment = equipment,
            level = level,
            primaryMuscles = ExerciseSeeder.unwrap(primaryMuscles),
            secondaryMuscles = ExerciseSeeder.unwrap(secondaryMuscles),
            instructionsEn = decode(instructionsEnJson),
            instructionsPt = decode(instructionsPtJson),
            // Caminhos relativos ganham o prefixo à leitura; um endereço completo passa
            // intacto. É o que permite mudar a origem das imagens sem reescrever a base.
            imageUrls = decode(imagesJson).map { path -> if (path.startsWith("http")) path else base + path },
            isCustom = isCustom,
            verified = verified,
        )
    }

    // Lista vazia em vez de exceção: um exercício com instruções malformadas continua
    // utilizável, só fica sem elas.
    private fun decode(jsonArray: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(jsonArray) }.getOrDefault(emptyList())

    fun observeFiltered(
        query: String,
        muscle: String?,
        equipment: String?,
        level: String?,
    ): Flow<List<Exercise>> =
        dao.observeFiltered(TextNormalize.normalize(query), muscle, equipment, level)
            .map { list -> list.map { it.toModel() } }

    suspend fun byId(id: String): Exercise? = withContext(io) { dao.byId(id)?.toModel() }

    suspend fun createCustom(
        nameEn: String,
        namePt: String,
        category: String,
        primaryMuscles: List<String>,
        equipment: String?,
    ): String = withContext(io) {
        val id = Ids.newUuid()
        dao.upsert(
            ExerciseEntity(
                id = id,
                // Um nome só preenche os dois campos: o filtro do catálogo procura em
                // ambos, e deixar um vazio escondia o exercício de metade das pesquisas.
                nameEn = nameEn.ifBlank { namePt },
                namePt = namePt.ifBlank { nameEn },
                searchText = TextNormalize.normalize("$nameEn $namePt"),
                category = category,
                force = null,
                mechanic = null,
                equipment = equipment,
                level = "beginner",
                primaryMuscles = ExerciseSeeder.wrap(primaryMuscles),
                secondaryMuscles = "",
                instructionsEnJson = "[]",
                instructionsPtJson = "[]",
                imagesJson = "[]",
                isCustom = true,
                verified = true,
                updatedAt = now(),
            ),
        )
        id
    }

    suspend fun deleteCustom(id: String) = withContext(io) { dao.softDeleteCustom(id, now()) }
}
