package pt.antares.app.core.fooddata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrinkClassifierTest {

    @Test
    fun `bebidas obvias sao liquido`() {
        assertTrue(DrinkClassifier.isLiquid("Água mineral", "Mineral water"))
        assertTrue(DrinkClassifier.isLiquid("Sumo de laranja", "Orange juice"))
        assertTrue(DrinkClassifier.isLiquid("Leite meio-gordo", "Milk, semi-skimmed"))
        assertTrue(DrinkClassifier.isLiquid("Café expresso", "Espresso coffee"))
        assertTrue(DrinkClassifier.isLiquid("Cerveja", "Beer"))
        assertTrue(DrinkClassifier.isLiquid("Coca-Cola", "Cola soda"))
    }

    @Test
    fun `solidos com termo de bebida nao sao liquido`() {
        assertFalse(DrinkClassifier.isLiquid("Leite em pó", "Milk powder"))
        assertFalse(DrinkClassifier.isLiquid("Chocolate de leite", "Milk chocolate"))
        assertFalse(DrinkClassifier.isLiquid("Queijo", "Cheese"))
        assertFalse(DrinkClassifier.isLiquid("Gelado de café", "Coffee ice cream"))

        assertFalse(DrinkClassifier.isLiquid("", "Puddings, banana, dry mix, prepared with 2% milk"))
        assertFalse(DrinkClassifier.isLiquid("", "Frozen novelties, juice type, juice with cream"))
        assertFalse(DrinkClassifier.isLiquid("", "Tomato sauce with milk"))
    }

    @Test
    fun `comida normal nao e liquido`() {
        assertFalse(DrinkClassifier.isLiquid("Arroz branco cozido", "White rice, cooked"))
        assertFalse(DrinkClassifier.isLiquid("Frango grelhado", "Grilled chicken"))
        assertFalse(DrinkClassifier.isLiquid("", ""))
    }

    @Test
    fun `nao apanha termo dentro de outra palavra`() {

        assertFalse(DrinkClassifier.isLiquid("Chalota", "Shallot"))
        assertFalse(DrinkClassifier.isLiquid("Linguiça", "Sausage"))
    }
}

class UsdaNameCleanerTest {

    @Test
    fun `reordena base e adjetivo conhecido`() {
        assertEquals("White rice, cooked", UsdaNameCleaner.clean("Rice, white, cooked"))
        assertEquals("Ground beef, 80% lean", UsdaNameCleaner.clean("Beef, ground, 80% lean"))
    }

    @Test
    fun `nao reordena quando a 2a parte nao e adjetivo conhecido`() {

        assertEquals("Cheerios, cereal", UsdaNameCleaner.clean("CHEERIOS, cereal"))
    }

    @Test
    fun `tira maiusculas gritadas de marcas`() {
        assertEquals("Nestle yogurt", UsdaNameCleaner.clean("NESTLE YOGURT"))
    }

    @Test
    fun `corta sufixos redundantes`() {
        assertEquals("White rice", UsdaNameCleaner.clean("Rice, white, all commercial varieties"))
        assertEquals("Apple", UsdaNameCleaner.clean("Apple, raw"))
    }

    @Test
    fun `corta o boilerplate do USDA FDP`() {
        assertEquals(
            "Pears, bartlett",
            UsdaNameCleaner.clean("Pears, bartlett (Includes foods for USDA's Food Distribution Program)"),
        )
    }

    @Test
    fun `so ruido devolve o original (nao fica vazio)`() {

        assertEquals("raw", UsdaNameCleaner.clean("raw"))
    }
}
