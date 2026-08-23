package pt.antares.app.core.catalogo

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import pt.antares.app.core.database.DbInfo
import pt.antares.app.core.database.DbInfoDao
import pt.antares.app.core.util.sha256
import pt.antares.app.feature.fooddata.FoodSeeder
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * **Uma actualização que corre mal não pode deixar a app sem catálogo.**
 *
 * O que interessa neste ficheiro não é a descarga correr — isso é o caminho fácil e tem um
 * teste só. O que interessa é **recusar o que devia ser recusado**, e em todas as recusas o
 * catálogo instalado ficar exactamente como estava.
 *
 * Corre contra uma pasta temporária de verdade, com ficheiros e mudanças de nome de verdade.
 * Uma imitação do armazém concordava com o que se espera dela, e o modo de falhar desta peça
 * está justamente no que acontece ao disco quando a troca se parte a meio.
 */
class CatalogoQueDesceTest {

    private val pasta: File = Files.createTempDirectory("catalogo").toFile()
    private val dbInfo = MemoriaDeDbInfo()
    private val armazem = ArmazemDoCatalogo(pasta, Dispatchers.Unconfined)

    @AfterTest
    fun limpar() {
        pasta.deleteRecursively()
    }

    @Test
    fun `o caminho feliz instala, e a marca fica escrita`() = runTest {
        val novo = catalogo(VERSAO_NOVA)
        val resultado = actualizador(novo, manifesto(novo)).instalar(manifesto(novo))

        assertEquals(ResultadoDaActualizacao.Instalado(VERSAO_NOVA, 1), resultado)
        assertEquals(novo, armazem.ler()?.decodeToString())
        assertEquals(VERSAO_NOVA.toString(), dbInfo.valores[FoodSeeder.KEY_DESCARREGADO])
    }

    @Test
    fun `um resumo que nao bate nao chega ao disco`() = runTest {
        instalarPrimeiro()
        val novo = catalogo(VERSAO_NOVA + 1)
        val mentiroso = manifesto(novo).copy(sha256 = sha256("outra coisa".encodeToByteArray()))

        val resultado = actualizador(novo, mentiroso).instalar(mentiroso)

        assertEquals(ResultadoDaActualizacao.ResumoNaoBate, resultado)
        assertEquals(catalogo(VERSAO_NOVA), armazem.ler()?.decodeToString())
        assertEquals(VERSAO_NOVA.toString(), dbInfo.valores[FoodSeeder.KEY_DESCARREGADO])
    }

    @Test
    fun `um ficheiro cortado a meio nao chega ao disco`() = runTest {

        // Cortado **e** com o resumo do que ficou: é o caso que o resumo não apanha, e o
        // único que prova que a leitura do ficheiro também é uma verificação.
        val cortado = catalogo(VERSAO_NOVA + 1).dropLast(TRINTA_CARACTERES)
        instalarPrimeiro()

        val resultado = actualizador(cortado, manifesto(cortado)).instalar(manifesto(cortado))

        assertEquals(ResultadoDaActualizacao.FicheiroIlegivel, resultado)
        assertEquals(catalogo(VERSAO_NOVA), armazem.ler()?.decodeToString())
    }

    @Test
    fun `uma pagina de erro com o resumo certo continua a nao ser um catalogo`() = runTest {
        instalarPrimeiro()
        val pagina = "<html>404</html>"

        val resultado = actualizador(pagina, manifesto(pagina)).instalar(manifesto(pagina))

        assertEquals(ResultadoDaActualizacao.FicheiroIlegivel, resultado)
        assertEquals(catalogo(VERSAO_NOVA), armazem.ler()?.decodeToString())
    }

    @Test
    fun `uma versao igual ou menor e recusada, mesmo com tudo o resto certo`() = runTest {
        instalarPrimeiro()

        for (versao in listOf(VERSAO_NOVA, VERSAO_NOVA - 1, 1)) {
            val velho = catalogo(versao)

            // O manifesto mente sobre a versão para o passo da procura não travar antes: o
            // que se quer provar é que a versão de dentro do ficheiro é a que decide.
            val mentiroso = manifesto(velho).copy(versao = VERSAO_NOVA + 1)
            val resultado = actualizador(velho, mentiroso).instalar(mentiroso)

            assertEquals(ResultadoDaActualizacao.NaoAvanca, resultado, "aceitou a versão $versao")
            assertEquals(catalogo(VERSAO_NOVA), armazem.ler()?.decodeToString())
        }
    }

