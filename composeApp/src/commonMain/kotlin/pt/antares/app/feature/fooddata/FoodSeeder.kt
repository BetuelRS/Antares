package pt.antares.app.feature.fooddata

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pt.antares.app.core.catalogo.ArmazemDoCatalogo
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

    /**
     * Os micronutrientes tal como o oleoduto os escreveu, **sem os interpretar aqui**.
     *
     * O sódio e a fibra vêm aqui dentro desde a v28, e não em campo próprio: têm meta
     * diária, e um nutriente com meta é um micronutriente como os outros.
     *
     * Não é um mapa de números porque desde a v29 um valor pode ser um número ou um estado
     * — `"<0.1"`, `"vestigios"` — e um mapa de números falharia a leitura do ficheiro
     * inteiro por causa de uma cadeia. Falharia em silêncio, ainda por cima: o semeador
     * apanha a exceção, devolve nulo, e a app abre sem catálogo nenhum.
     *
     * Guardar o objeto e passá-lo à frente também poupa desmontá-lo e voltar a montá-lo:
     * o que a base guarda é este mesmo texto. Quem o lê é o [microsDeJson], que devolve só
     * os números, e o [estadosDeJson], que devolve o resto.
     */
    val micros: JsonObject? = null,

    /** A família de confeção, ou nulo quando o alimento não se cozinha. Ver [FoodEntity]. */
    val familia: String? = null,

    /** As outras maneiras de medir este alimento. A principal vai no `servingName`. */
    val porcoes: List<PorcaoDoCatalogo>? = null,
    val servingName: String? = null,
    val servingGrams: Double? = null,
    val isLiquid: Boolean = false,
    val densidade: Double? = null,
    val verified: Boolean = false,
)

/** Uma maneira de medir um alimento: o rótulo e quanto pesa. */
@Serializable
data class PorcaoDoCatalogo(val nome: String, val gramas: Double)

/**
 * Um alimento que foi fundido noutro, e para onde vai quem o tinha.
 *
 * **A lápide não é arrumação.** O diário guarda cópia da nutrição no momento do registo, e
 * por isso os dias passados nunca mudam — mas o favorito guarda só o identificador, e um
 * ingrediente de receita também. Sem a lápide, fundir dois alimentos tirava um favorito a
 * alguém, ou um ingrediente a uma receita, sem aviso nenhum.
 */
@Serializable
data class LapideDoCatalogo(val id: String, val sucessor: String)

