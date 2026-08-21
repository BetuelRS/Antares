package pt.antares.app.core

import pt.antares.app.core.designsystem.motion.Movimento
import pt.antares.app.core.designsystem.motion.MovimentoDasRotas
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Toda a rota tem um movimento, e é escolhido por alguém.
 *
 * O `MovimentoDasRotas.de()` devolve `MAIS_FUNDO` a um nome que não conheça — para a app
 * nunca ficar sem animação por causa de um nome que mudou de forma. Esse valor por omissão é
 * uma rede de segurança em execução, não uma decisão, e sem este teste seria a maneira mais
 * fácil de o sistema apodrecer: bastava acrescentar rotas e nunca mais pensar nisto.
 *
 * Aqui lê-se o `Routes.kt` e exige-se a lista completa. Uma rota nova sem movimento **falha**,
 * e quem a acrescentou tem de decidir se é um irmão, um degrau, um modo, um mergulho ou um
 * resultado.
 */
class MovimentoDeTodasAsRotasTest {

    private val rotasDeclaradas: Set<String> =
        Regex("""^\s+data (?:object|class) ([A-Za-z]+)""", RegexOption.MULTILINE)
            .findAll(File("src/commonMain/kotlin/pt/antares/app/navigation/Routes.kt").readText())
            .map { it.groupValues[1] }
            .toSet()

    @Test
    fun `o Routes tem rotas para ler`() {

        // Se a expressão deixar de casar, os outros testes passariam sobre um conjunto vazio
        // e não guardariam nada. É a armadilha clássica de um teste que lê ficheiros.
        assertTrue(
            rotasDeclaradas.size > MINIMO_PLAUSIVEL,
            "só encontrei ${rotasDeclaradas.size} rotas no Routes.kt — a leitura partiu-se",
        )
    }

    @Test
    fun `nenhuma rota ficou sem movimento`() {
        val semMovimento = rotasDeclaradas - MovimentoDasRotas.nomes
        assertTrue(
            semMovimento.isEmpty(),
            "estas rotas não têm movimento escolhido: $semMovimento. Decide se cada uma é " +
                "um irmão, um degrau, um modo, um mergulho ou um resultado — o valor por " +
                "omissão existe para a app não ficar parada, não para ninguém decidir.",
        )
    }

    @Test
    fun `nao ha movimentos para rotas que ja nao existem`() {
        val fantasmas = MovimentoDasRotas.nomes - rotasDeclaradas
        assertTrue(fantasmas.isEmpty(), "movimentos de rotas apagadas: $fantasmas")
    }

    @Test
    fun `os cinco separadores sao irmaos`() {

        // É a relação que mais se nota: deslizar entre separadores daria a ideia falsa de que
        // se está a percorrer uma sequência, quando são cinco portas ao mesmo nível.
        for (aba in listOf("Today", "Diary", "Workout", "Run", "Me")) {
            assertEquals(
                Movimento.ENTRE_IRMAOS,
                MovimentoDasRotas.de("pt.antares.app.navigation.Route.$aba"),
                "o separador $aba deixou de ser tratado como irmão dos outros",
            )
        }
    }

    @Test
    fun `os cinco movimentos estao todos em uso`() {

        // Um movimento que ninguém usa é vocabulário morto, e vocabulário morto é a primeira
        // coisa que alguém copia por engano para o sítio errado.
        val usados = rotasDeclaradas
            .map { MovimentoDasRotas.de("pt.antares.app.navigation.Route.$it") }
            .toSet()
        assertEquals(
            Movimento.entries.toSet(),
            usados,
            "há movimentos declarados que nenhuma rota usa",
        )
    }

    @Test
    fun `o nome qualificado com argumentos continua a ser reconhecido`() {

        // O navegador junta os argumentos ao nome da rota — `…Route.FoodDetail/{id}` — e um
        // corte ingénuo pelo último ponto devolveria «FoodDetail/{id}», que não está no mapa.
        assertEquals(
            Movimento.MAIS_FUNDO,
            MovimentoDasRotas.de("pt.antares.app.navigation.Route.FoodDetail/{foodId}"),
        )
        assertEquals(
            Movimento.RESULTADO,
            MovimentoDasRotas.de("pt.antares.app.navigation.Route.CoachReport?semana={semana}"),
        )
    }

    private companion object {
        // As rotas eram 52 quando isto foi escrito. Metade disso já denuncia uma leitura
        // partida sem prender o teste ao número exacto, que muda a cada versão.
        const val MINIMO_PLAUSIVEL = 25
    }
}
