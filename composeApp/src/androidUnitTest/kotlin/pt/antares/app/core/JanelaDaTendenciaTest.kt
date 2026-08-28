package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A janela da tendência tem de chegar a **todos** os sítios que calculam o ritmo.
 *
 * **Nasce de um defeito que o `estudo/motor/03-peso-tendencia-e-projecao.md` seguiu até ao
 * fim.** Ele custou um valor inteiro à nota da honestidade daquele bloco, e a frase dele é
 * dura: «é um interruptor que não está ligado a nada… numa app cujo argumento central é
 * mostrar as contas, um controlo decorativo é a pior espécie de defeito».
 *
 * Depois disso ficou meio ligado, que é pior do que o defeito original: o perfil de saúde
 * passou a ler a janela escolhida e o ecrã do progresso continuou com a de omissão. **O
 * mesmo ritmo semanal, calculado com janelas diferentes em dois ecrãs**, sem nada a explicar
 * a discordância a quem olhasse para os dois.
 *
 * O que este teste guarda é que nenhuma chamada a `weeklyRateKg` fica sem janela. Não prova
 * que a janela vem do perfil — isso vê-se nos testes dos ViewModels — mas apanha a forma
 * exacta como isto se partiu: alguém acrescenta um ecrã que mostra o ritmo, chama a função
 * com a lista e mais nada, e herda a omissão em silêncio.
 */
class JanelaDaTendenciaTest {

    private val fontes = File("src/commonMain/kotlin")
        .walkTopDown()
        .filter { it.extension == "kt" }
        .toList()

    @Test
    fun `ninguem calcula o ritmo sem dizer que janela usa`() {
        // A própria declaração da função é a excepção: é lá que a omissão vive.
        val semJanela = fontes
            .filter { it.name != "WeightTrend.kt" }
            .flatMap { ficheiro ->
                Regex("""weeklyRateKg\((?!\s*\))""")
                    .findAll(ficheiro.readText())
                    .map { achado ->
                        // A chamada pode ocupar várias linhas; olha-se para o que vem a
                        // seguir até fechar os parênteses do primeiro nível.
                        ficheiro.name to trechoDaChamada(ficheiro.readText(), achado.range.last)
                    }
            }
            .filterNot { (_, trecho) -> "windowDays" in trecho }
            .map { it.first }
            .distinct()
            .sorted()

        assertTrue(
            semJanela.isEmpty(),
            "estes calculam o ritmo com a janela de omissão em vez da que a pessoa escolheu: " +
                semJanela.joinToString(),
        )
    }

    /** O texto da chamada, do parêntese de abrir ao de fechar do mesmo nível. */
    private fun trechoDaChamada(texto: String, inicio: Int): String {
        var profundidade = 0
        for (i in inicio until texto.length) {
            when (texto[i]) {
                '(' -> profundidade++
                ')' -> {
                    profundidade--
                    if (profundidade == 0) return texto.substring(inicio, i + 1)
                }
            }
        }
        return texto.substring(inicio)
    }
}