/** O catálogo inteiro, com a versão à cabeça — é ela que decide se se lê o resto. */
@Serializable
data class Catalogo(
    val versao: Int,
    val alimentos: List<AlimentoDoCatalogo>,
    val lapides: List<LapideDoCatalogo> = emptyList(),
)

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
    microsJson = alimento.micros?.toString(),
    familia = alimento.familia,
    porcoesJson = alimento.porcoes?.let { Json.encodeToString(it) },
    servingName = alimento.servingName,
    servingGrams = alimento.servingGrams,
    isLiquid = alimento.isLiquid,
    densidade = alimento.densidade,
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
    private val armazem: ArmazemDoCatalogo,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfNeeded() = withContext(io) {
        val instalada = db.dbInfoDao().get(KEY_CATALOGO)?.value?.toIntOrNull() ?: NENHUMA
        val descarregada = db.dbInfoDao().get(KEY_DESCARREGADO)?.value?.toIntOrNull() ?: NENHUMA

        // A pergunta faz-se antes de abrir o ficheiro, e não depois: são cinco megabytes,
        // e lê-los a cada arranque só para concluir que não há nada a fazer era o custo
        // que a marca existe para evitar.
        if (instalada >= maxOf(VERSAO_DO_CATALOGO, descarregada)) return@withContext

        if (descarregada > VERSAO_DO_CATALOGO && semearDoDisco()) return@withContext

        if (instalada >= VERSAO_DO_CATALOGO) return@withContext

        val catalogo = lerOuRegistar(FICHEIRO) {
            json.decodeFromString<Catalogo>(it.decodeToString())
        } ?: return@withContext

        instalar(catalogo)
    }

    /**
     * Semeia o catálogo que desceu, se ele lá estiver e abrir.
     *
     * **Falhar aqui não pode ser um ciclo.** Um ficheiro que não abre faz esquecer que
     * existe uma descarga — sem isso, cada abertura tentava de novo, falhava de novo, e a
     * app ficava presa a uma versão que nunca chegava a entrar. O que já está semeado não
     * se toca: o catálogo antigo continua exactamente como estava.
     */
    private suspend fun semearDoDisco(): Boolean {
        val catalogo = armazem.ler()?.let { bytes ->
            tentar(onde = "FoodSeeder: ${armazem.caminho()}") {
                json.decodeFromString<Catalogo>(bytes.decodeToString())
            }
        }
        if (catalogo == null) {
            db.dbInfoDao().upsert(DbInfo(KEY_DESCARREGADO, NENHUMA.toString()))
            return false
        }

        instalar(catalogo)

        // Só aqui — depois de a app ter aberto uma vez com o novo e o ter semeado — é que o
        // anterior deixa de fazer falta. É o que torna a troca reversível sem código de
        // reversão.
        armazem.esquecerAnterior()
        return true
    }

    private suspend fun instalar(catalogo: Catalogo) {
        val agora = Clock.System.now().toEpochMilliseconds()

        val alimentos = catalogo.alimentos.map { linhaDe(it, agora) }

        // Em blocos por causa do limite de variáveis de uma instrução SQLite: são oito mil
        // alimentos, e uma escrita única rebentava.
        alimentos.chunked(LOTE_DE_ESCRITA).forEach { db.foodDao().insertAll(it) }

        // As lápides **antes** da poda: é o que faz um favorito seguir para o sucessor em vez
        // de segurar o alimento fundido só porque alguém lhe tinha tocado.
        seguirLapides(catalogo.lapides)

        db.foodDao().podarCatalogoAnterior(agora)
        db.foodDao().pruneOrphanFts()

        val linhasDePesquisa = alimentos.map { f ->
            FoodFtsEntity(
                foodId = f.id,
                searchText = textoDePesquisa(f.namePt, f.nameEn, f.brand),
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
     * Manda o que era da pessoa seguir o alimento que foi fundido noutro.
     *
     * O diário não precisa: guarda cópia da nutrição no momento do registo, e um dia passado
     * continua a dizer o que dizia. **O que precisa é o que guarda só o identificador** — o
     * favorito, a última porção, o ingrediente de uma receita, a linha de uma refeição
     * guardada. Sem isto, fundir dois alimentos tirava um favorito ou um ingrediente a
     * alguém, sem aviso nenhum e sem forma de o recuperar.
     *
     * Uma lápide para um alimento que a pessoa não tinha não faz nada, e é o caso comum.
     */
    private suspend fun seguirLapides(lapides: List<LapideDoCatalogo>) {
        for (lapide in lapides) {
            db.foodMarkDao().seguir(lapide.id, lapide.sucessor)
            db.foodDao().seguirEmReceitas(lapide.id, lapide.sucessor)
            db.foodDao().seguirEmRefeicoes(lapide.id, lapide.sucessor)
        }
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
        tentar(onde = "FoodSeeder: $file") { ler(Res.readBytes(file)) }

    /** Faz o que lhe pedirem e, se rebentar, deixa rasto em vez de o engolir. */
    private suspend fun <T> tentar(onde: String, bloco: suspend () -> T): T? =
        try {
            bloco()
        } catch (e: Throwable) {
            crashes.registarEngolida(
                onde = onde,
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
        const val VERSAO_DO_CATALOGO = 5

        private const val NENHUMA = 0
        private const val KEY_CATALOGO = "catalogo_versao"

        /**
         * A versão do catálogo que desceu da rede e está no armazém, à espera de ser
         * semeada. Zero — ou ausente — quer dizer que o único catálogo que a app tem é o
         * que veio dentro do APK.
         *
         * Vive aqui, e não no [ActualizadorDoCatalogo], porque é este ficheiro que decide
         * qual dos dois catálogos se lê. Quem a escreve é o actualizador; quem a apaga é o
         * [semearDoDisco], quando o que desceu não abre.
         */
        const val KEY_DESCARREGADO = "catalogo_descarregado"

        const val KEY_REBUILT_DAY = "catalogue_rebuilt_day"

        // Lotes de escrita, para não montar uma instrução com dezenas de milhares de
        // parâmetros de uma vez.
        private const val LOTE_DE_ESCRITA = 400
    }
}
