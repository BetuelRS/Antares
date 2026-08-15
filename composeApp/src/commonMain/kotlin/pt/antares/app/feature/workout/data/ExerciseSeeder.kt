package pt.antares.app.feature.workout.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pt.antares.app.core.crash.CrashStore
import pt.antares.app.core.crash.registarEngolida
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.DbInfo
import pt.antares.app.core.database.entities.ExerciseEntity
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.generated.resources.Res

@Serializable
data class SeedExercisesFile(
    val version: Int,
    val imageBaseUrl: String,
    val exercises: List<SeedExercise>,
)

@Serializable
data class SeedExercise(
    val id: String,
    val nameEn: String,
    val namePt: String,
    val category: String,
    val force: String? = null,
    val mechanic: String? = null,
    val equipment: String? = null,
    val level: String,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructionsEn: List<String> = emptyList(),
    val instructionsPt: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val verified: Boolean = false,
)

class ExerciseSeeder(
    private val db: AntaresDb,
    private val io: CoroutineDispatcher,
    private val crashes: CrashStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    var imageBaseUrl: String = DEFAULT_IMAGE_BASE
        private set

    suspend fun seedIfNeeded() = withContext(io) {
        // Já semeado: só se relê o endereço das imagens, que é preciso em memória a cada
        // arranque, e se aplicam as correções de nomes que ainda faltem.
        if (db.dbInfoDao().get(KEY)?.value == DONE) {
            imageBaseUrl = db.dbInfoDao().get(KEY_IMAGE_BASE)?.value ?: DEFAULT_IMAGE_BASE

            fixNamesIfNeeded()
            return@withContext
        }

        // Falhar a ler ou a interpretar o ficheiro deixa a app sem catálogo de exercícios,
        // mas a funcionar: a próxima abertura tenta outra vez, porque a marca não foi posta.
        // Fica registado porque um catálogo vazio não se distingue, no ecrã, de um catálogo
        // que ainda não semeou.
        val file = try {
            @OptIn(ExperimentalResourceApi::class)
            val bytes = Res.readBytes("files/seed_exercises.json")
            json.decodeFromString<SeedExercisesFile>(bytes.decodeToString())
        } catch (e: Throwable) {
            crashes.registarEngolida(
                onde = "ExerciseSeeder: files/seed_exercises.json",
                erro = e,
                quando = Clock.System.now().toEpochMilliseconds(),
            )
            return@withContext
        }
        imageBaseUrl = file.imageBaseUrl
        db.dbInfoDao().upsert(DbInfo(KEY_IMAGE_BASE, file.imageBaseUrl))

        val now = Clock.System.now().toEpochMilliseconds()
        val entities = file.exercises.map { s ->
            ExerciseEntity(
                id = s.id,
                nameEn = s.nameEn,
                namePt = s.namePt,
                searchText = TextNormalize.normalize("${s.nameEn} ${s.namePt}"),
                category = s.category,
                force = s.force,
                mechanic = s.mechanic,
                equipment = s.equipment,
                level = s.level,
                primaryMuscles = wrap(s.primaryMuscles),
                secondaryMuscles = wrap(s.secondaryMuscles),
                instructionsEnJson = json.encodeToString(s.instructionsEn),
                instructionsPtJson = json.encodeToString(s.instructionsPt),
                imagesJson = json.encodeToString(s.images),
                isCustom = false,
                verified = s.verified,
                updatedAt = now,
            )
        }
        entities.chunked(300).forEach { db.exerciseLibraryDao().upsertAll(it) }
        db.dbInfoDao().upsert(DbInfo(KEY, DONE))
        fixNamesIfNeeded()
    }

    private suspend fun fixNamesIfNeeded() {
        if (db.dbInfoDao().get(KEY_NAMES)?.value == DONE_NAMES) return
        val dao = db.exerciseLibraryDao()
        dao.resetSeedNamesToEnglish()
        ExercisePtNames.curated.forEach { (nameEn, namePt) -> dao.setNamePtByNameEn(nameEn, namePt) }
        db.dbInfoDao().upsert(DbInfo(KEY_NAMES, DONE_NAMES))
    }

    companion object {
        private const val KEY = "seed_exercises_imported"
        private const val DONE = "v1"

        private const val KEY_IMAGE_BASE = "seed_exercises_image_base"
        private const val KEY_NAMES = "exercise_names_fixed"
        private const val DONE_NAMES = "v1"
        // As imagens não vêm dentro da app: são pedidas a este endereço quando alguém abre um
        // exercício, e quem as serve vê o IP e qual foi pedido. Guardar a base aqui, e só os
        // nomes de ficheiro na tabela, é o que permite mudá-la sem semear o catálogo outra vez.
        private const val DEFAULT_IMAGE_BASE =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

        fun wrap(items: List<String>): String =
            if (items.isEmpty()) "" else items.joinToString("|", prefix = "|", postfix = "|")

        fun unwrap(text: String): List<String> =
            text.trim('|').split('|').filter { it.isNotBlank() }
    }
}
