package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.RecipeEntity
import pt.antares.app.core.database.entities.RecipeIngredientEntity
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RecipeDaosTest {

    private lateinit var db: AntaresDb

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `receita e ingredientes persistem e leem por receita`() = runTest {
        val recipes = db.recipeDao()
        val ingredients = db.recipeIngredientDao()

        recipes.upsert(RecipeEntity(id = "r1", name = "Arroz de frango", yieldGrams = 280.0, updatedAt = 1))
        ingredients.upsert(RecipeIngredientEntity("i1", "r1", "arroz", 100.0, updatedAt = 1))
        ingredients.upsert(RecipeIngredientEntity("i2", "r1", "frango", 200.0, updatedAt = 2))

        ingredients.upsert(RecipeIngredientEntity("i3", "r2", "outro", 50.0, updatedAt = 3))

        assertEquals("Arroz de frango", recipes.byId("r1")?.name)
        assertEquals(2, ingredients.forRecipe("r1").size)
        assertEquals(listOf("arroz", "frango"), ingredients.observeForRecipe("r1").first().map { it.foodId })
    }

    @Test
    fun `soft delete esconde receita e ingrediente mas mantem tombstone`() = runTest {
        val recipes = db.recipeDao()
        val ingredients = db.recipeIngredientDao()
        recipes.upsert(RecipeEntity("r1", "Teste", null, updatedAt = 1))
        ingredients.upsert(RecipeIngredientEntity("i1", "r1", "f1", 100.0, updatedAt = 1))

        recipes.softDelete("r1", now = 5)
        ingredients.softDelete("i1", now = 5)

        assertEquals(0, recipes.observeAll().first().size)
        assertEquals(0, ingredients.observeForRecipe("r1").first().size)
    }
}
