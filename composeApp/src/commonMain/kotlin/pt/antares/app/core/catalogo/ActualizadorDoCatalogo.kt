package pt.antares.app.core.catalogo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pt.antares.app.core.database.DbInfo
import pt.antares.app.core.database.DbInfoDao
import pt.antares.app.core.util.sha256
import pt.antares.app.feature.fooddata.Catalogo
import pt.antares.app.feature.fooddata.FoodSeeder

/** O que a procura encontrou. Não descarrega nada: é só o manifesto. */
sealed interface ProcuraDeCatalogo {
    data class Ha(val manifesto: ManifestoDoCatalogo, val instalada: Int) : ProcuraDeCatalogo
    data class EmDia(val instalada: Int) : ProcuraDeCatalogo
    data object SemResposta : ProcuraDeCatalogo
}

/**
 * Como acabou a instalação. Cada recusa diz **porquê** — «erro» não é resposta para quem
 * está a olhar para o ecrã, e menos ainda para quem for ler isto daqui a seis meses.
 *
 * Em todas as recusas o catálogo instalado fica exactamente como estava.
 */
sealed interface ResultadoDaActualizacao {
    data class Instalado(val versao: Int, val alimentos: Int) : ResultadoDaActualizacao

    /** A descarga não chegou ao fim, ou o manifesto não respondeu. */
    data object SemResposta : ResultadoDaActualizacao

    /** O resumo do que desceu não bate com o do manifesto — ficheiro cortado ou trocado. */
    data object ResumoNaoBate : ResultadoDaActualizacao

    /** O ficheiro não abre: não é JSON, ou não é um catálogo. */
    data object FicheiroIlegivel : ResultadoDaActualizacao

    /** A versão de dentro do ficheiro não é mais recente do que a instalada. */
    data object NaoAvanca : ResultadoDaActualizacao

    /** Não houve onde o escrever, ou a troca não deu. O antigo continua no lugar. */
    data object NaoSeGuardou : ResultadoDaActualizacao
}

/**
 * O catálogo que se atualiza sem passar pela Play Store.
 *
 * Até aqui, corrigir as kcal de um alimento custava uma versão publicada: compilar, assinar,
 * esperar pela revisão. É por isso que as correções não se faziam.
 *
 * **A ordem das verificações é a garantia.** Nada toca no disco antes de o que desceu ter
 * passado por três provas — o resumo bate, o ficheiro abre, e a versão avança. Uma
 * actualização que corre mal não pode deixar a app sem catálogo, e nenhum destes caminhos
 * chega ao armazém.
 *
 * **Só avança, nunca recua.** Sem isto, uma release velha servida por engano — ou um
 * manifesto trocado — desfazia meses de curadoria sem ninguém dar por nada.
 */
class ActualizadorDoCatalogo(
    private val api: ApiDoCatalogo,
    private val armazem: ArmazemDoCatalogo,
    private val dbInfo: DbInfoDao,
    private val io: CoroutineDispatcher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * A versão mais recente que a app tem, venha do APK ou de uma descarga anterior. É esta
     * — e não a que está semeada na base — que decide se vale a pena descarregar: um
     * catálogo já descarregado e ainda por semear não se descarrega outra vez.
     */
    suspend fun versaoInstalada(): Int = withContext(io) {
        val descarregada = dbInfo.get(FoodSeeder.KEY_DESCARREGADO)?.value?.toIntOrNull() ?: 0
        maxOf(FoodSeeder.VERSAO_DO_CATALOGO, descarregada)
    }

    suspend fun procurar(): ProcuraDeCatalogo = withContext(io) {
        val manifesto = runCatching { api.manifesto() }.getOrNull()
            ?: return@withContext ProcuraDeCatalogo.SemResposta
        val instalada = versaoInstalada()
        if (manifesto.versao > instalada) {
            ProcuraDeCatalogo.Ha(manifesto, instalada)
        } else {
            ProcuraDeCatalogo.EmDia(instalada)
        }
    }

    suspend fun instalar(manifesto: ManifestoDoCatalogo): ResultadoDaActualizacao =
        withContext(io) {
            val bytes = runCatching { api.descarregar(manifesto.url) }.getOrNull()
                ?: return@withContext ResultadoDaActualizacao.SemResposta

            // O resumo primeiro, porque é a prova mais barata e a que apanha o caso mais
            // comum: uma descarga cortada a meio parece um ficheiro inteiro.
            if (sha256(bytes) != manifesto.sha256.lowercase()) {
                return@withContext ResultadoDaActualizacao.ResumoNaoBate
            }

            val catalogo = runCatching {
                json.decodeFromString<Catalogo>(bytes.decodeToString())
            }.getOrNull() ?: return@withContext ResultadoDaActualizacao.FicheiroIlegivel

            // A versão que conta é a de dentro do ficheiro, não a do manifesto: é o ficheiro
            // que vai ser semeado, e um manifesto que se engane a si próprio não pode fazer
            // a app recuar.
            if (catalogo.versao <= versaoInstalada()) {
                return@withContext ResultadoDaActualizacao.NaoAvanca
            }

            if (!armazem.guardarProvisorio(bytes)) {
                return@withContext ResultadoDaActualizacao.NaoSeGuardou
            }
            if (!armazem.trocar()) {
                armazem.descartarProvisorio()
                return@withContext ResultadoDaActualizacao.NaoSeGuardou
            }

            // A marca fica em último, e é ela que faz o [FoodSeeder] ir ao disco em vez de
            // ao APK na abertura seguinte. Escrita antes da troca, uma troca falhada
            // mandava-o ler um ficheiro que não estava lá.
            dbInfo.upsert(DbInfo(FoodSeeder.KEY_DESCARREGADO, catalogo.versao.toString()))
            ResultadoDaActualizacao.Instalado(catalogo.versao, catalogo.alimentos.size)
        }
}
