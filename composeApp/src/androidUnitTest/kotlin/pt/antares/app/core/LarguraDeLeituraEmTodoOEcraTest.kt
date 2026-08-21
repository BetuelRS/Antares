package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Todo o ecrã tem uma resposta para uma janela larga.
 *
 * Sem isso, um formulário num tablet estica-se por 1200 dp e o olho perde a linha ao voltar
 * à esquerda. Não dá erro, não dá aviso, e num telemóvel — que é onde se desenvolve — parece
 * perfeito. Trinta e sete ficheiros escreviam a chamada à mão, o que quer dizer que um ecrã
 * novo ficava sem ela por esquecimento e ninguém dava por isso até alguém abrir a app noutro
 * tamanho.
 *
 * **O que este teste exige é o resultado, e não o caminho.** Há duas respostas certas para
 * uma janela larga, e as duas contam: **limitar a linha** — com o [AntaresScreen], que o faz
 * por omissão, ou com a chamada à mão — ou **entregar a largura a um contentor que a
 * transforma em colunas**, que é o que a `ListaAdaptavel` e a `GrelhaDeCartoes` fazem. Uma
 * lista de alimentos numa coluna de 1200 dp seria tão má como um parágrafo com essa largura;
 * em três colunas é melhor do que em qualquer largura fixa.
 *
 * O que não é resposta nenhuma é não fazer nem uma coisa nem outra.
 */
class LarguraDeLeituraEmTodoOEcraTest {

    /**
     * Ecrãs sem resposta nenhuma, e porquê. São os que ocupam a janela toda por natureza:
     * um mapa, uma câmara, um esquema próprio.
     */
    private val semLargura = mapOf(
        "BarcodeScanScreen.kt" to "a pré-visualização da câmara ocupa a janela toda",
        "RunLiveScreen.kt" to "o mapa da corrida a decorrer ocupa a janela toda",
        "OnboardingScreen.kt" to "o arranque tem o seu próprio esquema, com o campo estrelado",
    )

    /** Os contentores que respondem à largura com colunas, em vez de com uma linha comprida. */
    private val contentoresAdaptaveis =
        listOf("ListaAdaptavel", "GrelhaDeCartoes", "PainelDeListaEDetalhe")

    private val ecras = File("src/commonMain/kotlin/pt/antares/app/feature")
        .walkTopDown()
        .filter { it.extension == "kt" }
        // Um ficheiro é um ecrã quando declara um composable com `Screen` no nome e recebe
        // um `onBack` ou é um separador. Os ficheiros de secções e de cartões não contam.
        .filter { Regex("""fun [A-Z][A-Za-z]*Screen\(""").containsMatchIn(it.readText()) }
        .toList()

    @Test
    fun `ha ecras para verificar`() {
        assertTrue(ecras.size > MINIMO_PLAUSIVEL, "só encontrei ${ecras.size} ecrãs — a leitura partiu-se")
    }

    @Test
    fun `todo o ecra responde a uma janela larga`() {
        val semNada = ecras.filter { f ->
            val texto = f.readText()
            val limitaALinha =
                texto.contains("larguraDeLeitura()") || texto.contains("AntaresScreen(")
            val daAsColunas = contentoresAdaptaveis.any { texto.contains(it) }
            !limitaALinha && !daAsColunas && f.name !in semLargura
        }.map { it.name }

        assertTrue(
            semNada.isEmpty(),
            "estes ecrãs esticam-se por 1200 dp num tablet: $semNada. Ou limitas a linha " +
                "(AntaresScreen, ou larguraDeLeitura na lista), ou entregas a largura a uma " +
                "ListaAdaptavel — ou escreve-os neste teste com a razão de ocuparem a " +
                "janela toda.",
        )
    }

    @Test
    fun `as excepcoes ainda existem e ainda sao ecras`() {

        // Uma excepção que aponta para um ficheiro apagado é uma excepção que ninguém volta a
        // rever, e a lista deixa de dizer a verdade sobre a app.
        val nomes = ecras.map { it.name }.toSet()
        val fantasmas = semLargura.keys - nomes
        assertTrue(fantasmas.isEmpty(), "excepções para ecrãs que já não existem: $fantasmas")
    }

    @Test
    fun `o AntaresScreen aplica mesmo a largura`() {

        // É o que dá valor ao caminho curto. Se sair de lá, os ecrãs migrados perdem-na todos
        // ao mesmo tempo e o teste de cima continua a passar, porque eles chamam o
        // `AntaresScreen` — que é exactamente o que deixaria de bastar.
        val andaime = File(
            "src/commonMain/kotlin/pt/antares/app/core/designsystem/components/Scaffold.kt",
        ).readText()
        val corpo = andaime.substringAfter("fun AntaresScreen(").substringBefore("fun AntaresTopBar")

        assertTrue(
            corpo.contains(".larguraDeLeitura()"),
            "o AntaresScreen deixou de aplicar a largura de leitura",
        )
    }

    private companion object {
        const val MINIMO_PLAUSIVEL = 25
    }
}
