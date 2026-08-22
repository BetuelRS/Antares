package pt.antares.app.feature.fooddata

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
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodFtsEntity
import pt.antares.app.core.database.entities.FoodNutrientEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.nutrition.microsDeJson
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.generated.resources.Res

/** Um alimento tal como o oleoduto o escreve. */
@Serializable
data class AlimentoDoCatalogo(
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
    val isLiquid: Boolean = false,
    val verified: Boolean = false,
)

/** O catálogo inteiro, com a versão à cabeça — é ela que decide se se lê o resto. */
@Serializable
data class Catalogo(val versao: Int, val alimentos: List<AlimentoDoCatalogo>)

/**
 * Monta a linha que vai ser gravada, tal como o oleoduto a escreveu.
 *
 * Na 2.4.0 esta função também transportava o que era da pessoa — o favorito, o uso, a porção
 * e a lápide — porque essas colunas viviam dentro da linha do alimento e a escrita do
 * catálogo gravava a linha inteira por cima. **Na v27 saíram daqui** para a `food_marca`, e o
 * risco deixou de existir em vez de ser contornado: o que não vive na linha do alimento não
 * pode ser apagado ao escrevê-la.
 */
internal fun linhaDe(
    alimento: AlimentoDoCatalogo,
    agora: Long,
): FoodEntity = FoodEntity(
    id = alimento.id,
    source = FoodSource.valueOf(alimento.source),
    sourceRef = alimento.sourceRef,
    namePt = alimento.namePt,
    nameEn = alimento.nameEn,
    brand = alimento.brand,
    kcal = alimento.kcal,
    proteinG = alimento.proteinG,
    carbsG = alimento.carbsG,
    sugarsG = alimento.sugarsG,
    fatG = alimento.fatG,
    satFatG = alimento.satFatG,
    fiberG = alimento.fiberG,
    sodiumMg = alimento.sodiumMg,
    microsJson = alimento.micros?.let { Json.encodeToString(it) },
    servingName = alimento.servingName,
    servingGrams = alimento.servingGrams,
    isLiquid = alimento.isLiquid,
    verified = alimento.verified,
    updatedAt = agora,
    deleted = false,
)

/**
 * Instala o catálogo de alimentos, e faz uma pergunta só: **o que está gravado é mais
 * antigo do que o que veio no ficheiro?**
 *
 * Até à 2.3.0 eram dezoito passos por ordem fixa — cinco ficheiros importados e treze
 * correções — e cada alimento mal escrito que se quisesse corrigir acrescentava um
 * décimo-nono. O estado final dependia do caminho: quem instalava a app hoje passava por
 * passos diferentes de quem a tinha desde março, e as correções não podiam ser fundidas
 * nem reordenadas sem mudar o resultado de alguém.
 *
 * O catálogo passa a ser construído fora da app, por `tools/catalogo/construir.mjs`, e a
 * chegar cozido num ficheiro só. **Corrigir um alimento deixa de custar código.** Aqui só
 * se escreve o que veio e se apaga o que deixou de vir, e por isso todas as instalações
 * convergem para o mesmo estado, venham de onde vierem.
 */
