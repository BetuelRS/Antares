package pt.antares.app.feature.templates

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.MealSlot
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Guardar o que se acabou de rever, e **só** isso.
 *
 * O método que já existia — o [MealTemplateRepository.saveMealAsTemplate] — lê a refeição
 * inteira do diário, e é o que se quer quando se guarda uma refeição já registada. Usá-lo
 * no fim da revisão da AI era o defeito calado desta versão: quem tivesse registado o pão
 * às oito ficava com ele dentro de um modelo chamado «Almoço», sem erro nenhum e sem ver.
 */
@RunWith(RobolectricTestRunner::class)
class GuardarItensComoModeloTest {

    private lateinit var db: AntaresDb
    private lateinit var repo: MealTemplateRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntaresDb::class.java)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        repo = MealTemplateRepository(
            db.foodLogDao(),
            db.mealTemplateDao(),
            db.mealTemplateItemDao(),
            Dispatchers.Default,
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun jaRegistado(nome: String, slot: MealSlot, epochDay: Long) =
        db.foodLogDao().upsert(
            FoodLogEntity(
                id = nome,
                epochDay = epochDay,
                mealSlot = slot,
                foodId = null,
                nameSnapshot = nome,
                quantityGrams = 50.0,
                kcalSnapshot = 130,
                proteinSnapshot = 4.0,
                carbsSnapshot = 25.0,
                fatSnapshot = 1.0,
                microsPer100Json = null,
                updatedAt = 1L,
            ),
        )

    private val arroz = ItemDeModelo(
        nome = "Arroz, branco, cozido",
        gramas = 200.0,
        kcal = 260,
        proteina = 5.4,
        hidratos = 56.0,
        gordura = 0.6,
        foodId = "ciqual-9104",
    )

    private val frango = ItemDeModelo(
        nome = "Frango, peito, grelhado",
        gramas = 150.0,
        kcal = 248,
        proteina = 46.5,
        hidratos = 0.0,
        gordura = 5.4,
    )

    @Test
    fun `guarda os itens que recebe, e nao o que ja estava no dia`() = runTest {
        jaRegistado("pão do pequeno-almoço", MealSlot.LUNCH, 20_000)

        val id = repo.saveItemsAsTemplate("Almoço de segunda", MealSlot.LUNCH, listOf(arroz, frango))

        val itens = repo.items(requireNotNull(id))
        assertEquals(
            listOf("Arroz, branco, cozido", "Frango, peito, grelhado"),
            itens.map { it.nameSnapshot },
            "o modelo trouxe o que já estava registado no dia",
        )
    }

    /** O `foodId` sobrevive: um item trocado no ecrã da AI continua ligado ao catálogo. */
    @Test
    fun `o foodId de um item trocado viaja para o modelo`() = runTest {
        val id = repo.saveItemsAsTemplate("Almoço", MealSlot.LUNCH, listOf(arroz, frango))
        val itens = repo.items(requireNotNull(id)).associateBy { it.nameSnapshot }

        assertEquals("ciqual-9104", itens.getValue("Arroz, branco, cozido").foodId)
        assertNull(itens.getValue("Frango, peito, grelhado").foodId)
    }

    /** Uma lista vazia não dá modelo nenhum: uma linha em branco na lista não serve a ninguém. */
    @Test
    fun `uma lista vazia nao cria modelo`() = runTest {
        assertNull(repo.saveItemsAsTemplate("Vazio", MealSlot.LUNCH, emptyList()))
    }

    /**
     * O outro método continua a fazer o que fazia. Não é o mesmo, e não se substitui:
     * guardar uma refeição já registada é ler o dia, e é a única coisa que serve aí.
     */
    @Test
    fun `o metodo antigo continua a ler o dia`() = runTest {
        jaRegistado("aveia", MealSlot.BREAKFAST, 20_000)

        val id = repo.saveMealAsTemplate("Pequeno-almoço", MealSlot.BREAKFAST, 20_000)

        assertEquals(listOf("aveia"), repo.items(requireNotNull(id)).map { it.nameSnapshot })
    }
}
