package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import pt.antares.app.core.database.daos.ExerciseLibraryDao
import pt.antares.app.core.database.daos.ExerciseMarkDao
import pt.antares.app.core.database.daos.UsoDoExercicioRow
import pt.antares.app.core.database.daos.WorkoutSetDao
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.database.entities.ExerciseMarkEntity
import pt.antares.app.core.util.Ids
import pt.antares.app.core.util.epochMillisAt
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.core.util.weekStartEpochDay
import pt.antares.app.feature.workout.model.Exercise

class ExerciseLibraryRepository(
    private val dao: ExerciseLibraryDao,
    private val markDao: ExerciseMarkDao,
    private val setDao: WorkoutSetDao,
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
        soMeus: Boolean,
    ): Flow<List<Exercise>> =
        dao.observeFiltered(TextNormalize.normalize(query), muscle, equipment, soMeus)
            .map { list -> list.map { it.toModel() } }

    fun observeFavoritos(): Flow<Set<String>> = markDao.observeFavoritos().map { it.toSet() }

    suspend fun eFavorito(exerciseId: String): Boolean =
        withContext(io) { markDao.eFavorito(exerciseId) > 0 }

    /**
     * Marcar e desmarcar. Desmarcar **apaga a linha**: a ausência é que quer dizer «não é
     * favorito», e guardar um `false` seria uma segunda maneira de dizer o mesmo.
     */
    suspend fun marcarFavorito(exerciseId: String, favorito: Boolean) = withContext(io) {
        if (favorito) {
            markDao.upsert(ExerciseMarkEntity(exerciseId = exerciseId, updatedAt = now()))
        } else {
            markDao.delete(exerciseId)
        }
    }

    /**
     * Quantas vezes cada exercício foi feito nas últimas doze semanas, do mais feito para o
     * menos.
     *
     * **Doze semanas, e o número não é meu:** é o que a `estudo/areas/10` usa para a
     * frequência de treino — *«treinos por semana nas últimas 12»* —, e ter duas janelas
     * diferentes para «as últimas semanas» dentro da mesma área era o defeito que a 2.25.0
     * passou a versão inteira a desfazer.
     *
     * A janela conta-se em **semanas ISO**, como a do painel de treino e a da grelha do
     * progresso, e não em `24 h × N` a partir do relógio.
     */
    fun observeMaisFeitos(hojeEpochDay: Long): Flow<List<UsoDoExercicioRow>> {
        val primeiraSemana = weekStartEpochDay(hojeEpochDay) - (SEMANAS_DE_USO - 1) * DIAS_POR_SEMANA
        return setDao.observeUsoPorExercicio(epochMillisAt(primeiraSemana, minuteOfDay = 0))
    }

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

    suspend fun restoreCustom(id: String) = withContext(io) { dao.restoreCustom(id, now()) }

    companion object {
        /** A janela dos «que mais fazes», em semanas ISO. Ver o [observeMaisFeitos]. */
        const val SEMANAS_DE_USO = 12

        /** Quantos «que mais fazes» cabem na secção — o mesmo cinco dos recentes da 2.19.0. */
        const val MAIS_FEITOS = 5

        private const val DIAS_POR_SEMANA = 7L
    }
}
