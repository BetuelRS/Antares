package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pt.antares.app.core.designsystem.usaVirgulaDecimal

/**
 * A notificação da corrida escreve os mesmos números que o ecrã, na mesma língua.
 *
 * Nasceu de quatro coisas erradas na mesma função de cinco linhas, e nenhuma delas dava
 * erro. O `RunTrackerService` tinha um `formatKm` próprio que **truncava** em vez de
 * arredondar — na mesma corrida e quase no mesmo instante, o ecrã dizia `0.06 km` e a
 * notificação `0,05 km` —, escrevia a vírgula decimal à mão, media sempre em quilómetros, e
 * era **o único notificador da app** que não passava pelo `appLocalized()`: com a app em
 * português num telemóvel inglês, esta notificação — e só esta — saía em inglês.
 *
 * O KDoc do `RunFormat` já proibia a terceira por escrito — *«um `= METRIC` aqui deixava
 * cada ecrã esquecido a mostrar quilómetros a quem escolheu milhas, sem erro nenhum a
 * avisar»* — e a notificação chegava-lhe **por fora**, com aritmética própria. É a forma de
 * defeito que o `estudo/transversal/02-robustez.md` §3 nomeia: a regra está escrita, e há um
 * caminho que não passa por ela.
 */
class NotificacaoDaCorridaTest {

    private val servico = File(
        "src/androidMain/kotlin/pt/antares/app/feature/running/RunTrackerService.kt",
    ).readText()

    @Test
    fun `a notificacao da corrida formata com o RunFormat e nao com contas proprias`() {
        assertTrue(
            servico.contains("RunFormat.distance(") && servico.contains("RunFormat.clock("),
            "o serviço deixou de usar o RunFormat: se voltar a ter contas próprias, volta a " +
                "poder discordar do ecrã sem ninguém dar por isso",
        )
    }

    @Test
    fun `a notificacao da corrida nao trunca nem escreve a virgula a mao`() {
        // `toInt()` sobre a parte decimal era o truncar; a vírgula entre aspas era o
        // separador escrito à mão. Nenhum dos dois volta sem passar por aqui.
        assertFalse(
            servico.contains("* 100).toInt()"),
            "voltou a truncar os decimais em vez de os arredondar",
        )
        assertFalse(
            Regex(""""\$\w+,\$\w+""").containsMatchIn(servico),
            "voltou a escrever a vírgula decimal à mão, em vez de a pedir ao idioma",
        )
    }

    @Test
    fun `a notificacao da corrida segue as unidades da pessoa`() {
        assertTrue(
            servico.contains("live.unidades"),
            "a notificação deixou de ler as unidades do estado da corrida — quem escolher " +
                "milhas volta a vê-la em quilómetros",
        )
    }

    @Test
    fun `a notificacao da corrida fala a lingua da app, como as outras todas`() {
        assertTrue(
            servico.contains("appLocalized()"),
            "o serviço deixou de localizar o contexto: os cinco trabalhadores, o aviso do " +
                "treinador, o do jejum e o widget fazem-no, e este voltaria a ser o único " +
                "que não faz",
        )
        assertFalse(
            Regex("""[^.]\bgetString\(R\.string""").containsMatchIn(servico),
            "há um getString fora do contexto localizado, e esse sai na língua do telemóvel",
        )
    }

    @Test
    fun `a regra da virgula decimal e uma so, e vive fora da composicao`() {
        // A função existe para quem formata fora de uma composição poder seguir a mesma
        // regra em vez de a escolher à mão — que foi exactamente como este defeito nasceu.
        assertTrue(usaVirgulaDecimal("pt"))
        assertFalse(usaVirgulaDecimal("en"))
    }
}
