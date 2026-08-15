package pt.antares.app.core.nutrition

import pt.antares.app.core.model.FoodSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * De onde vêm os números de um alimento não é cosmética.
 *
 * A licença do INSA exige que a fonte apareça **visivelmente onde os dados são mostrados**,
 * e a app dizia «Tabela Antares (PT/BR)» — uma tabela que não existe — nos 363 alimentos
 * escritos à mão. Prometer uma fonte inexistente é a única forma de errar aqui que tem
 * consequência para lá do gosto.
 */
class ProveniencaHonestaTest {

    private val textos = File("src/commonMain/composeResources/values/strings.xml").readText()
    private val textosEn = File("src/commonMain/composeResources/values-en/strings.xml").readText()

    private fun linha(chave: String, xml: String): String =
        Regex("""<string name="$chave">([^<]*)</string>""").find(xml)?.groupValues?.get(1)
            ?: error("a chave $chave desapareceu")

    @Test
    fun `nenhum texto promete uma tabela Antares`() {
        val inventadas = listOf("Tabela Antares", "Antares table", "Antares (PT")

        val encontradas = inventadas.filter { it in textos || it in textosEn }
        assertEquals(
            emptyList(),
            encontradas,
            "não existe nenhuma tabela Antares. Os 363 alimentos com prefixo `pt-` ou `ptx` " +
                "são escritos à mão a partir de tabelas publicadas, e é isso que o ecrã " +
                "tem de dizer",
        )
    }

    @Test
    fun `cada origem tem um texto proprio, e nenhum fica vazio`() {
        val chaves = listOf(
            "nutrition_source_curated",
            "nutrition_source_curated_plain",
            "nutrition_source_tca",
            "nutrition_source_ciqual",
            "nutrition_source_usda",
            "nutrition_source_off",
            "nutrition_source_ai",
            "nutrition_source_user",
        )
        val vazias = chaves.filter { linha(it, textos).isBlank() || linha(it, textosEn).isBlank() }
        assertEquals(emptyList(), vazias, "uma origem sem texto não se mostra a ninguém")
    }

    @Test
    fun `o INSA e nomeado onde os dados dele aparecem`() {
        assertTrue(
            "INSA" in linha("nutrition_source_tca", textos),
            "a licença da Tabela de Composição de Alimentos obriga a identificar a fonte " +
                "junto dos dados, e não só num ecrã de atribuições",
        )
        assertTrue("INSA" in linha("nutrition_source_tca", textosEn))
    }

    @Test
    fun `o prefixo do identificador decide a origem`() {
        val casos = mapOf(
            "tca-100" to FoodProvenance.TCA,
            "ciqual-100" to FoodProvenance.CIQUAL,
            "usda-100" to FoodProvenance.USDA,
            "pt-bacalhau" to FoodProvenance.CURATED,
            "ptx3-42" to FoodProvenance.CURATED,
        )
        val errados = casos.filter { (id, esperada) ->
            FoodProvenance.of(FoodSource.SEED, id) != esperada
        }
        assertEquals(
            emptyMap(),
            errados,
            "um alimento a mostrar a origem errada é pior do que não mostrar nenhuma",
        )
    }

    @Test
    fun `o que nao vem de uma tabela nunca se disfarca de tabela`() {
        assertEquals(FoodProvenance.AI, FoodProvenance.of(FoodSource.AI_ESTIMATE, "usda-1"))
        assertEquals(FoodProvenance.USER, FoodProvenance.of(FoodSource.CUSTOM, "ciqual-1"))
        assertEquals(FoodProvenance.OFF, FoodProvenance.of(FoodSource.OFF, "tca-1"))
    }
}
