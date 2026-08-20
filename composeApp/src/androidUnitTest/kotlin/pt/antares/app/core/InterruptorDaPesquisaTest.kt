package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O interruptor da pesquisa em linha só vale se não houver por onde lhe fugir.
 *
 * Vive dentro do `OffRepository`, nas duas funções públicas que falam com a Open Food Facts,
 * e não nos ecrãs — um ecrã novo que se esqueça de o consultar não é possível, porque não há
 * outro caminho até à rede. Este teste guarda esse desenho: se alguém puser um `OffApi` a
 * ser usado noutro sítio, ou chamar o caminho interno de fora, a promessa deixa de valer sem
 * nada estalar em tempo de execução.
 */
class InterruptorDaPesquisaTest {

    private val fontes = File("src/commonMain/kotlin")
        .walkTopDown()
        .filter { it.extension == "kt" }
        .toList()

    @Test
    fun `so o repositorio fala com a Open Food Facts`() {
        val quemUsaApi = fontes
            .filter { it.readText().contains("OffApi") }
            .map { it.name }
            .sorted()

        assertEquals(
            listOf("NetworkModule.kt", "OffApi.kt", "OffRepository.kt"),
            quemUsaApi,
            "alguém passou a usar o OffApi fora do repositório, e o interruptor não o cobre",
        )
    }

    @Test
    fun `o caminho sem interruptor nao e chamado de fora`() {

        // O `searchOnline` é o pedido cru, sem a pergunta do interruptor. É interno para os
        // testes o poderem exercitar sozinho; em produção chama-se sempre o `procurar`.
        val chamadores = fontes
            .filter { it.name != "OffRepository.kt" }
            .filter { it.readText().contains("searchOnline(") }
            .map { it.name }

        assertTrue(
            chamadores.isEmpty(),
            "estes chamam o caminho sem interruptor: $chamadores",
        )
    }

    @Test
    fun `as duas portas perguntam pelo interruptor`() {
        val repositorio = File(
            "src/commonMain/kotlin/pt/antares/app/feature/fooddata/OffRepository.kt",
        ).readText()

        // Uma por função pública. Se uma delas perder a linha, fica um caminho aberto.
        assertEquals(
            2,
            Regex("""if \(!emLinha\(\)\)""").findAll(repositorio).count(),
            "uma das portas da Open Food Facts deixou de consultar o interruptor",
        )
    }

    @Test
    fun `desligada nao se confunde com falha de rede`() {
        val repositorio = File(
            "src/commonMain/kotlin/pt/antares/app/feature/fooddata/OffRepository.kt",
        ).readText()

        // Dizer «sem rede» a quem desligou o interruptor é a app a culpar a ligação de uma
        // escolha da pessoa. São estados distintos, e têm de continuar a sê-lo.
        assertTrue(repositorio.contains("data object Desligada : OffFetch"))
        assertTrue(repositorio.contains("data object Desligada : OffSearch"))
        assertTrue(repositorio.contains("data object SemRede : OffSearch"))
    }
}
