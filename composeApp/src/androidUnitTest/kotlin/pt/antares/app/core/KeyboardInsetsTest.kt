package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class KeyboardInsetsTest {

    private val scaffold =
        File("src/commonMain/kotlin/pt/antares/app/core/designsystem/components/Scaffold.kt")
    private val app = File("src/commonMain/kotlin/pt/antares/app/App.kt")

    @Test
    fun `o scaffold de toda a app afasta o conteudo do teclado`() {
        assertTrue(scaffold.exists(), "não encontrei o Scaffold.kt — o teste deixou de olhar para o sítio certo")
        assertTrue(
            scaffold.readText().contains("imePadding()"),
            "o AntaresScaffold deixou de tratar do teclado. O `adjustResize` do " +
                "manifesto não o faz por ele: com `enableEdgeToEdge()` o sistema " +
                "não encolhe a janela, e o teclado volta a tapar os botões em " +
                "todos os ecrãs de uma vez",
        )
    }

    @Test
    fun `todos os ecras estao dentro desse scaffold`() {
        val texto = app.readText()
        val raiz = texto.substringAfter("private fun MainScaffold")
        assertTrue(raiz.isNotEmpty(), "não encontrei o MainScaffold — o teste perdeu o alvo")

        assertTrue(
            raiz.contains("AntaresScaffold("),
            "a raiz deixou de usar o AntaresScaffold; a correção do teclado " +
                "deixou de chegar aos ecrãs",
        )
        assertTrue(
            raiz.substringAfter("AntaresScaffold(").contains("AntaresNavHost("),
            "o NavHost saiu de dentro do scaffold — os ecrãs deixaram de herdar " +
                "o afastamento do teclado",
        )
    }

    @Test
    fun `a app continua em edge-to-edge, que e o motivo de isto ser preciso`() {

        val activity = File("src/androidMain/kotlin/pt/antares/app/MainActivity.kt").readText()
        assertTrue(
            activity.contains("enableEdgeToEdge()"),
            "a app já não está em edge-to-edge: revê o comentário do AntaresScaffold " +
                "antes que alguém tire o imePadding por o achar desnecessário",
        )
    }
}
