package pt.antares.app.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import pt.antares.app.core.ai.AiFoodItem
import pt.antares.app.core.ai.AiRepository
import pt.antares.app.core.ai.AiUsage
import pt.antares.app.core.ai.AiWarnings
import pt.antares.app.core.ai.withGrams
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.nutrition.microsDeJson
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.AppResult
import pt.antares.app.feature.templates.ItemDeModelo
import kotlin.math.roundToInt

/**
 * As fases do fluxo de análise. `REVIEW` é a que importa: nada é gravado antes de a pessoa
 * passar por ela e confirmar — o que a AI devolve é uma proposta, não um registo.
 */
enum class AiPhase { INPUT, ANALYZING, REVIEW, ERROR }

/**
 * A procura que se abre por cima da revisão, para trocar um item ou acrescentar um em falta.
 *
 * [alvo] é o índice do item a substituir, ou **nulo para acrescentar** — é a única coisa que
 * separa as duas operações, e é por isso que são o mesmo estado.
 */
data class ProcuraDeItem(
    val alvo: Int?,
    val texto: String = "",
    val resultados: List<FoodEntity> = emptyList(),
    val aProcurar: Boolean = false,
) {
    val aTrocar: Boolean get() = alvo != null
}

data class AiState(
    val phase: AiPhase = AiPhase.INPUT,
    val text: String = "",

    val items: List<AiFoodItem> = emptyList(),

    /**
     * O que está escrito em cada campo de gramas, a par de [items] e sempre do mesmo
     * comprimento.
     *
     * Vive aqui e não dentro do campo porque um campo de texto não pode guardar o que a
     * pessoa está a meio de escrever se o dono do valor for outro: escrever «1» num campo
     * ligado a um número reescrevia logo o campo com «1» convertido, e apagar tudo para
     * pôr «180» dava um zero pelo caminho. O `AiViewModelTest` guarda o comprimento.
     */
    val gramasTexto: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val usage: AiUsage? = null,
    val error: AppError? = null,

    val inputError: Boolean = false,
    val saved: Boolean = false,

    /** A fotografia analisada, guardada até se confirmar — é ela que vai para o registo. */
    val photoBase64: String? = null,

    val procura: ProcuraDeItem? = null,

    /** O nome do modelo que se acabou de guardar, para o ecrã o poder dizer. */
    val guardadaComoRefeicao: String? = null,
) {
    val totalKcal: Int get() = items.sumOf { it.kcal }

    val canConfirm: Boolean get() = items.isNotEmpty()

    val notFood: Boolean get() = warnings.contains(AiWarnings.NOT_FOOD)

    /**
     * O modelo identificou alguma coisa vaga — «uma sandes», sem dizer de quê.
     *
     * **Era calculado aqui e lido em lado nenhum**, com a folha a perguntar directamente à
     * lista de avisos. A área 04 do estudo apanhou-o como defeito concreto: um estado que o
     * ecrã ignora é o sinal de que os dois divergiram, e o próximo a mexer num deles parte o
     * outro sem dar por isso. Agora é este que a folha lê, como já lia o [notFood].
     */
    val vague: Boolean get() = warnings.contains(AiWarnings.VAGUE_ITEM)

    /** A fotografia veio difícil de ler. Mesma razão do [vague]: um sítio só a decidir. */
    val imagemPoucoClara: Boolean get() = warnings.contains(AiWarnings.UNCLEAR_IMAGE)
}

