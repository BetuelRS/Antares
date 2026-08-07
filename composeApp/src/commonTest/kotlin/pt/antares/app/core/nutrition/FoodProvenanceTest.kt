package pt.antares.app.core.nutrition

import pt.antares.app.core.model.FoodSource
import kotlin.test.Test
import kotlin.test.assertEquals

class FoodProvenanceTest {

    @Test
    fun `prefixo do id identifica a tabela do seed`() {
        assertEquals(FoodProvenance.CIQUAL, FoodProvenance.of(FoodSource.SEED, "ciqual-4003"))
        assertEquals(FoodProvenance.USDA, FoodProvenance.of(FoodSource.SEED, "usda-171413"))
        assertEquals(FoodProvenance.CURATED, FoodProvenance.of(FoodSource.SEED, "ptx3_feijoada_brasileira"))
        assertEquals(FoodProvenance.CURATED, FoodProvenance.of(FoodSource.SEED, "pt-galao"))
    }

    @Test
    fun `origem manda sobre o prefixo`() {

        assertEquals(FoodProvenance.OFF, FoodProvenance.of(FoodSource.OFF, "ciqual-4003"))
        assertEquals(FoodProvenance.AI, FoodProvenance.of(FoodSource.AI_ESTIMATE, "usda-1"))
        assertEquals(FoodProvenance.USER, FoodProvenance.of(FoodSource.CUSTOM, "ptx_galao"))
    }

    @Test
    fun `id sem prefixo conhecido nao inventa origem`() {
        assertEquals(FoodProvenance.UNKNOWN, FoodProvenance.of(FoodSource.SEED, "abc123"))
    }
}
