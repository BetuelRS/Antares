package pt.antares.app.feature.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pt.antares.app.core.ai.AiClient
import pt.antares.app.core.ai.AiFoodItem
import pt.antares.app.core.ai.AiRepository
import pt.antares.app.core.ai.AiUsage
import pt.antares.app.core.ai.ExerciseAnalysis
import pt.antares.app.core.ai.FoodAnalysis
import pt.antares.app.core.ai.LabelAnalysis
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.FoodSource
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult
import pt.antares.app.feature.templates.ItemDeModelo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A revisão da AI, que nesta versão passou de «ver e apagar» para «corrigir».
 *
 * O que estes testes protegem não é o ecrã: é que **cada correcção chega ao registo**. Um
 * campo de gramas que não escala os macros, ou uma troca que não leva o `foodId`, dão um
 * registo errado sem erro nenhum — que é como esta parte da app falha.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiViewModelTest {

    /**
     * O `viewModelScope` corre no despachante principal, que num teste não existe. Sem isto
     * cada `launch` do ViewModel fica por correr e o estado nunca sai do inicial — o que
     * dava testes a falhar com listas vazias em vez de com o valor errado.
     */
    @BeforeTest
    fun arrancaOPrincipal() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun largaOPrincipal() {
        Dispatchers.resetMain()
    }

    // ---- andaimes ------------------------------------------------------------------

    private fun item(
        name: String = "arroz cozido",
        grams: Double = 150.0,
        kcal: Int = 195,
        foodId: String? = null,
    ) = AiFoodItem(
        name = name,
        matchedSource = "CIQUAL",
        grams = grams,
        kcal = kcal,
        protein = 4.0,
        carbs = 42.0,
        fat = 0.5,
        confidence = 0.9,
        estimated = false,
        foodId = foodId,
    )

    private fun food(id: String = "ciqual-9104", nome: String = "Arroz, branco, cozido") = FoodEntity(
        id = id,
        source = FoodSource.SEED,
        sourceRef = null,
        namePt = nome,
        nameEn = "Rice, white, cooked",
        brand = null,
        kcal = 130,
        proteinG = 2.7,
        carbsG = 28.0,
        sugarsG = null,
        fatG = 0.3,
        satFatG = null,
        microsJson = """{"FE":0.2}""",
        servingName = "chávena",
        servingGrams = 200.0,
        updatedAt = 0,
    )

    private class ClienteQueDevolve(private val itens: List<AiFoodItem>) : AiClient {
        override suspend fun analyzeFoodText(text: String, lang: String, day: String) =
            AppResult.Success(FoodAnalysis(items = itens, usage = AiUsage(1, 30, true)))

        override suspend fun analyzeFoodPhoto(
            imageBase64: String,
            mime: String,
            lang: String,
            day: String,
        ) = AppResult.Success(FoodAnalysis(items = itens, usage = AiUsage(1, 30, true)))

        override suspend fun readLabel(imageBase64: String, mime: String, lang: String, day: String):
            AppResult<LabelAnalysis> = AppResult.Failure(AppError.Unknown("não é preciso aqui"))

        override suspend fun analyzeExercise(
            text: String,
            weightKg: Double,
            lang: String,
            day: String,
        ): AppResult<ExerciseAnalysis> = AppResult.Failure(AppError.Unknown("não é preciso aqui"))
    }

    private class Registos {
        val linhas = mutableListOf<FoodLogEntity>()
        val fotosGravadas = mutableListOf<String>()
        var modelo: Triple<String, MealSlot, List<ItemDeModelo>>? = null
    }

    private fun vm(
        itens: List<AiFoodItem> = listOf(item()),
        catalogo: List<FoodEntity> = listOf(food()),
        registos: Registos = Registos(),
    ): Pair<AiViewModel, Registos> {
        val repo = AiRepository(
            client = ClienteQueDevolve(itens),
            ensureAccount = {},
            saveFoodLog = { registos.linhas += it },
            latestWeightKg = { 70.0 },
            persistUsage = { _, _ -> },
            savePhoto = { id, base64 ->
                registos.fotosGravadas += base64
                "/fotos/$id.jpg"
            },
            io = Dispatchers.Unconfined,
            newId = { "id-${registos.linhas.size}" },
            now = { 0L },
        )
        val viewModel = AiViewModel(
            repository = repo,
            procurarNoCatalogo = { catalogo },
            guardarModelo = { nome, slot, lista ->
                registos.modelo = Triple(nome, slot, lista)
                "modelo-1"
            },
        )
        return viewModel to registos
    }

    private suspend fun emRevisao(
        itens: List<AiFoodItem> = listOf(item()),
        catalogo: List<FoodEntity> = listOf(food()),
    ): Pair<AiViewModel, Registos> {
        val (viewModel, registos) = vm(itens, catalogo)
        viewModel.onTextChange("arroz")
        viewModel.analyzeText()
        return viewModel to registos
    }

    // ---- a voz ---------------------------------------------------------------------

    /**
     * O ditado fica escrito e **não** dispara a análise.
     *
     * É a resposta do dono à pergunta de abertura desta versão: o reconhecimento de voz
     * ouve mal com frequência, e uma análise disparada por engano gasta uma utilização da
     * quota que não volta.
     */
    @Test
    fun `o ditado enche o campo e fica em INPUT`() = runTest {
        val (viewModel, _) = vm()
        viewModel.prefill("dois ovos mexidos e uma torrada")

        assertEquals(AiPhase.INPUT, viewModel.state.value.phase)
        assertEquals("dois ovos mexidos e uma torrada", viewModel.state.value.text)
    }

    /** Voltar ao ecrã não apaga o que já lá estava escrito à mão. */
    @Test
    fun `o ditado nao escreve por cima do que ja estava`() = runTest {
        val (viewModel, _) = vm()
        viewModel.onTextChange("uma sopa")
        viewModel.prefill("outra coisa qualquer")

        assertEquals("uma sopa", viewModel.state.value.text)
    }

    // ---- o campo de gramas ---------------------------------------------------------

    @Test
    fun `escrever as gramas reescala os macros`() = runTest {
        val (viewModel, _) = emRevisao()
        viewModel.onGramsText(0, "300")

        val item = viewModel.state.value.items[0]
        assertEquals(300.0, item.grams)
        assertEquals(390, item.kcal, "195 kcal em 150 g são 390 em 300")
        assertEquals(84.0, item.carbs, 0.001)
    }

    /**
     * Um campo vazio é um estado legítimo — é por onde passa quem apaga «30» para escrever
     * «180». Recusá-lo tornava o campo impossível de limpar.
     */
    @Test
    fun `apagar o campo nao mexe no numero`() = runTest {
        val (viewModel, _) = emRevisao()
        viewModel.onGramsText(0, "")

        assertEquals("", viewModel.state.value.gramasTexto[0])
        assertEquals(150.0, viewModel.state.value.items[0].grams, "o número esperou pelo próximo")
    }

    @Test
    fun `a virgula conta como separador decimal`() = runTest {
        val (viewModel, _) = emRevisao()
        viewModel.onGramsText(0, "37,5")

        assertEquals(37.5, viewModel.state.value.items[0].grams)
    }

    /**
     * Letras não entram num campo de gramas.
     *
     * Apareceu no emulador: com o campo em foco, o texto que se escrevia caía lá dentro e a
     * linha passava a dizer «Ovos e» onde devia dizer gramas. O teclado do campo é o dos
     * números, mas um teclado de hardware escreve o que quiser.
     */
    @Test
    fun `o campo de gramas so aceita numeros`() = runTest {
        val (viewModel, _) = emRevisao()
        viewModel.onGramsText(0, "Ovos e")

        assertEquals("", viewModel.state.value.gramasTexto[0])
        assertEquals(150.0, viewModel.state.value.items[0].grams, "o número não se mexeu")
    }

    /** «12a3» é um engano de dedo, e o que se aproveita são os algarismos. */
    @Test
    fun `os algarismos aproveitam-se do que vem misturado`() = runTest {
        val (viewModel, _) = emRevisao()
        viewModel.onGramsText(0, "12a3")

        assertEquals("123", viewModel.state.value.gramasTexto[0])
        assertEquals(123.0, viewModel.state.value.items[0].grams)
    }

    /** Um segundo separador não é número nenhum, e deixá-lo entrar mentia sobre o valor. */
    @Test
    fun `so ha um separador decimal`() = runTest {
        val (viewModel, _) = emRevisao()
        viewModel.onGramsText(0, "37,5,2")

        assertEquals("37,52", viewModel.state.value.gramasTexto[0])
        assertEquals(37.52, viewModel.state.value.items[0].grams, 0.0001)
    }

    /**
     * O texto e os itens andam sempre ao mesmo comprimento.
     *
     * Sem isto, remover o primeiro item deixava o campo do segundo a mostrar as gramas do
     * que saiu — e a pessoa corrigia um número que já não era o dela.
     */
    @Test
    fun `remover um item leva o texto das gramas atras`() = runTest {
        val (viewModel, _) = emRevisao(
            itens = listOf(item("arroz", 150.0), item("frango", 200.0, kcal = 330)),
        )
        assertEquals(listOf("150", "200"), viewModel.state.value.gramasTexto)

        viewModel.removeItem(0)

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(listOf("200"), viewModel.state.value.gramasTexto)
    }

    // ---- trocar e acrescentar ------------------------------------------------------

    /**
     * A troca liga o registo ao catálogo. É o que o plano não previa: até aqui **tudo** o
     * que a AI gravava ia com `foodId` nulo, e um registo sem `foodId` não conta para os
     * «mais registados» nem herda a porção habitual do alimento.
     */
    @Test
    fun `trocar um item leva o foodId ate ao registo`() = runTest {
        val (viewModel, registos) = emRevisao()
        viewModel.abrirTroca(0)
        viewModel.procurar("arroz")
        viewModel.escolher(viewModel.state.value.procura!!.resultados.first())

        val trocado = viewModel.state.value.items.single()
        assertEquals("ciqual-9104", trocado.foodId)
        assertEquals("Arroz, branco, cozido", trocado.name)
        assertEquals(200.0, trocado.grams, "a porção do alimento, que é uma chávena")
        assertEquals(260, trocado.kcal, "130 kcal por 100 g dão 260 em 200")
        assertNull(viewModel.state.value.procura, "a procura fecha-se ao escolher")

        viewModel.confirm(MealSlot.LUNCH, epochDay = 20_000)
        assertEquals("ciqual-9104", registos.linhas.single().foodId)
    }

    /** Um item vindo do catálogo é uma medição, e deixa de estar marcado para revisão. */
    @Test
    fun `o item trocado deixa de pedir revisao`() = runTest {
        val (viewModel, _) = emRevisao()
        viewModel.abrirTroca(0)
        viewModel.procurar("arroz")
        viewModel.escolher(viewModel.state.value.procura!!.resultados.first())

        assertTrue(!viewModel.state.value.items.single().needsReview)
    }

    /**
     * Acrescentar é a mesma folha sem alvo. Existe porque o modelo omite: o arroz tapado
     * pela carne na fotografia não aparece em lista nenhuma.
     */
    @Test
    fun `acrescentar junta um item em vez de substituir`() = runTest {
        val (viewModel, _) = emRevisao()
        viewModel.abrirAcrescento()
        assertEquals(null, viewModel.state.value.procura?.alvo)

        viewModel.procurar("arroz")
        viewModel.escolher(food(id = "ciqual-1", nome = "Pão de trigo"))

        assertEquals(2, viewModel.state.value.items.size)
        assertEquals(2, viewModel.state.value.gramasTexto.size)
        assertEquals("Pão de trigo", viewModel.state.value.items[1].name)
    }

    /** Os micronutrientes do catálogo vêm por 100 g e entram por porção, como os da AI. */
    @Test
    fun `a troca escala os micronutrientes para a porcao`() = runTest {
        val (viewModel, _) = emRevisao()
        viewModel.abrirTroca(0)
        viewModel.procurar("arroz")
        viewModel.escolher(viewModel.state.value.procura!!.resultados.first())

        assertEquals(0.4, viewModel.state.value.items.single().micros?.get("FE") ?: 0.0, 0.0001)
    }

    // ---- guardar como refeição -----------------------------------------------------

    /**
     * Guarda **estes** itens, e não o slot do dia.
     *
     * O método que já existia lia a refeição inteira do diário: quem tivesse registado o
     * pão às oito ficava com ele dentro de um modelo chamado «Almoço», e o nome deixava de
     * descrever o que lá está.
     */
    @Test
    fun `guardar como refeicao guarda os itens da revisao`() = runTest {
        val (viewModel, registos) = emRevisao(
            itens = listOf(item("arroz", 150.0), item("frango", 200.0, kcal = 330)),
        )
        viewModel.guardarComoRefeicao("Almoço de segunda", MealSlot.LUNCH)

        val (nome, slot, itens) = assertNotNull(registos.modelo)
        assertEquals("Almoço de segunda", nome)
        assertEquals(MealSlot.LUNCH, slot)
        assertEquals(listOf("arroz", "frango"), itens.map { it.nome })
        assertEquals("Almoço de segunda", viewModel.state.value.guardadaComoRefeicao)
    }

    @Test
    fun `um modelo sem nome nao se guarda`() = runTest {
        val (viewModel, registos) = emRevisao()
        viewModel.guardarComoRefeicao("   ", MealSlot.LUNCH)

        assertNull(registos.modelo)
    }

    // ---- a fotografia --------------------------------------------------------------

    /**
     * Uma fotografia, um ficheiro, e o mesmo caminho em todos os registos que saíram dela.
     *
     * Gravá-la por item dava três cópias da mesma imagem em disco, e apagar um registo
     * deixava as outras duas sem dono.
     */
    @Test
    fun `a foto grava-se uma vez e o caminho repete-se nos registos`() = runTest {
        val (viewModel, registos) = vm(itens = listOf(item("arroz"), item("frango")))
        viewModel.analyzePhoto("base64-do-prato", "image/jpeg")
        viewModel.confirm(MealSlot.DINNER, epochDay = 20_000)

        assertEquals(1, registos.fotosGravadas.size, "uma foto, um ficheiro")
        assertEquals(2, registos.linhas.size)
        assertEquals(
            1,
            registos.linhas.mapNotNull { it.photoPath }.toSet().size,
            "os dois registos apontam para a mesma imagem",
        )
    }

    /** O que se escreve não tem retrato, e a coluna fica nula — que é o estado normal. */
    @Test
    fun `o registo por texto fica sem foto`() = runTest {
        val (viewModel, registos) = emRevisao()
        viewModel.confirm(MealSlot.LUNCH, epochDay = 20_000)

        assertNull(registos.linhas.single().photoPath)
        assertTrue(registos.fotosGravadas.isEmpty())
    }
}
