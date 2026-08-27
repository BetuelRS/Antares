package pt.antares.app.feature.fooddata

import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.FoodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A pesquisa agrupa o mesmo alimento em estados diferentes.
 *
 * Uma procura por «frango» dava sete linhas quase iguais, e o argumento da confeção era
 * exactamente esse: o cru e o cozinhado não são dois alimentos. Faltava a lista saber disso.
 *
 * O que estes testes protegem é sobretudo o que **não** se agrupa: o agrupamento errado
 * esconde comida, e uma linha escondida não dá erro nenhum.
 */
class EstadosDoAlimentoTest {

    private fun food(id: String, nome: String, kcal: Int = 100, marca: String? = null) = FoodEntity(
        id = id,
        source = FoodSource.SEED,
        sourceRef = null,
        namePt = nome,
        nameEn = nome,
        brand = marca,
        kcal = kcal,
        proteinG = 0.0,
        carbsG = 0.0,
        sugarsG = null,
        fatG = 0.0,
        satFatG = null,
        servingName = null,
        servingGrams = null,
        microsJson = null,
        updatedAt = 0L,
    )

    @Test
    fun `parte o estado do fim do nome`() {
        assertEquals("Frango, carne" to "cru", partirEstado("Frango, carne, cru"))
        assertEquals("Beterraba" to "cozinhada", partirEstado("Beterraba, cozinhada"))
    }

    /** «assado no forno» tem de ganhar a «assado», ou a base fica «Frango, no forno». */
    @Test
    fun `o estado mais longo ganha`() {
        assertEquals("Frango, carne e pele" to "assado no forno", partirEstado("Frango, carne e pele, assado no forno"))
    }

    @Test
    fun `um nome sem estado fica inteiro`() {
        assertEquals("Queijo camembert" to null, partirEstado("Queijo camembert"))
        assertEquals("Bolo de arroz" to null, partirEstado("Bolo de arroz"))
    }

    /** Um estado no meio do nome não é o estado do alimento — é parte do nome dele. */
    @Test
    fun `um estado a meio do nome nao se parte`() {
        val (base, estado) = partirEstado("Sanduíche, frango, legumes crus com maionese")
        assertEquals("Sanduíche, frango, legumes crus com maionese", base)
        assertEquals(null, estado)
    }

    @Test
    fun `agrupa os estados do mesmo alimento`() {
        val grupos = agruparEstados(
            listOf(
                food("ciqual-1", "Frango, carne, cru", 109),
                food("ciqual-2", "Frango, carne, assado no forno", 148),
            ),
        )

        assertEquals(1, grupos.size)
        assertEquals("Frango, carne, cru", grupos.first().principal.namePt)
        assertEquals(1, grupos.first().quantosOutros)
    }

    /**
     * A ordem de chegada manda em quem é o principal.
     *
     * A pesquisa já pôs à frente o que a pessoa marcou, o que usou há pouco e o que é
     * português. Reordenar aqui por estado deitava fora esse trabalho — e punha à frente um
     * alimento que a pessoa nunca escolheu.
     */
    @Test
    fun `o primeiro a chegar e o principal`() {
        val grupos = agruparEstados(
            listOf(
                food("ciqual-2", "Frango, carne, assado no forno", 148),
                food("ciqual-1", "Frango, carne, cru", 109),
            ),
        )

        assertEquals("Frango, carne, assado no forno", grupos.first().principal.namePt)
        assertEquals("Frango, carne, cru", grupos.first().outros.first().namePt)
    }

    /**
     * Entre os outros, o cru vem primeiro: é dele que a confeção sabe partir.
     *
     * O principal continua a ser o primeiro a chegar — aqui a cozinhada —, porque a ordem da
     * pesquisa manda nessa escolha. A ordem dos estados só decide onde não há mais nada a
     * distinguir, que é dentro da lista escondida.
     */
    @Test
    fun `entre os outros o cru vem a frente`() {
        val grupos = agruparEstados(
            listOf(
                food("ciqual-3", "Batata, cozinhada"),
                food("ciqual-4", "Batata, frita"),
                food("ciqual-5", "Batata, crua"),
            ),
        )

        assertEquals("Batata, cozinhada", grupos.first().principal.namePt)
        assertEquals(
            listOf("Batata, crua", "Batata, frita"),
            grupos.first().outros.map { it.namePt },
        )
    }

    /**
     * Bases diferentes não se juntam, por muito parecidas que sejam.
     *
     * «Frango, carne» tem 2,9 g de gordura e «Frango, carne e pele» tem 15,1. Agrupá-los
     * escondia o segundo atrás do primeiro, e quem comeu a pele registava menos de metade
     * da gordura sem nada no ecrã a dizê-lo.
     */
    @Test
    fun `bases diferentes ficam separadas`() {
        val grupos = agruparEstados(
            listOf(
                food("ciqual-1", "Frango, carne, cru"),
                food("ciqual-6", "Frango, carne e pele, cru"),
            ),
        )

        assertEquals(2, grupos.size)
    }

    /**
     * Duas fontes com o mesmo nome não são estados uma da outra.
     *
     * A batata assada da CIQUAL tem 0,1 g de gordura e a da TCA tem 4,8 — foi por isso que a
     * arbitragem do bloco D as manteve às duas, com nomes desambiguados. Escondê-las aqui
     * desfazia essa decisão pela porta do ecrã.
     */
    @Test
    fun `fontes diferentes nao se agrupam`() {
        val grupos = agruparEstados(
            listOf(
                food("ciqual-7", "Batata, assada", 93),
                food("tca-7", "Batata, assada", 120),
            ),
        )

        assertEquals(2, grupos.size)
    }

    /** Marcas diferentes também não: são produtos distintos com o mesmo nome genérico. */
    @Test
    fun `marcas diferentes nao se agrupam`() {
        val grupos = agruparEstados(
            listOf(
                food("off-1", "Milho, cozido", marca = "Marca A"),
                food("off-2", "Milho, cru", marca = "Marca B"),
            ),
        )

        assertEquals(2, grupos.size)
    }

    @Test
    fun `alimentos sem estado ficam cada um no seu grupo`() {
        val grupos = agruparEstados(
            listOf(
                food("a", "Queijo camembert"),
                food("b", "Pão de forma"),
            ),
        )

        assertEquals(2, grupos.size)
        assertTrue(grupos.all { it.quantosOutros == 0 })
    }

    /**
     * A ordem geral da lista não muda — e é o que impede o agrupamento de reordenar a
     * pesquisa por baixo de quem a leu.
     */
    @Test
    fun `a ordem dos resultados mantem-se`() {
        val grupos = agruparEstados(
            listOf(
                food("a", "Arroz branco"),
                food("ciqual-1", "Frango, carne, cru"),
                food("b", "Azeite"),
                food("ciqual-2", "Frango, carne, assado"),
            ),
        )

        assertEquals(
            listOf("Arroz branco", "Frango, carne, cru", "Azeite"),
            grupos.map { it.principal.namePt },
        )
    }

    @Test
    fun `uma lista vazia da uma lista vazia`() {
        assertTrue(agruparEstados(emptyList()).isEmpty())
    }
}
