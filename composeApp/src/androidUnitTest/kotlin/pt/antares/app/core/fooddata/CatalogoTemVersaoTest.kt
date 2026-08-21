package pt.antares.app.core.fooddata

import kotlinx.serialization.json.Json
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

    private companion object {
        const val MINIMO_PLAUSIVEL = 7000
    }
}