    @Test
    fun `sem rede nao se mexe em nada`() = runTest {
        instalarPrimeiro()
        val cliente = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })
        val api = ApiDoCatalogo(cliente, "teste")
        val actualizador = ActualizadorDoCatalogo(api, armazem, dbInfo, Dispatchers.Unconfined)

        assertEquals(ProcuraDeCatalogo.SemResposta, actualizador.procurar())
        assertEquals(catalogo(VERSAO_NOVA), armazem.ler()?.decodeToString())
    }

    @Test
    fun `uma troca interrompida a meio repoe o catalogo anterior`() = runTest {
        instalarPrimeiro()

        // O estado exacto em que a app fica se morrer entre as duas mudanças de nome: o
        // instalado já saiu do lugar e o novo ainda não entrou. Sem a reparação, a app
        // abria sem catálogo nenhum e ninguém saberia porquê.
        val instalado = File(pasta, "catalogo.json")
        assertTrue(instalado.renameTo(File(pasta, "catalogo.json.anterior")))

        assertEquals(catalogo(VERSAO_NOVA), armazem.ler()?.decodeToString())
        assertTrue(instalado.exists(), "a reparação não deixou o ficheiro no sítio")
    }

    @Test
    fun `sem descarga nenhuma o armazem esta vazio, e diz que sim`() = runTest {
        assertNull(armazem.ler())
    }

    @Test
    fun `a procura nao descarrega o catalogo`() = runTest {
        val novo = catalogo(VERSAO_NOVA)
        var descargas = 0
        val cliente = HttpClient(
            MockEngine { pedido ->
                val manifesto = pedido.url.toString().endsWith("manifesto.json")
                if (!manifesto) descargas++
                respond(if (manifesto) manifestoEmJson(manifesto(novo)) else novo)
            },
        )
        val actualizador = ActualizadorDoCatalogo(
            ApiDoCatalogo(cliente, "teste", MANIFESTO),
            armazem,
            dbInfo,
            Dispatchers.Unconfined,
        )

        val procura = actualizador.procurar()

        assertTrue(procura is ProcuraDeCatalogo.Ha, "não viu a versão nova: $procura")
        assertEquals(0, descargas, "a procura foi buscar os cinco megabytes sem ninguém pedir")
    }

    @Test
    fun `com o catalogo do APK em dia, a procura nao propoe nada`() = runTest {
        val igual = catalogo(FoodSeeder.VERSAO_DO_CATALOGO)

        val procura = actualizador(igual, manifesto(igual)).procurar()

        assertEquals(ProcuraDeCatalogo.EmDia(FoodSeeder.VERSAO_DO_CATALOGO), procura)
    }

    /** Deixa uma descarga bem sucedida no armazém, para as recusas terem o que estragar. */
    private suspend fun instalarPrimeiro() {
        val primeiro = catalogo(VERSAO_NOVA)
        actualizador(primeiro, manifesto(primeiro)).instalar(manifesto(primeiro))
    }

    private fun actualizador(corpo: String, manifesto: ManifestoDoCatalogo) =
        ActualizadorDoCatalogo(
            ApiDoCatalogo(
                HttpClient(
                    MockEngine { pedido ->
                        val ehManifesto = pedido.url.toString().endsWith("manifesto.json")
                        respond(if (ehManifesto) manifestoEmJson(manifesto) else corpo)
                    },
                ),
                "teste",
                MANIFESTO,
            ),
            armazem,
            dbInfo,
            Dispatchers.Unconfined,
        )

    private fun manifesto(corpo: String) = ManifestoDoCatalogo(
        versao = versaoDe(corpo),
        url = "$BASE/catalogo.json",
        sha256 = sha256(corpo.encodeToByteArray()),
        alimentos = 1,
    )

    private fun manifestoEmJson(m: ManifestoDoCatalogo) =
        """{"versao":${m.versao},"url":"${m.url}","sha256":"${m.sha256}","alimentos":${m.alimentos}}"""

    // A versão sai do corpo para o manifesto do caminho feliz não ter de a repetir. Um corpo
    // que não seja catálogo nenhum não tem versão, e aí vale a mais alta: assim a recusa que
    // o teste espera vem da leitura, e não de um travão anterior.
    private fun versaoDe(corpo: String): Int =
        Regex(""""versao":(\d+)""").find(corpo)?.groupValues?.get(1)?.toInt() ?: (VERSAO_NOVA + 1)

    private fun catalogo(versao: Int) = """{"versao":$versao,"alimentos":[$ALIMENTO]}"""

    private class MemoriaDeDbInfo : DbInfoDao {
        val valores = mutableMapOf<String, String>()
        override suspend fun upsert(info: DbInfo) {
            valores[info.key] = info.value
        }

        override suspend fun get(key: String): DbInfo? = valores[key]?.let { DbInfo(key, it) }
    }

    private companion object {
        const val BASE = "https://github.com/BetuelRS/Antares/releases/latest/download"
        const val MANIFESTO = "$BASE/manifesto.json"

        // Acima da que vem no APK, que é o que faz o actualizador a aceitar de todo.
        val VERSAO_NOVA = FoodSeeder.VERSAO_DO_CATALOGO + 1

        const val TRINTA_CARACTERES = 30

        const val ALIMENTO =
            """{"id":"x","source":"SEED","nameEn":"a","namePt":"a","kcal":1,""" +
                """"proteinG":1.0,"carbsG":1.0,"fatG":1.0}"""
    }
}
