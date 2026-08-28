package pt.antares.app.core

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * O estudo tem de continuar alcançável a partir dos ficheiros que se lêem ao começar.
 *
 * **Isto nasce de um falhanço, e não de uma precaução.** Treze versões saíram construídas a
 * partir das linhas-resumo do `PLANO-DE-PRODUCAO.md`, sem um único esboço aberto — e a razão
 * não foi distração: o `como-continuar.md`, que é o ficheiro que se lê ao começar qualquer
 * sessão, não mencionava o estudo em lado nenhum. Nem as regras. Nem a memória.
 *
 * Um sistema de continuidade que não aponta para o estudo produz trabalho que não o segue,
 * por muito boa que seja a intenção de quem o usa. O que este teste guarda é **o ponteiro** —
 * a única parte mecanizável da regra C5.
 *
 * O que ele **não** consegue guardar: que alguém abriu mesmo os documentos. Isso verifica-se
 * pelo registo de cada versão no plano, que nomeia os ficheiros abertos, e o plano vive fora
 * do git por decisão do dono.
 */
class OEstudoTemPonteiroTest {

    private val raiz = File("..")

    private fun doc(caminho: String) = File(raiz, caminho).readText().replace("\r\n", "\n")

    @Test
    fun `o guia da divida com o estudo existe`() {
        assertTrue(
            File(raiz, "docs/referencia/a-divida-com-o-estudo.md").exists(),
            "o guia que diz o que abrir antes de cada área desapareceu",
        )
    }

    /**
     * O ficheiro que se lê ao começar tem de mandar abrir o estudo — e antes das perguntas
     * de abertura, porque metade delas já lá está respondida.
     */
    @Test
    fun `o como-continuar manda abrir o estudo`() {
        val texto = doc("docs/referencia/como-continuar.md")

        assertTrue("estudo/" in texto, "o `como-continuar` deixou de apontar para o estudo")
        assertTrue(
            "a-divida-com-o-estudo.md" in texto,
            "o `como-continuar` deixou de apontar para a rota do estudo",
        )
        assertTrue("esboço" in texto, "o `como-continuar` deixou de falar dos esboços")
    }

    @Test
    fun `a regra C5 continua escrita`() {
        val regras = doc("docs/referencia/regras.md")

        assertTrue("**C5**" in regras, "a regra que manda abrir o estudo saiu das regras")
        assertTrue(
            "a-divida-com-o-estudo.md" in regras,
            "a C5 deixou de dizer onde está a rota",
        )
    }

    /**
     * Cada esboço que existe no disco tem de estar nomeado na rota.
     *
     * O `estudo/` está fora do git — no CI esta verificação não tem o que ler, e passa. Vale
     * na máquina de quem tem o estudo, que é onde o trabalho se faz e onde um esboço novo
     * pode aparecer sem entrar na rota.
     */
    @Test
    fun `nenhum esboco fica de fora da rota`() {
        val pasta = File(raiz, "estudo/esbocos")
        if (!pasta.isDirectory) return

        val guia = doc("docs/referencia/a-divida-com-o-estudo.md")
        val forasteiros = pasta.listFiles()
            .orEmpty()
            .filter { it.extension == "html" && it.name != "index.html" }
            // O número é o que identifica o esboço na rota — os títulos mudam, o número não.
            .map { it.name.substringBefore('-') }
            .distinct()
            .filterNot { numero -> Regex("""\b0?$numero\b""").containsMatchIn(guia) }

        assertTrue(
            forasteiros.isEmpty(),
            "esboços que existem e a rota não nomeia: $forasteiros",
        )
    }
}
