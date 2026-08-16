package pt.antares.app.core

import com.antares.app.BuildConfig
import org.junit.Test
import pt.antares.app.feature.about.AppChangelog
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A documentação é a parte do repositório que apodrece sem dar erro: o código deixa de compilar
 * quando alguém lhe muda o nome a um ficheiro, e um `.md` não deixa. Este teste é o que impede a
 * documentação de descrever uma app que já não existe.
 *
 * O diretório de trabalho dos testes é `composeApp/`, e por isso a raiz do repositório é `..`.
 */
class DocumentationHonestyTest {

    private val raiz = File("..")

    private val documentos: List<File>
        get() = (raiz.listFiles { f -> f.isFile && f.extension == "md" }?.toList().orEmpty() +
            File(raiz, "docs").walkTopDown().filter { it.isFile && it.extension == "md" }.toList())
            .sortedBy { it.path }

    /**
     * Caminhos que a documentação cita e que não existem numa cópia acabada de clonar. Cada
     * exceção precisa de motivo: sem isso, a lista cresce até o teste não verificar nada.
     */
    private val foraDoRepositorio = listOf(
        // Os APKs são entregues fora do git e o `.gitignore` exclui-os; a documentação fala
        // deles precisamente para explicar porque é que não estão aqui.
        "apks/",

        // O que o Gradle escreve. Estes caminhos são a resposta a «onde é que fica o APK
        // depois de compilar», e por isso a documentação tem de os citar — mas numa cópia
        // acabada de clonar, ou logo a seguir a um `clean`, ainda não existem. Sem esta
        // exceção o teste só passava por acaso: bastava alguém correr o `clean` para o
        // guarda acusar uma documentação que está certa.
        "composeApp/build/",
    )

    private val ficheiroCitado = Regex("""`([A-Za-z0-9_][A-Za-z0-9_./-]*\/[A-Za-z0-9_./-]*)`""")
    private val ligacaoRelativa = Regex("""]\((?!https?://|#)([^)\s]+)\)""")

    @Test
    fun `a versao do topo do changelog e a versao que a app diz ser`() {
        val changelog = File(raiz, "CHANGELOG.md")
        assertTrue(changelog.exists(), "não há CHANGELOG.md na raiz")

        // A primeira versão numerada, saltando o `[Unreleased]`, que não tem número.
        val topo = Regex("""^## \[(\d+\.\d+\.\d+)]""", RegexOption.MULTILINE)
            .find(changelog.readText())
            ?.groupValues
            ?.get(1)

        assertEquals(
            AppChangelog.CURRENT,
            topo,
            "o CHANGELOG.md abre numa versão e a app diz outra",
        )
        assertEquals(
            BuildConfig.VERSION_NAME,
            AppChangelog.CURRENT,
            "o AppChangelog.CURRENT não acompanha o versionName do build",
        )
    }

    @Test
    fun `o versionCode deriva do versionName, e nao de uma contagem a mao`() {
        val (major, minor, patch) = BuildConfig.VERSION_NAME.split(".").map { it.toInt() }

        assertEquals(
            major * 10_000 + minor * 100 + patch,
            BuildConfig.VERSION_CODE,
            "a fórmula do versionCode saiu do sítio — ver docs/VERSIONING.md",
        )
    }

    @Test
    fun `a versao segue SemVer, com tres numeros e nunca quatro`() {
        assertTrue(
            Regex("""^\d+\.\d+\.\d+$""").matches(BuildConfig.VERSION_NAME),
            "versão fora de SemVer: ${BuildConfig.VERSION_NAME}",
        )
    }

    @Test
    fun `todas as versoes que a app mostra existem no changelog`() {
        val texto = File(raiz, "CHANGELOG.md").readText()

        for (v in AppChangelog.versions) {
            assertTrue(
                texto.contains("## [${v.name}]"),
                "a app mostra a versão ${v.name}, que o CHANGELOG.md não conhece",
            )
        }
    }

    @Test
    fun `nenhum documento cita um ficheiro que nao existe`() {
        val faltam = mutableListOf<String>()

        for (doc in documentos) {
            val texto = doc.readText()

            // Uma ligação de markdown é relativa ao documento que a escreve; um caminho entre
            // crases é sempre a partir da raiz, e é essa a convenção: crases significam
            // «isto existe mesmo, aqui».
            val citados = ficheiroCitado.findAll(texto).map { it.groupValues[1] to raiz } +
                ligacaoRelativa.findAll(texto).map { it.groupValues[1] to doc.parentFile }

            for ((caminho, base) in citados.distinctBy { it.first }) {
                if (foraDoRepositorio.any { caminho.startsWith(it) }) continue
                if (!File(base, caminho).exists()) {
                    faltam += "${doc.name} cita `$caminho`"
                }
            }
        }

        assertTrue(
            faltam.isEmpty(),
            "a documentação aponta para ficheiros que não existem:\n" + faltam.joinToString("\n"),
        )
    }

    @Test
    fun `o cartaz de versao do README nao envelhece sozinho`() {
        // Um número escrito à mão dentro de uma imagem de cartaz é a coisa mais fácil de
        // esquecer numa versão nova, e a primeira que um visitante lê.
        val readme = File(raiz, "README.md").readText()
        val cartaz = Regex("""img\.shields\.io/badge/vers%C3%A3o-([0-9.]+)-""").find(readme)

        assertEquals(
            BuildConfig.VERSION_NAME,
            cartaz?.groupValues?.get(1),
            "o cartaz de versão do README não bate com a versão da app",
        )
    }

    @Test
    fun `o changelog diz de onde veio, por ter sido reconstruido`() {
        // Os números aqui não são os que a app teve durante o desenvolvimento. Sem esta nota,
        // quem comparar o histórico com os artefactos conclui — com razão — que foi inventado.
        val texto = File(raiz, "CHANGELOG.md").readText()

        assertTrue(
            texto.contains("Sobre este histórico"),
            "o CHANGELOG.md perdeu a nota que explica que o histórico foi reconstruído",
        )
    }
}