class AiViewModel(
    private val repository: AiRepository,

    // Funções em vez dos repositórios inteiros, pela mesma razão do [AiRepository]: o que
    // este ecrã pode fazer à base fica escrito no módulo que o cria, e não escondido atrás
    // de duas classes com trinta métodos cada.
    private val procurarNoCatalogo: suspend (String) -> List<FoodEntity>,
    private val guardarModelo: suspend (String, MealSlot, List<ItemDeModelo>) -> String?,
) : ViewModel() {

    private val _state = MutableStateFlow(AiState())
    val state: StateFlow<AiState> = _state.asStateFlow()

    private var job: Job? = null

    private var procuraJob: Job? = null

    fun onTextChange(value: String) {

        _state.value = _state.value.copy(text = value.take(MAX_TEXT_CHARS))
    }

    /**
     * Enche o campo com o que se ditou, e fica em `INPUT`.
     *
     * Não analisa sozinho, por decisão do dono: o reconhecimento de voz ouve mal com
     * frequência, e uma análise disparada por engano gasta uma utilização da quota que não
     * se recupera. Um toque a mais é mais barato do que uma utilização deitada fora.
     *
     * Só enche um campo vazio — voltar ao ecrã não pode apagar o que já lá estava escrito.
     */
    fun prefill(value: String) {
        if (value.isBlank() || _state.value.text.isNotEmpty()) return
        onTextChange(value)
    }

    fun analyzeText() {
        val text = _state.value.text.trim()

        if (text.length < MIN_TEXT_CHARS) {
            _state.value = _state.value.copy(phase = AiPhase.ERROR, inputError = true, error = null)
            return
        }
        run(LogOrigin.AI_TEXT) { repository.analyzeText(text) }
    }

    fun analyzePhoto(base64: String, mime: String) {
        _state.value = _state.value.copy(photoBase64 = base64)
        run(LogOrigin.AI_PHOTO) { repository.analyzePhoto(base64, mime) }
    }

    private var origin: LogOrigin = LogOrigin.AI_TEXT

    private fun run(from: LogOrigin, block: suspend () -> AppResult<pt.antares.app.core.ai.FoodAnalysis>) {
        origin = from
        // Cancela a análise anterior: quem carrega duas vezes não pode ficar com a resposta
        // mais lenta a escrever por cima da mais recente.
        job?.cancel()
        _state.value = _state.value.copy(phase = AiPhase.ANALYZING, error = null, inputError = false)
        job = viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Success -> _state.value = _state.value.comItens(
                    result.value.items,
                ).copy(
                    phase = AiPhase.REVIEW,
                    warnings = result.value.warnings,
                    usage = result.value.usage,
                )

                is AppResult.Failure -> _state.value = _state.value.copy(
                    phase = AiPhase.ERROR,
                    error = result.error,
                )
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _state.value = _state.value.copy(phase = AiPhase.INPUT)
    }

    fun setGrams(index: Int, grams: Double) {
        val items = _state.value.items.toMutableList()
        val item = items.getOrNull(index) ?: return
        items[index] = item.withGrams(grams)
        _state.value = _state.value.comItens(items)
    }

    /**
     * O que a pessoa escreveu no campo de gramas.
     *
     * **Só entram algarismos e um separador decimal.** O teclado deste campo já é o dos
     * números, mas um teclado de hardware — ou o `adb input text` de um teste no aparelho —
     * escreve o que quiser, e o campo ficava a dizer «Ovos e» onde devia dizer gramas. Foi
     * assim que apareceu, a conduzir o emulador.
     *
     * O texto vazio guarda-se: apagar o campo para escrever outro valor passa por lá, e
     * recusar esse estado tornava o campo impossível de limpar. O número só muda quando o
     * texto **é** um número maior que zero.
     */
    fun onGramsText(index: Int, texto: String) {
        val estado = _state.value
        if (index !in estado.items.indices) return

        val textos = estado.gramasTexto.toMutableList()
        textos[index] = apenasNumero(texto).take(MAX_GRAMS_CHARS)

        val gramas = textos[index].replace(',', '.').toDoubleOrNull()
        val items = estado.items.toMutableList()
        if (gramas != null && gramas > 0) items[index] = items[index].withGrams(gramas)

        _state.value = estado.copy(items = items, gramasTexto = textos)
    }

    fun removeItem(index: Int) {
        val estado = _state.value
        if (index !in estado.items.indices) return
        val items = estado.items.toMutableList().apply { removeAt(index) }
        _state.value = estado.comItens(items)
    }

    // ---- trocar e acrescentar -------------------------------------------------------

    /** Abre a procura para substituir o item [index] pelo alimento que se escolher. */
    fun abrirTroca(index: Int) {
        if (index !in _state.value.items.indices) return
        _state.value = _state.value.copy(procura = ProcuraDeItem(alvo = index))
    }

    /** Abre a mesma procura, sem alvo: o que se escolher entra como item novo. */
    fun abrirAcrescento() {
        _state.value = _state.value.copy(procura = ProcuraDeItem(alvo = null))
    }

    fun fecharProcura() {
        procuraJob?.cancel()
        _state.value = _state.value.copy(procura = null)
    }

    fun procurar(texto: String) {
        val atual = _state.value.procura ?: return
        _state.value = _state.value.copy(procura = atual.copy(texto = texto, aProcurar = true))

        procuraJob?.cancel()
        procuraJob = viewModelScope.launch {
            val achados = procurarNoCatalogo(texto).take(MAX_RESULTADOS)
            // O alvo pode ter mudado enquanto a consulta corria — ou a procura ter sido
            // fechada. Escrever resultados numa procura que já não existe reabria-a.
            val agora = _state.value.procura ?: return@launch
            if (agora.texto != texto) return@launch
            _state.value = _state.value.copy(
                procura = agora.copy(resultados = achados, aProcurar = false),
            )
        }
    }

    /**
     * Fecha a troca ou o acrescento com o alimento escolhido.
     *
     * O item novo leva o `foodId`, e é isso que faz o registo deixar de ser um retrato
     * solto: passa a ligar ao catálogo, conta para os «mais registados» e ganha os
     * micronutrientes medidos em vez dos que o modelo estimou.
     */
    fun escolher(food: FoodEntity) {
        val estado = _state.value
        val procura = estado.procura ?: return
        val novo = itemDe(food)

        val items = estado.items.toMutableList()
        val alvo = procura.alvo
        if (alvo != null && alvo in items.indices) items[alvo] = novo else items.add(novo)

        _state.value = estado.comItens(items).copy(procura = null)
    }

    // ---- guardar como refeição ------------------------------------------------------

    /**
     * Guarda **os itens desta revisão** como modelo, e não a refeição do dia.
     *
     * A diferença não é detalhe: o método que lê o slot do diário apanharia o que já lá
     * estava registado de antes, e um modelo chamado «Almoço de terça» traria o pão do
     * pequeno-almoço lá dentro sem ninguém ver.
     */
    fun guardarComoRefeicao(nome: String, mealSlot: MealSlot) {
        val itens = _state.value.items
        if (itens.isEmpty() || nome.isBlank()) return
        viewModelScope.launch {
            val id = guardarModelo(nome, mealSlot, itens.map { modeloDe(it) })
            if (id != null) _state.value = _state.value.copy(guardadaComoRefeicao = nome.trim())
        }
    }

    fun confirm(mealSlot: MealSlot, epochDay: Long) {
        val estado = _state.value
        if (estado.items.isEmpty()) return
        viewModelScope.launch {
            repository.confirmFood(
                items = estado.items,
                mealSlot = mealSlot,
                epochDay = epochDay,
                origin = origin,
                photoBase64 = estado.photoBase64,
            )
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun reset() {
        job?.cancel()
        procuraJob?.cancel()
        _state.value = AiState(usage = _state.value.usage)
    }

    /**
     * Fechar a folha sem deitar fora o que se escreveu.
     *
     * **Cancelar guardava o texto e fechar apagava-o** — dois gestos parecidos com memórias
     * opostas, e a área 04 do estudo apanhou-o como defeito concreto. Um arrastão para baixo
     * por engano custava a frase toda, que é a parte que deu trabalho a escrever.
     *
     * A análise, essa, vai-se: os itens de uma foto que já não está à vista voltariam a
     * aparecer da próxima vez que a folha abrisse, e uma revisão velha é pior do que nenhuma.
     * O que sobrevive é o que a pessoa escreveu.
     */
    fun fecharGuardandoOTexto() {
        job?.cancel()
        procuraJob?.cancel()
        _state.value = AiState(usage = _state.value.usage, text = _state.value.text)
    }

    companion object {

        const val MAX_TEXT_CHARS = 2_000

        const val MIN_TEXT_CHARS = 3

        // Chega para «1234.5». Sem limite, um dedo preso no teclado dava um número que
        // rebentava a conta ao multiplicar os macros.
        const val MAX_GRAMS_CHARS = 6

        const val MAX_RESULTADOS = 20

        /** O que se assume quando o alimento não declara porção nenhuma. */
        const val GRAMAS_POR_OMISSAO = 100.0
    }
}

/**
 * Algarismos e, quando muito, um separador decimal — vírgula ou ponto, como se escreveu.
 *
 * Um segundo separador cai, e é de propósito: «37,5,2» não é um número, e deixá-lo entrar
 * fazia o campo mostrar um valor que a conta ignorava em silêncio.
 */
private fun apenasNumero(texto: String): String {
    val limpo = StringBuilder()
    var jaTemSeparador = false
    for (c in texto) {
        when {
            c.isDigit() -> limpo.append(c)
            (c == ',' || c == '.') && !jaTemSeparador -> {
                jaTemSeparador = true
                limpo.append(c)
            }
        }
    }
    return limpo.toString()
}

/**
 * Põe a lista de itens e os textos das gramas ao mesmo comprimento.
 *
 * É o único sítio onde [AiState.items] muda de tamanho. Fora daqui, uma remoção que
 * esquecesse os textos deixava o campo do item seguinte a mostrar as gramas do que saiu.
 */
private fun AiState.comItens(novos: List<AiFoodItem>): AiState =
    copy(items = novos, gramasTexto = novos.map { it.grams.roundToInt().toString() })

/**
 * Um alimento do catálogo visto como item da revisão.
 *
 * A porção é a do alimento quando ele declara uma — uma fatia, uma chávena —, e cem gramas
 * quando não declara. Cem gramas é a unidade da tabela, e é honesto: ninguém confunde
 * «100 g» com uma dose medida, e quem confundisse tinha o campo das gramas à frente.
 */
private fun itemDe(food: FoodEntity): AiFoodItem {
    val gramas = food.servingGrams?.takeIf { it > 0 } ?: AiViewModel.GRAMAS_POR_OMISSAO
    val f = gramas / 100.0
    return AiFoodItem(
        name = food.namePt,

        // O catálogo é uma medição e não uma estimativa do modelo, e é por isso que o item
        // trocado deixa de vir marcado para revisão: já foi revisto por quem o trocou.
        matchedSource = food.source.name,
        grams = gramas,
        kcal = (food.kcal * f).roundToInt(),
        protein = food.proteinG * f,
        carbs = food.carbsG * f,
        fat = food.fatG * f,

        // Por porção, como tudo o que vem da AI — o repositório é que os volta a pôr por
        // 100 g ao gravar. Mandá-los por 100 g aqui gravava valores dez vezes errados.
        micros = microsDeJson(food.microsJson).mapValues { (_, v) -> v * f }.takeIf { it.isNotEmpty() },
        confidence = 1.0,
        estimated = false,
        foodId = food.id,
    )
}

/**
 * Um item da revisão visto como item de modelo.
 *
 * Os micronutrientes voltam a ficar **por 100 g**, que é como a base os guarda em todo o
 * lado. Os da AI vêm por porção, e copiá-los como estão gravava valores escalados duas
 * vezes assim que alguém mudasse as gramas do modelo.
 */
private fun modeloDe(item: AiFoodItem, json: Json = Json): ItemDeModelo = ItemDeModelo(
    nome = item.name,
    gramas = item.grams,
    kcal = item.kcal,
    proteina = item.protein,
    hidratos = item.carbs,
    gordura = item.fat,
    microsPer100Json = item.micros
        ?.takeIf { it.isNotEmpty() && item.grams > 0 }
        ?.mapValues { (_, v) -> v * 100.0 / item.grams }
        ?.let { json.encodeToString(it) },
    foodId = item.foodId,
)