class FoodSeeder(
    private val db: AntaresDb,
    private val io: CoroutineDispatcher,
    private val crashes: CrashStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfNeeded() = withContext(io) {
        val instalada = db.dbInfoDao().get(KEY_CATALOGO)?.value?.toIntOrNull() ?: NENHUMA

        // A pergunta faz-se antes de abrir o ficheiro, e não depois: são cinco megabytes,
        // e lê-los a cada arranque só para concluir que não há nada a fazer era o custo
        // que a marca existe para evitar.
        if (instalada >= VERSAO_DO_CATALOGO) return@withContext

        val catalogo = lerOuRegistar(FICHEIRO) {
            json.decodeFromString<Catalogo>(it.decodeToString())
        } ?: return@withContext

        instalar(catalogo)
    }

    private suspend fun instalar(catalogo: Catalogo) {
        val agora = Clock.System.now().toEpochMilliseconds()

        val alimentos = catalogo.alimentos.map { linhaDe(it, agora) }

        // Em blocos por causa do limite de variáveis de uma instrução SQLite: são oito mil
        // alimentos, e uma escrita única rebentava.
        alimentos.chunked(LOTE_DE_ESCRITA).forEach { db.foodDao().insertAll(it) }

        db.foodDao().podarCatalogoAnterior(agora)
        db.foodDao().pruneOrphanFts()

        val linhasDePesquisa = alimentos.map { f ->
            FoodFtsEntity(
                foodId = f.id,
                searchText = TextNormalize.normalize("${f.namePt} ${f.nameEn} ${f.brand.orEmpty()}"),
            )
        }
        // Apagar antes de reinserir: o FTS4 não tem chave primária, e reimportar sem isto
        // duplicava cada alimento nos resultados.
        alimentos.map { it.id }.chunked(LOTE_DE_ESCRITA).forEach { db.foodDao().deleteFtsIn(it) }
        linhasDePesquisa.chunked(LOTE_DE_ESCRITA).forEach { db.foodDao().insertFtsAll(it) }

        reindexarMicros()

        if (db.dbInfoDao().get(KEY_REBUILT_DAY) == null) {
            db.dbInfoDao().upsert(DbInfo(KEY_REBUILT_DAY, todayEpochDay().toString()))
        }

        // A marca fica em último: uma instalação interrompida a meio recomeça na abertura
        // seguinte em vez de dar o catálogo por semeado.
        db.dbInfoDao().upsert(DbInfo(KEY_CATALOGO, catalogo.versao.toString()))
    }

    /**
     * Reconstrói a tabela por nutriente a partir do JSON que acabou de entrar. É derivada:
     * apagá-la inteira e refazê-la não perde nada, e é mais barato do que descobrir o que
     * mudou.
     */
    private suspend fun reindexarMicros() {
        db.foodNutrientDao().clearAll()
        db.foodDao().microsParaIndexar()
            .flatMap { linha -> linhasDe(linha.id, linha.microsJson) }
            .chunked(LOTE_DE_ESCRITA)
            .forEach { db.foodNutrientDao().upsertAll(it) }
    }

    // Valores a zero ficam de fora: um nutriente declarado a zero não é o alimento ser
    // rico nele, e enchia a tabela com linhas que nenhuma pergunta quer.
    private fun linhasDe(foodId: String, microsJson: String?): List<FoodNutrientEntity> =
        microsDeJson(microsJson)
            .filterValues { it > 0 }
            .map { (chave, valor) -> FoodNutrientEntity(foodId = foodId, key = chave, value = valor) }

    /**
     * Lê o ficheiro do catálogo, ou devolve nulo e deixa rasto. Um recurso que não abre é
     * quase sempre um ficheiro que mudou de nome sem alguém reparar, e é um defeito da
     * app — não uma condição do telemóvel de quem a usa.
     */
    @OptIn(ExperimentalResourceApi::class)
    private suspend fun <T> lerOuRegistar(file: String, ler: (ByteArray) -> T): T? =
        try {
            ler(Res.readBytes(file))
        } catch (e: Throwable) {
            crashes.registarEngolida(
                onde = "FoodSeeder: $file",
                erro = e,
                quando = Clock.System.now().toEpochMilliseconds(),
            )
            null
        }

    companion object {
        const val FICHEIRO = "files/catalogo.json"

        /**
         * A versão que esta compilação traz. **Sobe com a do `construir.mjs`**, e o
         * [CatalogoTemVersaoTest] não deixa que uma suba sem a outra: se ficasse para
         * trás, o catálogo novo viajava dentro do APK e não entrava em telemóvel nenhum.
         */
        const val VERSAO_DO_CATALOGO = 1

        private const val NENHUMA = 0
        private const val KEY_CATALOGO = "catalogo_versao"

        const val KEY_REBUILT_DAY = "catalogue_rebuilt_day"

        // Lotes de escrita, para não montar uma instrução com dezenas de milhares de
        // parâmetros de uma vez.
        private const val LOTE_DE_ESCRITA = 400
    }
}
