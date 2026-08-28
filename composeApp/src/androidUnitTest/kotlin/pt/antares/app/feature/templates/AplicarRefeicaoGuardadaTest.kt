package pt.antares.app.feature.templates

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
import pt.antares.app.core.database.AntaresDb
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.MealSlot
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Aplicar uma refeição guardada: com multiplicador, e com volta atrás.
 *
 * O que estes testes defendem é o **desfazer**, e ele não era possível: o `applyTemplate`
 * devolvia quantos registos tinha criado e deitava fora os identificadores. Aplicar sete
 * itens ao dia errado obrigava a apagar sete linhas à mão, à procura de quais tinham
 * acabado de entrar no meio das que já lá estavam.
 */
@RunWith(RobolectricTestRunner::class)
class AplicarRefeicaoGuardadaTest {

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

    private suspend fun registar(id: String, nome: String, kcal: Int, gramas: Double) =
        db.foodLogDao().upsert(
            FoodLogEntity(
                id = id,
                epochDay = 100,
                mealSlot = MealSlot.LUNCH,
                foodId = null,
                nameSnapshot = nome,
                quantityGrams = gramas,
                kcalSnapshot = kcal,
                proteinSnapshot = 10.0,
                carbsSnapshot = 20.0,
                fatSnapshot = 5.0,
                microsPer100Json = """{"FE":1.0}""",
                updatedAt = 1L,
            ),
        )

    private suspend fun modeloDeDoisItens(): String {
        registar("a", "Arroz", 200, 150.0)
        registar("b", "Frango", 330, 200.0)
        return requireNotNull(repo.saveMealAsTemplate("Almoço", MealSlot.LUNCH, 100))
    }

    // ---- desfazer -------------------------------------------------------------------

    /**
     * Aplicar devolve os registos que criou, e desfazer apaga **exactamente esses**.
     *
     * O dia já tinha comida antes: é isso que torna o teste útil. Um desfazer que apagasse
     * «o que está neste slot» levava o pequeno-almoço atrás.
     */
    @Test
    fun `desfazer apaga so o que a aplicacao criou`() = runTest {
        val templateId = modeloDeDoisItens()
        registar("ja-la-estava", "Pão", 90, 40.0)
        db.foodLogDao().upsert(
            db.foodLogDao().byId("ja-la-estava")!!.copy(epochDay = 200, mealSlot = MealSlot.DINNER),
        )

        val criados = repo.applyTemplate(templateId, MealSlot.DINNER, 200)
        assertEquals(2, criados.size)
        assertEquals(3, db.foodLogDao().mealLogs(200, MealSlot.DINNER).size)

        repo.desfazerAplicacao(criados)

        val sobra = db.foodLogDao().mealLogs(200, MealSlot.DINNER)
        assertEquals(listOf("Pão"), sobra.map { it.nameSnapshot })
    }

    /**
     * Apagar é marcar, como em todo o diário — e prova-se pelo único sítio por onde se vê:
     * a linha volta inteira.
     *
     * O `exportRows` não serve para isto, e foi o que este teste tentou primeiro: filtra
     * `deleted = 0`, como tudo o que lê o diário. Uma lápide não se vê de fora; vê-se a
     * ressuscitar.
     */
    @Test
    fun `desfazer marca, e a linha ainda la esta para voltar`() = runTest {
        val templateId = modeloDeDoisItens()
        val criados = repo.applyTemplate(templateId, MealSlot.DINNER, 200)

        repo.desfazerAplicacao(criados)
        assertTrue(db.foodLogDao().mealLogs(200, MealSlot.DINNER).isEmpty())

        criados.forEach { db.foodLogDao().restore(it, now = 9L) }

        val voltaram = db.foodLogDao().mealLogs(200, MealSlot.DINNER)
        assertEquals(2, voltaram.size)
        assertEquals(530, voltaram.sumOf { it.kcalSnapshot }, "voltou vazia de números")
    }

    // ---- multiplicador --------------------------------------------------------------

    @Test
    fun `meia refeicao escala as gramas e os macros`() = runTest {
        val templateId = modeloDeDoisItens()
        repo.applyTemplate(templateId, MealSlot.DINNER, 200, multiplicador = 0.5)

        val jantar = db.foodLogDao().mealLogs(200, MealSlot.DINNER).associateBy { it.nameSnapshot }
        assertEquals(75.0, jantar.getValue("Arroz").quantityGrams)
        assertEquals(100, jantar.getValue("Arroz").kcalSnapshot)
        assertEquals(5.0, jantar.getValue("Arroz").proteinSnapshot)
    }

    /**
     * Os micronutrientes **não** escalam, e é de propósito.
     *
     * A base guarda-os por 100 g em todo o lado. Meia dose e dose e meia do mesmo alimento
     * valem o mesmo por 100 g — escalá-los aqui era contá-los duas vezes na leitura, que
     * volta a multiplicar pelas gramas.
     */
    @Test
    fun `os micronutrientes ficam por 100 g, sem escalar`() = runTest {
        val templateId = modeloDeDoisItens()
        repo.applyTemplate(templateId, MealSlot.DINNER, 200, multiplicador = 2.0)

        val linha = db.foodLogDao().mealLogs(200, MealSlot.DINNER).first()
        assertEquals("""{"FE":1.0}""", linha.microsPer100Json)
    }

    /**
     * Zero e negativo valem um.
     *
     * Aplicar zero vezes escrevia dois registos de zero calorias — ficavam no diário a somar
     * nada e a ocupar a lista, que é pior do que não escrever nada.
     */
    @Test
    fun `zero e negativo nao encolhem a refeicao a nada`() = runTest {
        val templateId = modeloDeDoisItens()
        repo.applyTemplate(templateId, MealSlot.DINNER, 200, multiplicador = 0.0)
        repo.applyTemplate(templateId, MealSlot.SNACK, 200, multiplicador = -3.0)

        assertEquals(530, db.foodLogDao().mealLogs(200, MealSlot.DINNER).sumOf { it.kcalSnapshot })
        assertEquals(530, db.foodLogDao().mealLogs(200, MealSlot.SNACK).sumOf { it.kcalSnapshot })
    }

    // ---- o resumo da linha ----------------------------------------------------------

    /**
     * A linha da lista sabe quantos itens e quantas calorias sem abrir o modelo.
     *
     * Dizia o nome e a refeição do dia — «Almoço» —, que é a coisa menos útil que se pode
     * dizer sobre uma lista chamada «Almoço». Quem escolhe entre duas escolhe pelo tamanho.
     */
    @Test
    fun `o resumo conta os itens e soma as calorias`() = runTest {
        modeloDeDoisItens()

        val resumo = repo.observeResumos().first().single()
        assertEquals("Almoço", resumo.modelo.name)
        assertEquals(2, resumo.itens)
        assertEquals(530, resumo.kcal)
    }

    /**
     * Um modelo sem itens aparece na lista com zeros, e não desaparece dela.
     *
     * A agregação é uma junção interna e não o traz; se a lista viesse só dela, um modelo
     * vazio ficava invisível — e um modelo invisível não se consegue apagar.
     */
    @Test
    fun `um modelo vazio continua na lista, com zeros`() = runTest {
        val templateId = modeloDeDoisItens()
        val ts = 9L
        repo.items(templateId).forEach { db.mealTemplateItemDao().softDelete(it.id, ts) }

        val resumo = repo.observeResumos().first().single()
        assertEquals(0, resumo.itens)
        assertEquals(0, resumo.kcal)
    }
}
