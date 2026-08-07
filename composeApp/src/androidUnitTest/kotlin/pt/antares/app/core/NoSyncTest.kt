package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoSyncTest {

    private val build = File("build.gradle.kts").readText()
    private val manifest = File("src/androidMain/AndroidManifest.xml").readText()
    private val fontes = listOf(File("src/commonMain/kotlin"), File("src/androidMain/kotlin"))
        .filter { it.exists() }
        .flatMap { it.walkTopDown().filter { f -> f.isFile && f.extension == "kt" } }

    private fun codigoDe(f: File): String =
        Regex("""/\*[\s\S]*?\*/|//[^\n]*""").replace(f.readText(), "")

    @Test
    fun `o postgrest nao volta ao build`() {

        assertTrue(
            !build.contains("supabase.postgrest") && !build.contains("postgrest-kt"),
            "o postgrest voltou ao build — meia sincronização com ele",
        )
    }

    @Test
    fun `nao ha login com Google`() {
        val proibido = listOf("googleid", "androidx.credentials", "play-services-auth")
        val presentes = proibido.filter { build.contains(it) }
        assertEquals(emptyList(), presentes, "dependências de login social de volta ao build")

        val comGoogle = fontes.filter { f ->
            Regex("""linkIdentity|signInWith\(Google|Provider\.Google""").containsMatchIn(codigoDe(f))
        }.map { it.name }
        assertEquals(emptyList(), comGoogle, "código de login com Google")
    }

    @Test
    fun `o retorno do OAuth saiu do manifesto`() {

        assertTrue(
            !manifest.contains("android:scheme=\"antares\""),
            "o deep link do OAuth continua declarado",
        )
    }

    @Test
    fun `nenhum ecra fala de sincronizar contas`() {

        val orfaos = fontes.filter { f ->
            Regex("""\bSyncEngine\b|\bSyncCoordinator\b|\bSyncTable\b|\bAccountRepository\b""")
                .containsMatchIn(codigoDe(f))
        }.map { it.name }
        assertEquals(emptyList(), orfaos, "referências à maquinaria de sync que devia ter saído")
    }

    @Test
    fun `nenhum texto visivel promete conta ou sincronizacao`() {
        val strings = listOf(
            File("src/commonMain/composeResources/values/strings.xml"),
            File("src/commonMain/composeResources/values-en/strings.xml"),
        )
        assertTrue(strings.all { it.exists() }, "não encontrei os ficheiros de strings")

        val promessas = Regex(
            """se tiveres conta|if you have an account|do servidor|from the server|""" +
                """sincroniza(r|ção)?\b|\bsync(ing|ed)?\b|na nuvem|in the cloud""",
            RegexOption.IGNORE_CASE,
        )
        val entrada = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

        val mentiras = strings.flatMap { f ->
            entrada.findAll(f.readText())
                .filter { promessas.containsMatchIn(it.groupValues[2]) }
                .map { "${f.parentFile.name}/${it.groupValues[1]}: \"${it.groupValues[2]}\"" }
        }
        assertEquals(
            emptyList(),
            mentiras,
            "estes textos prometem à pessoa uma conta ou uma sincronização que a app já não tem",
        )
    }

    @Test
    fun `a sessao anonima sobrevive, porque a AI precisa dela`() {

        val sessao = File("src/commonMain/kotlin/pt/antares/app/core/network/supabase/AnonymousSession.kt")
        assertTrue(sessao.exists(), "a sessão anónima desapareceu — a análise da AI deixa de funcionar")
        assertTrue(
            sessao.readText().contains("signInAnonymously"),
            "a sessão anónima deixou de assinar",
        )
        assertTrue(build.contains("supabase.functions"), "as Edge Functions saíram do build")
    }
}
