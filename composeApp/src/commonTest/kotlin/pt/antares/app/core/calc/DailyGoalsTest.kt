package pt.antares.app.core.calc

import pt.antares.app.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyGoalsTest {

    @Test
    fun `a agua parte da ingestao adequada da EFSA e nao de uma regra por quilo`() {
        // Mudança intencional. Eram 35 ml por quilo, iguais para homens e mulheres: 80 kg
        // davam 2800 ml só de bebida, cerca de 40% acima da referência que a app usa para
        // tudo o resto. A EFSA dá 2,5 L de água total nos homens e 2,0 L nas mulheres.
        assertEquals(2500, DailyGoals.waterMl(Sex.MALE, weightKg = 70.0))
        assertEquals(2000, DailyGoals.waterMl(Sex.FEMALE, weightKg = 70.0))
    }

    @Test
    fun `a 80 quilos o numero quase nao muda, mas o que ele significa muda todo`() {
        // Antes: 2800 ml, e **só de bebida**. Agora: 2850, de **água total** — sopa, fruta
        // e o resto da comida contam para lá. Na prática pede-se menos de copo, e é por
        // isso que o ecrã tem de dizer que a meta é de água total.
        assertEquals(2850, DailyGoals.waterMl(Sex.MALE, 80.0))
    }

    @Test
    fun `escala com o peso, que e da app e nao da EFSA`() {
        val leve = DailyGoals.waterMl(Sex.MALE, 50.0)
        val referencia = DailyGoals.waterMl(Sex.MALE, DailyGoals.WATER_REFERENCE_WEIGHT_KG)
        val pesado = DailyGoals.waterMl(Sex.MALE, 110.0)

        assertTrue(leve < referencia, "pedir o mesmo a 50 kg e a 110 é pior do que escalar")
        assertTrue(pesado > referencia)
        assertEquals(DailyGoals.WATER_EFSA_MALE_ML, referencia, "no peso de referência dá o da EFSA")
    }

    @Test
    fun `um dia de treino pede meio litro a mais`() {
        val parado = DailyGoals.waterMl(Sex.FEMALE, 70.0, treinouHoje = false)
        val treinou = DailyGoals.waterMl(Sex.FEMALE, 70.0, treinouHoje = true)

        assertEquals(DailyGoals.WATER_TRAINING_BONUS_ML, treinou - parado)
    }

    @Test
    fun `o resultado e sempre multiplo de 50`() {
        for (kg in 40..150) {
            for (sex in Sex.entries) {
                val ml = DailyGoals.waterMl(sex, kg.toDouble(), treinouHoje = kg % 2 == 0)
                assertEquals(0, ml % DailyGoals.WATER_ROUNDING_ML, "$kg kg deu $ml ml")
            }
        }
    }

    @Test
    fun `peso invalido nao produz meta`() {
        assertEquals(0, DailyGoals.waterMl(Sex.MALE, 0.0))
        assertEquals(0, DailyGoals.waterMl(Sex.FEMALE, -5.0))
    }

    @Test
    fun `mais peso nunca da menos agua`() {
        for (sex in Sex.entries) {
            var anterior = 0
            for (kg in 40..150) {
                val ml = DailyGoals.waterMl(sex, kg.toDouble())
                assertTrue(ml >= anterior, "a $kg kg a meta desceu para $ml")
                anterior = ml
            }
        }
    }

    @Test
    fun `a fibra e a mesma para toda a gente`() {
        assertEquals(25, DailyGoals.fibreG())
    }
}
