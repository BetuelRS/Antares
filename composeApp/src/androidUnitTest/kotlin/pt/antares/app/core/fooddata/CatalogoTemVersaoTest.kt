package pt.antares.app.core.fooddata

import kotlinx.serialization.json.Json
import pt.antares.app.core.catalogo.ManifestoDoCatalogo
import pt.antares.app.core.util.sha256
import pt.antares.app.feature.fooddata.Catalogo
import pt.antares.app.feature.fooddata.FoodSeeder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O ficheiro do catálogo e a compilação que o transporta têm de concordar sobre a versão.
 *
 * O `FoodSeeder` decide se importa comparando o que está gravado no telemóvel com uma
 * constante compilada, e **não com o ficheiro** — abrir cinco megabytes a cada arranque só
 * para concluir que não há nada a fazer era o custo que a marca existe para evitar. O preço
 * dessa escolha é este teste: se a constante ficar para trás de uma reconstrução, o catálogo
 * novo viaja dentro do APK e não entra em telemóvel nenhum. Não dá erro. Ninguém dá por isso
 * até alguém reparar que a correção prometida não apareceu.
 *
 * A ordem e a unicidade também se verificam aqui, e não por gosto de arrumação: são as duas
 * propriedades que tornam a construção determinística visível no `git diff`. Um catálogo por
 * ordem de tabela de dispersão muda de bytes sem mudar de conteúdo, e a partir daí ninguém
 * revê o que mudou.
 */
class CatalogoTemVersaoTest {

    private val ficheiro = File("src/commonMain/composeResources/files/catalogo.json")
    private val json = Json { ignoreUnknownKeys = true }

    private fun catalogo(): Catalogo = json.decodeFromString(ficheiro.readText())

    @Test
    fun `a versao compilada e a do ficheiro sao a mesma`() {
        assertEquals(
            FoodSeeder.VERSAO_DO_CATALOGO,
            catalogo().versao,
            "o `construir.mjs` e o `FoodSeeder` discordam sobre a versão do catálogo. " +
                "As duas sobem juntas, ou o catálogo novo não chega a instalação nenhuma.",
        )
    }

    @Test
    fun `o ficheiro esta onde o semeador o vai buscar`() {

        // O caminho é uma cadeia de caracteres dos dois lados, e um ficheiro que muda de
        // nome só se descobre a correr a app — onde a falha é silenciosa, porque o
        // semeador regista e devolve nulo em vez de rebentar.
        assertTrue(ficheiro.exists(), "falta ${ficheiro.path}")
        assertEquals("files/catalogo.json", FoodSeeder.FICHEIRO)
    }

    @Test
    fun `os alimentos vem por ordem e sem identificadores repetidos`() {
        val ids = catalogo().alimentos.map { it.id }

        assertEquals(ids.sorted(), ids, "o catálogo não veio ordenado pelo identificador")
        assertEquals(ids.size, ids.toSet().size, "há identificadores repetidos no catálogo")
    }

    @Test
    fun `todo o alimento tem nome nos dois idiomas e energia`() {
        val alimentos = catalogo().alimentos
        assertTrue(alimentos.size > MINIMO_PLAUSIVEL, "só ${alimentos.size} alimentos — a leitura partiu-se")

        val partidos = alimentos.filter {
            it.namePt.isBlank() || it.nameEn.isBlank() || it.kcal < 0
        }
        assertTrue(partidos.isEmpty(), "alimentos sem nome ou com energia negativa: ${partidos.take(5).map { it.id }}")
    }

    @Test
    fun `o manifesto descreve o catalogo que esta ao lado dele`() {

        // O manifesto e o catálogo sobem juntos para a mesma release, e a app recusa o
        // catálogo se o resumo não bater. Um manifesto desactualizado não dá erro nenhum
        // aqui: dá uma actualização que **nunca** instala, e ninguém descobre porquê sem
        // ler o código da verificação.
        val manifesto = json.decodeFromString<ManifestoDoCatalogo>(
            File("../tools/catalogo/manifesto.json").readText(),
        )

        assertEquals(FoodSeeder.VERSAO_DO_CATALOGO, manifesto.versao, "versão fora de sítio")
        assertEquals(catalogo().alimentos.size, manifesto.alimentos, "contagem fora de sítio")
        assertEquals(
            sha256(ficheiro.readBytes()),
            manifesto.sha256,
            "o resumo do manifesto não é o do catálogo — correr o `construir.mjs` outra vez",
        )
    }

    @Test
    fun `as porcoes chegaram, e nenhuma delas e impossivel`() {
        val alimentos = catalogo().alimentos
        val comPorcao = alimentos.count { it.servingGrams != null && !it.servingName.isNullOrBlank() }

        // Antes da junção com a tabela do FoodData Central eram 297 — 3,7 % do catálogo, e
        // para os outros 96 % registar era escrever gramas à mão. Se este número cair, a
        // junção deixou de encontrar os alimentos e ninguém dá por isso.
        assertTrue(comPorcao > MINIMO_DE_PORCOES, "só $comPorcao alimentos com porção")

        val impossiveis = alimentos
            .filter { it.servingGrams != null && (it.servingGrams!! <= 0 || it.servingGrams!! > MAXIMO_G) }
            .map { "${it.id}=${it.servingGrams}" }
        assertEquals(emptyList(), impossiveis, "porções que ninguém come de uma vez")
    }

    private companion object {
        const val MINIMO_PLAUSIVEL = 7000

        // Contados a 2026-08-24: 2 101 alimentos com porção, de 8 011.
        const val MINIMO_DE_PORCOES = 1500

        // Dois quilos. Acima disto não é uma porção — é uma embalagem inteira, e a tabela
        // publica algumas dessas.
        const val MAXIMO_G = 2000.0
    }
}
