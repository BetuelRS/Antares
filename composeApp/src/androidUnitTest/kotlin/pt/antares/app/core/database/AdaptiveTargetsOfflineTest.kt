package pt.antares.app.core.database

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveTargetsOfflineTest {

    private val repositorio =
        File("src/commonMain/kotlin/pt/antares/app/core/coach/CoachRepository.kt")

    private fun codigo(): String =
        Regex("""/\*[\s\S]*?\*/|//[^\n]*""").replace(repositorio.readText(), "")

    @Test
    fun `o motor de metas nao chama a AI`() {
        assertTrue(repositorio.exists(), "o CoachRepository desapareceu — as metas adaptativas com ele")
        val corpo = codigo()

        val chamadasAi = Regex("""client\.\w+\(|weeklyCoach|functions\.invoke""")
            .findAll(corpo)
            .map { it.value }
            .toList()
        assertEquals(
            emptyList(),
            chamadasAi,
            "o cálculo das metas voltou a passar pela AI — em modo avião deixa de propor nada",
        )
    }

    @Test
    fun `a proposta continua a gravar um override por dia`() {
        val corpo = codigo()

        assertTrue(
            corpo.contains("\"adaptive-\$d\"") || corpo.contains("adaptive-"),
            "o id determinístico da meta adaptativa desapareceu",
        )
        assertTrue(corpo.contains("overrideDao.upsert"), "a proposta deixou de escrever a meta")
        assertTrue(corpo.contains("evaluateAdaptive"), "o motor deixou de ser avaliado")
    }

    @Test
    fun `o texto do modelo saiu, e as colunas ficaram vazias em vez de desaparecer`() {
        val corpo = codigo()

        assertTrue(corpo.contains("winsJson = \"[]\""), "o resumo voltou a escrever texto de modelo")
        assertTrue(corpo.contains("focus = \"\""), "o resumo voltou a escrever texto de modelo")
    }

    @Test
    fun `a funcao weekly-coach saiu do servidor`() {
        val fn = File("../supabase/functions/weekly-coach")
        assertTrue(!fn.exists(), "a Edge Function do coach continua no repositório")
    }
}
