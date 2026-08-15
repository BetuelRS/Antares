package pt.antares.app.core.fooddata

import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O dicionário que põe em português os nomes americanos, medido contra o catálogo que a app
 * traz mesmo.
 *
 * A cobertura é baixa de propósito: só se traduz o que está coberto por inteiro, e o resto
 * fica em inglês limpo. Este teste existe para essa cobertura ser um **número conhecido** em
 * vez de uma impressão, e para não descer sem alguém dar por isso.
 */
@RunWith(RobolectricTestRunner::class)
class DicionarioUsdaTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val recursos = File("src/commonMain/composeResources/files")

    private fun segmentos(): Map<String, String> =
        json.decodeFromString<Map<String, String>>(
            File(recursos, "seed_pt_usda_names.json").readText(),
        ).filterKeys { !it.startsWith("_") }

    private fun nomesUsda(): List<String> =
        Regex(""""id":"(usda-[^"]+)","source":"[^"]*","sourceRef":("[^"]*"|null),"nameEn":"((?:[^"\\]|\\.)*)"""")
            .findAll(File(recursos, "seed_foods.json").readText())
            .map { it.groupValues[3].replace("\\\"", "\"") }
            .toList()

    @Test
    fun `o dicionario cobre pelo menos o que ja cobria`() {
        val segmentos = segmentos()
        val nomes = nomesUsda()
        assertTrue(nomes.size > 2_000, "só encontrei ${nomes.size} nomes USDA — o padrão falhou")

        val traduzidos = nomes.count { UsdaNameTranslator.traduzir(it, segmentos) != null }
        val percentagem = traduzidos * 100.0 / nomes.size

        println("USDA em português: $traduzidos de ${nomes.size} (${percentagem.toInt()}%)")
        assertTrue(
            traduzidos >= MINIMO_TRADUZIDOS,
            "a cobertura caiu para $traduzidos: alguém tirou entradas do dicionário, ou o " +
                "catálogo foi regenerado com nomes diferentes",
        )
    }

    @Test
    fun `nenhuma traducao fica a meio`() {
        val segmentos = segmentos()

        // Palavras que denunciam inglês por traduzir. Se alguma aparecer num nome que o
        // tradutor deu por bom, a regra do «tudo ou nada» partiu-se.
        val inglesas = listOf(" with ", " and ", " without ", "prepared", "flavor", "added")

        val maus = nomesUsda()
            .mapNotNull { en -> UsdaNameTranslator.traduzir(en, segmentos)?.let { en to it } }
            .filter { (_, pt) -> inglesas.any { it in pt.lowercase() } }

        assertEquals(
            emptyList(),
            maus,
            "estes nomes saíram meio traduzidos, que é pior do que os deixar em inglês",
        )
    }

    @Test
    fun `o dicionario nao tem chaves com maiusculas nem espacos a mais`() {
        val problemas = segmentos().keys.filter { it != it.trim().lowercase() }
        assertEquals(
            emptyList(),
            problemas,
            "a procura é feita em minúsculas e já aparada: estas chaves nunca casariam",
        )
    }

    private companion object {
        // Medido a 2026-08-15: 445 de 2940. Serve de chão, não de meta — subir é bem-vindo,
        // e para isso acrescentam-se segmentos ao dicionário.
        const val MINIMO_TRADUZIDOS = 445
    }
}
