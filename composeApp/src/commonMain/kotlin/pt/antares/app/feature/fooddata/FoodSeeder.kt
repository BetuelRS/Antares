package pt.antares.app.feature.fooddata

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.DbInfo
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodFtsEntity
import pt.antares.app.core.fooddata.DrinkClassifier
import pt.antares.app.core.fooddata.UsdaNameCleaner
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.generated.resources.Res

@Serializable
data class SeedFood(
    val id: String,
    val source: String,
    val sourceRef: String? = null,
    val nameEn: String,
    val namePt: String,
    val brand: String? = null,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val sugarsG: Double? = null,
    val fatG: Double,
    val satFatG: Double? = null,
    val fiberG: Double? = null,
    val sodiumMg: Int? = null,
    val micros: Map<String, Double>? = null,
    val servingName: String? = null,
    val servingGrams: Double? = null,
    val verified: Boolean = false,
)

class FoodSeeder(
    private val db: AntaresDb,
    private val io: CoroutineDispatcher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Semeia o catálogo e aplica-lhe as correções acumuladas. Corre a cada arranque, e
     * cada passo verifica primeiro a sua marca na `db_info` — é isso que o torna barato e
     * seguro de repetir.
     *
     * A ordem é a das dependências: importar antes de limpar, limpar antes de deduplicar.
     * Cada passo é uma correção ao catálogo que já foi distribuído, e por isso não podem
     * ser fundidos num só nem reordenados — quem tem a app instalada há muito passou por
     * uns e não por outros.
     */
    suspend fun seedIfNeeded() = withContext(io) {

        val importedAt = importIfNeeded("files/seed_foods.json", KEY, DONE)
        if (importedAt != null) pruneOldCatalog(importedAt)
        importIfNeeded("files/seed_foods_pt.json", KEY_PT, DONE_PT)

        importIfNeeded("files/seed_foods_pt2.json", KEY_PT2, DONE_PT2)

        importIfNeeded("files/seed_foods_pt3.json", KEY_PT3, DONE_PT3)

        importIfNeeded("files/seed_foods_tca.json", KEY_TCA, DONE_TCA)
        cleanUsdaNamesIfNeeded()
        markLiquidsIfNeeded()
        cleanUsdaNamesV2IfNeeded()
        dedupeFtsIfNeeded()
        restoreCuratedNamesIfNeeded()
        pruneCuratedCoveredByTcaIfNeeded()
        pruneDuplicateCuratedIfNeeded()
        markAnalysedVerifiedIfNeeded()
        enrichCuratedWithMicrosIfNeeded()
        stampCatalogueRebuildIfNeeded()
    }

    private suspend fun stampCatalogueRebuildIfNeeded() {
        if (db.dbInfoDao().get(KEY_REBUILT_DAY) != null) return
        db.dbInfoDao().upsert(DbInfo(KEY_REBUILT_DAY, todayEpochDay().toString()))
    }

    private suspend fun markAnalysedVerifiedIfNeeded() {
        if (db.dbInfoDao().get(KEY_VERIFIED)?.value == DONE_VERIFIED) return
        db.foodDao().markAnalysedAsVerified()
        db.dbInfoDao().upsert(DbInfo(KEY_VERIFIED, DONE_VERIFIED))
    }

    private suspend fun pruneCuratedCoveredByTcaIfNeeded() {
        if (db.dbInfoDao().get(KEY_TCA_DUPES)?.value == DONE_TCA_DUPES) return
        val tcaNames = db.foodDao().tcaIdsAndNames()
            .map { normalizeForDedupe(it.namePt) }
            .toHashSet()
        if (tcaNames.isEmpty()) return

        val duplicados = db.foodDao().curatedIdsAndNames()
            .filter { normalizeForDedupe(it.namePt) in tcaNames }
            .map { it.id }

        duplicados.chunked(400).forEach { db.foodDao().pruneByIds(it) }
        db.foodDao().pruneOrphanFts()
        db.dbInfoDao().upsert(DbInfo(KEY_TCA_DUPES, DONE_TCA_DUPES))
    }

    private suspend fun pruneDuplicateCuratedIfNeeded() {
        if (db.dbInfoDao().get(KEY_PT_DUPES)?.value == DONE_PT_DUPES) return
        db.foodDao().pruneDuplicateCurated()
        db.foodDao().pruneOrphanFts()
        db.dbInfoDao().upsert(DbInfo(KEY_PT_DUPES, DONE_PT_DUPES))
    }

    private suspend fun markLiquidsIfNeeded() {
        if (db.dbInfoDao().get(KEY_LIQUID)?.value == DONE_LIQUID) return

        db.foodDao().clearAllLiquid()
        val liquidIds = db.foodDao().nameRows()
            .filter { DrinkClassifier.isLiquid(it.namePt, it.nameEn) }
            .map { it.id }
        liquidIds.chunked(400).forEach { db.foodDao().markLiquid(it) }
        db.dbInfoDao().upsert(DbInfo(KEY_LIQUID, DONE_LIQUID))
    }

    private suspend fun cleanUsdaNamesV2IfNeeded() {
        if (db.dbInfoDao().get(KEY_CLEAN2)?.value == DONE_CLEAN2) return
        db.foodDao().usdaNameRows().forEach { row ->
            val cleaned = UsdaNameCleaner.clean(row.nameEn)
            if (cleaned.isNotBlank() && cleaned != row.namePt) {
                val fts = TextNormalize.normalize("$cleaned ${row.nameEn} ${row.brand.orEmpty()}")
                db.foodDao().setDisplayNameWithFts(row.id, cleaned, fts)
            }
        }
        db.dbInfoDao().upsert(DbInfo(KEY_CLEAN2, DONE_CLEAN2))
    }

    private suspend fun cleanUsdaNamesIfNeeded() {
        if (db.dbInfoDao().get(KEY_CLEAN)?.value == DONE_CLEAN) return
        db.foodDao().cleanUsdaDisplayNames()
        db.dbInfoDao().upsert(DbInfo(KEY_CLEAN, DONE_CLEAN))
    }

    private suspend fun pruneOldCatalog(importedAt: Long) {
        db.foodDao().pruneStaleUsda(importedAt)
        db.foodDao().pruneOrphanFts()
    }

    private suspend fun dedupeFtsIfNeeded() {
        if (db.dbInfoDao().get(KEY_FTS_DEDUPE)?.value == DONE_FTS_DEDUPE) return
        db.foodDao().dedupeFts()
        db.dbInfoDao().upsert(DbInfo(KEY_FTS_DEDUPE, DONE_FTS_DEDUPE))
    }

    private suspend fun restoreCuratedNamesIfNeeded() {
        if (db.dbInfoDao().get(KEY_PT_NAMES)?.value == DONE_PT_NAMES) return
        var restored = 0
        for ((file, onlyPtExtras) in PT_SEED_FILES) {
            @OptIn(ExperimentalResourceApi::class)
            val bytes = runCatching { Res.readBytes(file) }.getOrNull() ?: continue
            val seeds = json.decodeFromString<List<SeedFood>>(bytes.decodeToString())
            for (s in seeds) {

                if (onlyPtExtras && !s.id.startsWith("pt-")) continue
                val current = db.foodDao().byId(s.id) ?: continue
                if (current.namePt == s.namePt) continue
                val fts = TextNormalize.normalize(
                    "${s.namePt} ${s.nameEn} ${s.brand.orEmpty()}",
                )
                db.foodDao().setDisplayNameWithFts(s.id, s.namePt, fts)
                restored++
            }
        }
        db.dbInfoDao().upsert(DbInfo(KEY_PT_NAMES, DONE_PT_NAMES))
    }

    private suspend fun enrichCuratedWithMicrosIfNeeded() {
        if (db.dbInfoDao().get(KEY_PT_MICROS)?.value == DONE_PT_MICROS) return

        @OptIn(ExperimentalResourceApi::class)
        val bytes = runCatching { Res.readBytes("files/seed_pt_micros.json") }.getOrNull()
        if (bytes != null) {
            val table = json.decodeFromString<Map<String, Map<String, Double>>>(
                bytes.decodeToString(),
            )
            for ((id, micros) in table) {
                if (micros.isEmpty()) continue

                val food = db.foodDao().byId(id) ?: continue
                if (food.microsJson != null) continue
                db.foodDao().setMicros(id, Json.encodeToString(micros))
            }
        }
        db.dbInfoDao().upsert(DbInfo(KEY_PT_MICROS, DONE_PT_MICROS))
    }

    /**
     * Importa um ficheiro de catálogo se ainda não foi, ou se a sua versão mudou. Devolve
     * o instante da importação, que serve para limpar o que ficou da versão anterior.
     *
     * A marca é chave e versão: subir a versão faz o ficheiro ser reimportado por cima de
     * quem já o tinha, sem tocar em mais nada.
     */
    private suspend fun importIfNeeded(file: String, key: String, doneVersion: String): Long? {
        if (db.dbInfoDao().get(key)?.value == doneVersion) return null

        @OptIn(ExperimentalResourceApi::class)
        val bytes = Res.readBytes(file)
        val seeds = json.decodeFromString<List<SeedFood>>(bytes.decodeToString())
        val now = Clock.System.now().toEpochMilliseconds()

        val foods = seeds.map { s ->
            FoodEntity(
                id = s.id,
                source = FoodSource.valueOf(s.source),
                sourceRef = s.sourceRef,
                namePt = s.namePt,
                nameEn = s.nameEn,
                brand = s.brand,
                kcal = s.kcal,
                proteinG = s.proteinG,
                carbsG = s.carbsG,
                sugarsG = s.sugarsG,
                fatG = s.fatG,
                satFatG = s.satFatG,
                fiberG = s.fiberG,
                sodiumMg = s.sodiumMg,
                microsJson = s.micros?.let { m ->
                    Json.encodeToString(kotlinx.serialization.serializer(), m)
                },
                servingName = s.servingName,
                servingGrams = s.servingGrams,
                verified = s.verified,
                updatedAt = now,
                dirty = false,
            )
        }
        val fts = foods.map { f ->
            FoodFtsEntity(
                foodId = f.id,
                searchText = TextNormalize.normalize("${f.namePt} ${f.nameEn} ${f.brand.orEmpty()}"),
            )
        }

        // Em blocos de 400 por causa do limite de variáveis de uma instrução SQLite: os
        // ficheiros têm milhares de alimentos, e uma inserção única rebentava.
        foods.chunked(400).forEach { db.foodDao().insertAll(it) }

        // Apagar as linhas de pesquisa antes de as reinserir: o FTS4 não tem chave
        // primária, e reimportar sem isto duplicava cada alimento nos resultados.
        foods.map { it.id }.chunked(400).forEach { db.foodDao().deleteFtsIn(it) }
        fts.chunked(400).forEach { db.foodDao().insertFtsAll(it) }
        // A marca fica em último: uma importação interrompida a meio recomeça na abertura
        // seguinte em vez de dar o catálogo por semeado.
        db.dbInfoDao().upsert(DbInfo(key, doneVersion))
        return now
    }

    companion object {
        private const val KEY = "seed_foods_imported"

        private const val DONE = "v2"
        private const val KEY_PT = "seed_foods_pt_imported"
        private const val DONE_PT = "v1"
        private const val KEY_PT2 = "seed_foods_pt2_imported"
        private const val DONE_PT2 = "v1"
        private const val KEY_PT3 = "seed_foods_pt3_imported"
        private const val DONE_PT3 = "v1"
        private const val KEY_TCA = "seed_foods_tca_imported"
        private const val DONE_TCA = "v1"
        private const val KEY_TCA_DUPES = "curated_dupes_tca_pruned"
        private const val DONE_TCA_DUPES = "v1"
        private const val KEY_CLEAN = "usda_names_cleaned"
        private const val DONE_CLEAN = "v1"
        private const val KEY_FTS_DEDUPE = "fts_deduped"
        private const val DONE_FTS_DEDUPE = "v1"
        private const val KEY_PT_NAMES = "curated_names_restored"
        private const val DONE_PT_NAMES = "v1"
        private const val KEY_PT_DUPES = "curated_dupes_pruned"
        private const val DONE_PT_DUPES = "v1"
        private const val KEY_VERIFIED = "analysed_verified"
        private const val DONE_VERIFIED = "v1"

        const val KEY_REBUILT_DAY = "catalogue_rebuilt_day"

        private const val KEY_PT_MICROS = "curated_micros_enriched"

        private const val DONE_PT_MICROS = "v2"

        private val PT_SEED_FILES = listOf(
            "files/seed_foods_pt.json" to false,
            "files/seed_foods_pt2.json" to false,
            "files/seed_foods_pt3.json" to false,
            "files/seed_foods_tca.json" to false,
            "files/seed_foods.json" to true,
        )
        private const val KEY_LIQUID = "drinks_marked"
        private const val DONE_LIQUID = "v3"
        private const val KEY_CLEAN2 = "usda_names_cleaned_v2"
        private const val DONE_CLEAN2 = "v2"
    }
}

internal fun normalizeForDedupe(name: String): String {
    val semAcentos = name.map { ch ->
        when (ch) {
            'á', 'à', 'â', 'ã', 'ä' -> 'a'
            'é', 'è', 'ê', 'ë' -> 'e'
            'í', 'ì', 'î', 'ï' -> 'i'
            'ó', 'ò', 'ô', 'õ', 'ö' -> 'o'
            'ú', 'ù', 'û', 'ü' -> 'u'
            'ç' -> 'c'
            'ñ' -> 'n'
            else -> ch
        }
    }.joinToString("")
    return semAcentos.lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
