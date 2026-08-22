package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Um cartão só, e uma linha de lista só.
 *
 * O `AntaresCard` coexistia com o `Card` do Material e com o `ListItem`, e a razão era
 * concreta: o nosso não aceitava clique, e quem precisava de um cartão tocável não tinha
 * outra hipótese senão o do Material — e levava com ele cantos, elevação e espaçamento a
 * diferir de ecrã para ecrã sem ninguém ter decidido isso.
 *
 * Resolvida a causa na 2.3.0, este teste guarda o efeito. **Com duas excepções nomeadas**:
 * uma regra sem excepções escritas é uma regra que alguém contorna em silêncio na primeira
 * vez que ela não serve.
 */
class CartaoUnicoTest {

    /**
     * As duas linhas que não passam pela [pt.antares.app.core.designsystem.components.LinhaDaLista],
     * e porquê. Acrescentar um nome aqui é uma decisão que se lê; não acrescentar e usar o
     * `Card` do Material é o teste vermelho.
     */
    private val excepcoes = mapOf(
        "FoodSearchScreen.kt" to
            "o FoodRow tem um distintivo de origem dentro da própria linha do título, e um " +
                "conteúdo à direita que muda entre ícone e caixa de selecção",
        "OnboardingScreen.kt" to
            "o SelectableCard é um controlo de selecção, com contorno e cor próprios para o " +
                "estado escolhido — não é um cartão de conteúdo",
    )

    /**
     * O import inteiro, e por isso a linha inteira: sem isto, o `Card` apanhava também o
     * `CardDefaults` e o `ElevatedCard`. A leitura normaliza os fins de linha porque no
     * Windows os ficheiros são gravados com `CRLF`, e um teste que dependa disso acusa
     * quem mexeu no ficheiro em vez de quem usou o cartão errado.
     */
    private fun File.importa(classe: String): Boolean =
        readText()
            .replace("\r\n", "\n")
            .contains("import androidx.compose.material3.$classe\n")

    private val ecras = File("src/commonMain/kotlin/pt/antares/app/feature")
        .walkTopDown()
        .filter { it.extension == "kt" }
        .toList()

    @Test
    fun `so as excepcoes usam o cartao do Material`() {
        val usam = ecras
            .filter { it.importa("Card") }
            .map { it.name }
            .toSet()

        assertEquals(
            excepcoes.keys,
            usam,
            "a lista de quem usa o Card do Material mudou. Se é um cartão novo, usa o " +
                "AntaresCard — ele aceita clique desde a 2.3.0. Se é mesmo uma excepção, " +
                "escreve-a neste teste com a razão.",
        )
    }

    @Test
    fun `so as excepcoes usam o ListItem`() {
        val usam = ecras
            .filter { it.importa("ListItem") }
            .map { it.name }
            .toSet()

        assertTrue(
            usam.all { it in excepcoes.keys },
            "estes usam o ListItem sem serem excepção declarada: ${usam - excepcoes.keys}",
        )
    }

    @Test
    fun `cada excepcao tem uma razao escrita`() {

        // O ponto da lista não é permitir: é obrigar a escrever porquê. Uma excepção sem
        // razão é indistinguível de um esquecimento daqui a seis meses.
        for ((ficheiro, razao) in excepcoes) {
            assertTrue(
                razao.length > TAMANHO_DE_UMA_RAZAO,
                "$ficheiro está na lista sem uma razão a sério",
            )
        }
    }

    @Test
    fun `o AntaresCard continua a aceitar clique e papel`() {

        // É a causa de tudo isto. Se estes parâmetros saírem, os ecrãs voltam ao Card do
        // Material um a um, e o teste de cima passa a acusar sem dizer porquê.
        val cartao = File(
            "src/commonMain/kotlin/pt/antares/app/core/designsystem/components/Card.kt",
        ).readText()

        assertTrue(cartao.contains("onClick: (() -> Unit)? = null"), "o clique saiu do AntaresCard")
        assertTrue(cartao.contains("role: Role?"), "o papel saiu do AntaresCard")
    }

    private companion object {
        const val TAMANHO_DE_UMA_RAZAO = 40
    }
}
