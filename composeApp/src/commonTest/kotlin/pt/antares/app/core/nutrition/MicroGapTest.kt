package pt.antares.app.core.nutrition

import pt.antares.app.core.model.FoodSource
import kotlin.test.Test
import kotlin.test.assertEquals

class MicroGapTest {

    @Test
    fun `com micros nao ha nada a explicar`() {
        assertEquals(MicroGap.NONE, MicroGap.of(FoodSource.OFF, hasMicros = true))
        assertEquals(MicroGap.NONE, MicroGap.of(FoodSource.SEED, hasMicros = true))
    }

    @Test
    fun `produto de codigo de barras culpa a lei dos rotulos`() {

        assertEquals(MicroGap.PACKAGED_LABEL, MicroGap.of(FoodSource.OFF, hasMicros = false))
    }

    @Test
    fun `estimativa da AI diz que foi estimativa`() {
        assertEquals(MicroGap.AI_ESTIMATE, MicroGap.of(FoodSource.AI_ESTIMATE, hasMicros = false))
    }

    @Test
    fun `alimento do proprio utilizador so tem o que ele escreveu`() {
        assertEquals(MicroGap.USER_CREATED, MicroGap.of(FoodSource.CUSTOM, hasMicros = false))
    }

    @Test
    fun `ficha de catalogo sem medicao diz isso mesmo`() {
        assertEquals(MicroGap.NOT_MEASURED, MicroGap.of(FoodSource.SEED, hasMicros = false))
    }
}
