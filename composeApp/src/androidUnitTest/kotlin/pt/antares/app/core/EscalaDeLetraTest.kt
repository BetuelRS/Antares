package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * O que a escala de letra do sistema parte, e que os testes não veem.
 *
 * A app é para ser usada por quem precisa de letra grande, e a 200 % — o máximo que o Android
 * oferece — os ecrãs deixam de ser os mesmos: o que numa linha cabia passa a duas, e o que tem
 * altura fixa corta a segunda a meio das letras.
 *
 * **E não há teste de composição que os apanhe.** O Robolectric não tem tipos de letra: mede a
 * frase «Corpo inteiro A · terminado» como 27 dp de largura, e por isso ela nunca muda de linha
 * lá dentro por muito que se lhe suba a escala — medido, ao tentar escrever este teste como um
 * `runComposeUiTest`. Estes defeitos encontram-se **no aparelho** (regra D3), e o que fica aqui
 * é a defesa contra alguém desfazer a correcção sem dar por isso.
 */
class EscalaDeLetraTest {

    private fun fonte(caminho: String): String =
        File("src/commonMain/kotlin/pt/antares/app/$caminho").readText()

    /**
     * A barra do topo é de altura fixa: um título de duas linhas não a faz crescer, faz-lhe
     * cortar a segunda. Visto a correr, a 200 %, no ecrã da sessão — «Corpo inteiro A» ficava
     * «Corpo» e um segundo risco de letras meio comidas por baixo.
     *
     * Uma linha com reticências não diz o nome todo, mas diz que há mais nome; duas linhas
     * cortadas dizem que a app se avariou. Isto vale para os quarenta e tal ecrãs de uma vez,
     * porque todos passam pelo mesmo `AntaresTopBar`.
     */
    @Test
    fun `o titulo da barra do topo e de uma linha`() {
        val barra = fonte("core/designsystem/components/Scaffold.kt")
            .substringAfter("fun AntaresTopBar(")
            .substringBefore("navigationIcon")

        assertTrue(
            "maxLines = 1" in barra && "TextOverflow.Ellipsis" in barra,
            "o título do `AntaresTopBar` perdeu o `maxLines = 1` com reticências: a 200 % de " +
                "escala de letra passa a duas linhas e a barra corta a segunda a meio.",
        )
    }

    /**
     * Dois textos lado a lado numa linha, e só o segundo com `weight`: o primeiro come a
     * largura toda e ao segundo sobra uma coluna de uma letra. Foi o que aconteceu ao «1RM
     * est. · 114 kg» do ecrã da sessão, que a 200 % saía na vertical, uma letra por linha.
     *
     * A correcção foi juntá-los num texto só — o que não cabe muda de linha por palavras.
     */
    @Test
    fun `o alvo e o 1RM sao um texto so`() {
        val linha = fonte("feature/workout/ui/WorkoutSessionScreen.kt")
            .substringAfter("private fun LinhaDoAlvo(")
            .substringBefore("FilterChip")

        assertTrue(
            linha.count { it == '\n' } > 0 && Regex("""\bText\(""").findAll(linha).count() == 1,
            "o alvo e o 1RM voltaram a ser dois textos na mesma linha: sem `weight` no " +
                "primeiro, a 200 % o segundo sai uma letra por linha.",
        )
    }
}
