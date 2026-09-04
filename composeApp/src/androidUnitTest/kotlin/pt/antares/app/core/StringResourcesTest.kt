package pt.antares.app.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringResourcesTest {

    private val resDir = File("src/commonMain/composeResources")

    private fun stringFiles(): List<File> =
        resDir.walkTopDown().filter { it.name == "strings.xml" }.toList()

    private val stringEntry = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    /** Chaves sem `Res.string.` que mesmo assim se justificam. Vazia hoje — ver o teste. */
    private val orfasPermitidas = mapOf<String, String>()

    @Test
    fun `nenhuma string usa percentagem duplicada`() {
        val files = stringFiles()
        assertTrue(files.isNotEmpty(), "não encontrei nenhum strings.xml em $resDir")

        val offenders = files.flatMap { file ->
            stringEntry.findAll(file.readText())
                .filter { it.groupValues[2].contains("%%") }
                .map { "${file.parentFile.name}/${it.groupValues[1]}" }
        }
        assertEquals(
            emptyList(),
            offenders,
            "o Compose Resources não colapsa %% — a app mostraria dois sinais de percentagem",
        )
    }

    /**
     * O mesmo problema do `%%`, com outra personagem.
     *
     * **Apanhado no aparelho a 2026-08-28**, e por nenhum dos 1633 testes: o estado vazio da
     * pesquisa mostrava, escrito no ecrã, «This is where you\'ll see what you eat most». O
     * Compose Resources não desfaz o escape do XML do Android, e a barra invertida vai para o
     * ecrã como qualquer outro carácter.
     *
     * O apóstrofo escreve-se nu. É o que as outras trezentas strings inglesas já faziam — as
     * duas que traziam a barra eram as duas que eu tinha acabado de escrever.
     */
    @Test
    fun `nenhuma string escapa o apostrofo`() {
        val offenders = stringFiles().flatMap { file ->
            stringEntry.findAll(file.readText())
                .filter { it.groupValues[2].contains("\\'") }
                .map { "${file.parentFile.name}/${it.groupValues[1]}" }
        }
        assertEquals(
            emptyList(),
            offenders,
            "o Compose Resources não desfaz o escape — a app mostraria a barra invertida",
        )
    }

    @Test
    fun `PT e EN tem exatamente as mesmas chaves`() {
        val byLocale = stringFiles().associate { file ->
            file.parentFile.name to stringEntry.findAll(file.readText())
                .map { it.groupValues[1] }
                .toSet()
        }
        val pt = byLocale["values"].orEmpty()
        val en = byLocale["values-en"].orEmpty()
        assertTrue(pt.isNotEmpty() && en.isNotEmpty(), "faltam ficheiros de strings")

        assertEquals(emptySet(), pt - en, "chaves só em PT — o ecrã em inglês rebentaria")
        assertEquals(emptySet(), en - pt, "chaves só em EN — o ecrã em português rebentaria")
    }

    private val formatArg = Regex("""%(\d+)\$([sdf])""")

    private fun argsOf(text: String): Map<String, String> =
        formatArg.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }

    private fun argsByLocale(): Map<String, Map<String, Map<String, String>>> =
        stringFiles().associate { file ->
            file.parentFile.name to stringEntry.findAll(file.readText())
                .associate { it.groupValues[1] to argsOf(it.groupValues[2]) }
        }

    @Test
    fun `os argumentos de formato batem entre PT e EN`() {

        val entries = argsByLocale()
        val pt = entries["values"].orEmpty()
        val en = entries["values-en"].orEmpty()

        val mismatched = pt.keys.intersect(en.keys)
            .filter { pt[it] != en[it] }
            .map { "$it: PT=${pt[it]} EN=${en[it]}" }
        assertEquals(emptyList(), mismatched, "argumentos de formato diferentes entre PT e EN")
    }

    @Test
    fun `os argumentos estao numerados de 1 sem saltos`() {

        val offenders = argsByLocale().flatMap { (locale, strings) ->
            strings.filter { (_, args) ->
                args.isNotEmpty() &&
                    args.keys.map { it.toInt() }.toSortedSet().toList() != (1..args.size).toList()
            }.map { "$locale/${it.key} → ${it.value.keys.sorted()}" }
        }
        assertEquals(emptyList(), offenders, "argumentos de formato com saltos na numeração")
    }

    @Test
    fun `o seletor de unidades do onboarding nao repete a unidade`() {
        val curtas = setOf("settings_units_metric_short", "settings_units_imperial_short")
        val unidade = Regex("""\b(kg|lb|cm|ft|in)\b""", RegexOption.IGNORE_CASE)

        val comUnidade = stringFiles().flatMap { file ->
            stringEntry.findAll(file.readText())
                .filter { it.groupValues[1] in curtas }
                .filter { unidade.containsMatchIn(it.groupValues[2]) }
                .map { "${file.parentFile.name}/${it.groupValues[1]} → \"${it.groupValues[2]}\"" }
        }
        assertEquals(
            emptyList(),
            comUnidade,
            "o chip fica por cima de campos que já dizem a unidade — repeti-la " +
                "põe `kg` duas vezes em métrico, e `kg` ao lado de `lb` em imperial",
        )

        val ecra = File("src/commonMain/kotlin/pt/antares/app/feature/onboarding/OnboardingScreen.kt").readText()
        assertTrue(
            !ecra.contains("Res.string.settings_units_metric\n") &&
                !ecra.contains("Res.string.settings_units_metric,"),
            "o onboarding voltou a usar o rótulo longo, que traz a unidade",
        )
    }

    @Test
    fun `rotulos que recebem unidade do codigo nao trazem outra`() {

        val recebemUnidade = setOf("onb_body_weight")
        val unidade = Regex("""\((kg|lb|cm|ft|in|g|ml)\)""", RegexOption.IGNORE_CASE)

        val offenders = stringFiles().flatMap { file ->
            stringEntry.findAll(file.readText())
                .filter { it.groupValues[1] in recebemUnidade }
                .filter { unidade.containsMatchIn(it.groupValues[2]) }
                .map { "${file.parentFile.name}/${it.groupValues[1]} → \"${it.groupValues[2]}\"" }
        }
        assertEquals(
            emptyList(),
            offenders,
            "estes rótulos já trazem unidade e o ecrã junta-lhe outra",
        )
    }

    /**
     * Uma string sem quem a use é o resto de um ecrã que mudou de forma, e não dá erro nenhum:
     * traduz-se, revê-se e viaja no APK para sempre. Treze delas sobreviveram às duas passagens
     * da auditoria — os separadores antigos da pesquisa, o botão flutuante que deixou de criar,
     * o campo de texto que virou chips — e só apareceram quando alguém as procurou de propósito.
     *
     * A décima terceira é a razão de o teste varrer **só o código da app**: o `session_weight`
     * dizia «kg» em duro, o ecrã passou a usar o rótulo que conhece o sistema de unidades, e a
     * chave sobreviveu porque um **teste** continuava a importá-la. Contar os testes como quem
     * usa faria esta varredura passar por cima exactamente do caso que a justifica.
     *
     * A [orfasPermitidas] existe porque uma chave pode ser legítima sem estar num `Res.string.`;
     * hoje está vazia, e isso é a medida de que a varredura ficou limpa.
     */
    @Test
    fun `nenhuma string fica sem quem a use`() {
        val chaves = stringEntry.findAll(File(resDir, "values/strings.xml").readText())
            .map { it.groupValues[1] }
            .toList()
        assertTrue(chaves.isNotEmpty(), "não li chave nenhuma de $resDir/values/strings.xml")

        val codigo = listOf("src/commonMain/kotlin", "src/androidMain/kotlin")
            .map(::File)
            .filter { it.exists() }
            .flatMap { it.walkTopDown().filter { f -> f.isFile && f.extension == "kt" } }
            .joinToString("\n") { it.readText() }
        val usadas = Regex("""Res\.string\.(\w+)""").findAll(codigo)
            .map { it.groupValues[1] }
            .toSet()

        val orfas = chaves.filter { it !in usadas && it !in orfasPermitidas }
        assertEquals(
            emptyList(),
            orfas,
            "chaves que ninguém usa. Cada uma é uma de duas coisas: uma string a apagar dos " +
                "dois idiomas, ou uma excepção com a razão escrita neste teste:\n" +
                orfas.joinToString("\n"),
        )
    }

    @Test
    fun `toda a excecao de string orfa traz a razao por escrito`() {
        assertEquals(
            emptyList(),
            orfasPermitidas.filterValues { it.isBlank() }.keys.toList(),
            "uma excepção sem razão é uma string esquecida com autorização",
        )
    }

    /**
     * Strings onde um número inteiro de minutos está certo, com a razão de cada uma.
     *
     * O critério é estreito: só passa quem mostra uma **diferença** de minutos escolhida de
     * uma lista curta, e não uma duração medida. Uma duração medida cresce sem ninguém a
     * vigiar — um treino esquecido aberto de um dia para o outro chega aos milhares.
     */
    private val minutosInteirosPermitidos = mapOf(
        "exercise_duration_less" to
            "é o passo do botão −, e vale 5: uma diferença de cinco minutos nunca vira horas",
        "exercise_duration_more" to
            "o mesmo, do botão +",
    )

    private val minutosCrus = Regex("""%\d*\$?d\s*(min\b|minuto|minute)""", RegexOption.IGNORE_CASE)

    /**
     * **Uma duração de minutos nunca se mostra como inteiro.**
     *
     * A app tem o `formatDurationMin`, que escreve `43h 39m`, e é ele que a janela alimentar,
     * o jejum, o histórico do treino e o resumo já usam. Um `%1$d min` cru lê-se enquanto o
     * número é pequeno e deixa de se ler no dia em que não é.
     *
     * **Este guarda nasce de a correcção ter falhado uma vez.** A 2.24.0 encontrou o defeito
     * em quatro sítios, converteu os quatro, e escreveu no CHANGELOG que estava fechado — e a
     * varredura de 2026-09-04 achou o quinto no aparelho: o cartão de destaque do treino dizia
     * «~4236 min last time» duas linhas acima de «70h 36m». Escapou porque a busca foi feita
     * pelo **nome** da string, `workout_hub_minutes`, e a quinta chamava-se outra coisa.
     *
     * Procurar pelo formato apanha as cinco, e apanha a sexta que alguém escrever.
     */
    @Test
    fun `nenhuma duracao em minutos e mostrada como inteiro`() {
        val offenders = stringFiles().flatMap { file ->
            stringEntry.findAll(file.readText())
                .filter { it.groupValues[1] !in minutosInteirosPermitidos }
                .filter { minutosCrus.containsMatchIn(it.groupValues[2]) }
                .map { "${file.parentFile.name}/${it.groupValues[1]} → \"${it.groupValues[2]}\"" }
        }
        assertEquals(
            emptyList(),
            offenders,
            "duração em minutos sem conversão. Ou passa pelo `formatDurationMin`, ou entra " +
                "no `minutosInteirosPermitidos` com a razão escrita:\n" + offenders.joinToString("\n"),
        )
    }

    @Test
    fun `toda a excecao de minutos inteiros traz a razao por escrito`() {
        assertEquals(
            emptyList(),
            minutosInteirosPermitidos.filterValues { it.isBlank() }.keys.toList(),
            "uma excepção sem razão é um número por converter com autorização",
        )
    }
}
