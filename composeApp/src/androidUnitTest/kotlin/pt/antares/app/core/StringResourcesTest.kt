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
}
