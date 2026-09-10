package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O Hoje abre pela meta, e o jejum tem porta fora dele.
 *
 * Até à 2.32.0 a meta era o **quinto** cartão — abaixo do aviso da cópia, do treinador, da
 * sequência e das respostas em falta —, e num telemóvel ficava abaixo da dobra. Cada cartão
 * novo que alguém acrescentasse ao princípio da grelha empurrava-a mais um degrau, e nada o
 * dizia: o ecrã continuava a funcionar, só que com a coisa mais importante fora dele.
 *
 * E o cartão vazio do jejum saiu do Hoje na mesma versão. Ele era **a única porta** para o jejum
 * em toda a app; se a linha do «Mais» desaparecer, quem não está a jejuar deixa de o poder
 * começar, e nenhum outro teste repara.
 *
 * Lê a fonte, porque o que se guarda é a ordem da chamada e não o que um ecrã desenhado num
 * teste mostra — a grelha distribui os cartões por colunas conforme a largura.
 */
class OrdemDoHojeTest {

    private val hoje = File("src/commonMain/kotlin/pt/antares/app/feature/today/TodayScreen.kt").readText()
    private val mais = File("src/commonMain/kotlin/pt/antares/app/feature/me/AppMenuScreen.kt").readText()

    @Test
    fun `o primeiro cartao da grelha e a meta do dia`() {
        val grelha = hoje.substringAfter("GrelhaDeCartoes(").substringAfter("    ) {")
        val primeiro = Regex("""cartao \{\s*([A-Za-z]+)""").find(grelha)?.groupValues?.get(1)
        assertEquals("CartaoDaMeta", primeiro, "a meta deixou de ser o primeiro cartão do Hoje")
    }

    @Test
    fun `o jejum tem porta no Mais`() {
        assertTrue(
            Regex("""MenuItem\([^)]*corpo\.jejum\)""").containsMatchIn(mais),
            "o «Mais» perdeu a porta do jejum — e o Hoje já não mostra o jejum a quem não está a jejuar",
        )
    }
}
