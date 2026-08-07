package pt.antares.app.feature.fooddata

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FoodEditValidationTest {

    @Test
    fun `estado vazio e invalido`() {
        assertFalse(FoodEditState().valid)
    }

    @Test
    fun `nome curto invalida`() {
        val s = FoodEditState(name = "O", kcal = "155", protein = "13", carbs = "1", fat = "11")
        assertFalse(s.valid)
    }

    @Test
    fun `kcal fora do intervalo invalida`() {

        val s = FoodEditState(name = "Ovo", kcal = "950", protein = "13", carbs = "1", fat = "11")
        assertFalse(s.valid)
    }

    @Test
    fun `macro nao numerico invalida`() {
        val s = FoodEditState(name = "Ovo", kcal = "155", protein = "abc", carbs = "1", fat = "11")
        assertFalse(s.valid)
    }

    @Test
    fun `valores coerentes validam`() {
        val s = FoodEditState(name = "Ovo", kcal = "155", protein = "13", carbs = "1", fat = "11")
        assertTrue(s.valid)
    }

    @Test
    fun `aceita virgula decimal`() {
        val s = FoodEditState(name = "Leite", kcal = "64", protein = "3,3", carbs = "4,8", fat = "3,6")
        assertTrue(s.valid)
    }

    @Test
    fun `kcal coerente com atwater nao alerta`() {

        val s = FoodEditState(name = "Ovo", kcal = "155", protein = "13", carbs = "1", fat = "11")
        assertFalse(s.kcalMismatch)
    }

    @Test
    fun `kcal muito divergente alerta`() {

        val s = FoodEditState(name = "Erro", kcal = "100", protein = "0", carbs = "0", fat = "0")
        assertTrue(s.kcalMismatch)
    }
}
