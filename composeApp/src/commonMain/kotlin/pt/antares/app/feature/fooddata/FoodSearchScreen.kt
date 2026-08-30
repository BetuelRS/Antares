package pt.antares.app.feature.fooddata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import pt.antares.app.core.nutrition.FoodProvenance
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.model.mealSlotLabel
import pt.antares.app.feature.ai.AiFoodSheet
import pt.antares.app.feature.ai.AiMode
import pt.antares.app.generated.resources.ai_photo
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.util.rememberVoiceInput
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.feature.recipe.RecipeSummary
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import kotlin.math.roundToInt

/**
 * Para onde a pesquisa leva, tirando o alimento.
 *
 * As três viajam juntas porque são a mesma coisa — sair deste ecrã para outro — e separadas
 * faziam do separador uma função com mais parâmetros do que linhas úteis.
 */
private data class NavegacaoDaPesquisa(
    val abrirReceita: (String) -> Unit,
    val editarReceita: (String) -> Unit,
    val novaReceita: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodSearchScreen(
    onBack: () -> Unit,
    onFoodSelected: (String) -> Unit,
    // Leva o que estava escrito na caixa: quem procurou «pão da avó» e não encontrou já
    // escreveu o nome uma vez, e escrevê-lo outra é a app a fingir que não ouviu.
    onCreateCustom: (String) -> Unit,
    onScan: () -> Unit,
    pickMode: Boolean = false,
    onRecipeSelected: (String) -> Unit = {},
    onEditRecipe: (String) -> Unit = {},
    onNewRecipe: () -> Unit = {},

    aiSlot: MealSlot? = null,
    aiEpochDay: Long? = null,

    initialMode: String = "SEARCH",

    initialQuery: String = "",
    viewModel: FoodSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var aiMode by remember { mutableStateOf<AiMode?>(null) }

    // Uma vez na vida da instalação, e antes da primeira procura sair. Não tem botão de
    // fechar por fora: a escolha é o que dá valor ao interruptor, e um toque ao lado a
    // valer «continuar» seria consentimento por acidente.
    if (state.pedirAvisoDaOff) AvisoDaProcuraEmLinha(viewModel::responderAoAvisoDaOff)

    var handledInitial by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!handledInitial) {
            handledInitial = true

            // O ditado não entra na caixa de procura: vai para a folha da AI, e pô-lo aqui
            // deixava «dois ovos e uma torrada» a não encontrar nada por trás da folha.
            if (initialQuery.isNotBlank() && initialMode != "DESCRIBE") viewModel.setQuery(initialQuery)
            // O atalho de onde se veio: a app abre-se a ler um código ou a fotografar, e a
            // pesquisa é só a estação de passagem.
            aoAbrirCom(initialMode, onScan, viewModel::setTab) { aiMode = it }
        }
    }
    val suggestions by viewModel.suggestions.collectAsState()
    val openFood by viewModel.openFood.collectAsState()
    val escritoNoDiario by viewModel.escritoNoDiario.collectAsState()
    val preVisualizacao by viewModel.preVisualizacao.collectAsState()

    val multiSelect = !pickMode && aiSlot != null && aiEpochDay != null

    val marcados = state.selected.size

    LaunchedEffect(openFood) {
        openFood?.let {
            onFoodSelected(it)
            viewModel.consumeOpenFood()
        }
    }

    preVisualizacao?.let { pre ->
        FolhaDaRefeicaoGuardada(pre, viewModel, aiSlot, aiEpochDay)
    }

    LaunchedEffect(escritoNoDiario) {
        if (escritoNoDiario) {
            viewModel.consumirEscritoNoDiario()
            onBack()
        }
    }

    AntaresScaffold(
        // **O cabeçalho diz onde é que isto vai cair** — «Almoço · hoje». É a proposta 6 do
        // esboço 03: quem chega aqui por três caminhos diferentes precisa de saber a que
        // refeição e a que dia o registo se vai colar, e «Adicionar alimento» não diz.
        topBar = {
            AntaresTopBar(
                title = tituloDaPesquisa(aiSlot, aiEpochDay),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            // **O botão flutuante deixou de criar**, e é a proposta 5 do esboço 03: «criar
            // alimento sai do botão flutuante e passa a linha no fim, já com o nome
            // escrito». O botão mais visível do ecrã estava a oferecer a acção mais rara —
            // quem chega aqui quer registar, não criar.
            BotaoFlutuante(
                marcados = marcados,
                onRegistar = if (aiSlot != null && aiEpochDay != null) {
                    { viewModel.logSelected(aiSlot, aiEpochDay) }
                } else {
                    null
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ProcuraComAtalhos(
                texto = state.query,
                onTexto = viewModel::setQuery,
                onLerCodigo = onScan.takeIf { !pickMode },
                slot = aiSlot.takeIf { !pickMode },
                epochDay = aiEpochDay,
                modo = aiMode,
                onModo = { aiMode = it },
                ditadoInicial = initialQuery.takeIf { initialMode == "DESCRIBE" }.orEmpty(),
            )

            if (state.tab == SearchTab.TUDO) {
                ChipsDeSugestao(suggestions, onFoodSelected)
            }

            SeparadoresDaPesquisa(state.tab, viewModel::setTab)

            CorpoDoSeparador(
                state = state,
                viewModel = viewModel,
                multiSelect = multiSelect,
                pickMode = pickMode,
                onCreateCustom = onCreateCustom,
                onFoodSelected = onFoodSelected,
                navegacao = NavegacaoDaPesquisa(onRecipeSelected, onEditRecipe, onNewRecipe),
            )
        }
    }
}


/**
 * O que cada separador mostra.
 *
 * Sai do [FoodSearchScreen] porque essa passou dos 200 linhas com os três separadores, e
 * uma função que já não cabe num ecrã deixa de se poder ler de uma vez.
 */
/**
 * O aviso de que a procura sai do telemóvel, uma vez na vida da instalação.
 *
 * Não tem botão de fechar por fora: a escolha é o que dá valor ao interruptor, e um toque ao
 * lado a valer «continuar» seria consentimento por acidente.
 */
/**
 * Os atalhos para o que a pessoa já comeu, enquanto escreve.
 *
 * **Não desaparecem quando a pesquisa responde.** Desapareciam, e era no instante em que
 * passavam a poder ser comparados com o resto — o «arroz» que ela come sempre ao lado dos
 * quarenta arrozes do catálogo é precisamente a comparação que ela quer fazer.
 */
/**
 * A caixa de procura, com o leitor de códigos ao lado.
 *
 * O leitor é nulo no modo de escolha: a escolher um ingrediente para uma receita não se lê
 * um código de barras, e um botão que não faz nada é pior do que não haver botão.
 */
/**
 * Os dois atalhos para a IA — descrever e fotografar — e a folha que eles abrem.
 *
 * Só existem a registar num dia: descrever um prato só faz sentido se houver onde o pôr.
 */
/**
 * O botão flutuante, que diz duas coisas diferentes.
 *
 * Com alimentos marcados, regista-os todos; sem eles, cria um alimento novo. Nunca os dois:
 * quem marcou cinco linhas está a registar, não a criar.
 */
/**
 * O que fazer com o modo por que este ecrã foi aberto.
 *
 * Vem do atalho da aplicação ou de um botão noutro ecrã, e é uma cadeia de texto porque
 * atravessa a navegação — que só sabe transportar tipos simples.
 */
private fun aoAbrirCom(
    modo: String,
    onLerCodigo: () -> Unit,
    onSeparador: (SearchTab) -> Unit,
    onIa: (AiMode) -> Unit,
) {
    when (modo) {
        "SCAN" -> onLerCodigo()
        "PHOTO" -> onIa(AiMode.PHOTO)
        "DESCRIBE" -> onIa(AiMode.TEXT)
        // Vem do menu de uma refeição do diário, que é onde a vontade de repetir uma
        // refeição guardada nasce — e não a três toques de distância, dentro de «adicionar
        // comida».
        "MEALS" -> onSeparador(SearchTab.TUDO)
        else -> Unit
    }
}

/**
 * «Almoço · hoje», ou «Adicionar alimento» quando não se sabe onde é que aquilo vai cair.
 *
 * O dia só se escreve quando é hoje. Um «29 de agosto» ao lado da refeição é ruído no caso
 * comum — e o caso comum é registar no próprio dia.
 */
@Composable
private fun tituloDaPesquisa(slot: MealSlot?, epochDay: Long?): String {
    if (slot == null) return stringResource(Res.string.search_title)
    val refeicao = mealSlotLabel(slot)
    return if (epochDay == pt.antares.app.core.util.todayEpochDay()) {
        "$refeicao · " + stringResource(Res.string.diary_today).lowercase()
    } else {
        refeicao
    }
}

@Composable
private fun BotaoFlutuante(
    marcados: Int,
    onRegistar: (() -> Unit)?,
) {
    if (marcados > 0 && onRegistar != null) {
        ExtendedFloatingActionButton(
            onClick = onRegistar,
            // Decorativo: o botão traz o texto ao lado do ícone.
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(Res.string.search_log_selected, marcados)) },
        )
        return
    }
}

/**
 * Os tres separadores do esboco 03: **Tudo · Favoritos · Meus**.
 *
 * As refeicoes guardadas nao tem separador — sao a primeira seccao do «Tudo», que e onde o
 * esboco as poe. Ja tiveram um, e sai-lo custou a seccao de abertura: fechei a porta errada
 * das duas que a area 03 dizia serem uma a mais.
 */
@Composable
private fun SeparadoresDaPesquisa(activo: SearchTab, onSeparador: (SearchTab) -> Unit) {
    val tabs = listOf(
        SearchTab.TUDO to Res.string.search_tab_search,
        SearchTab.FAVORITOS to Res.string.search_tab_favorites,
        SearchTab.MEUS to Res.string.search_tab_mine,
    )

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == activo }.coerceAtLeast(0),
        edgePadding = Spacing.sm,
    ) {
        tabs.forEach { (tab, label) ->
            Tab(
                selected = activo == tab,
                onClick = { onSeparador(tab) },
                text = {
                    Text(
                        stringResource(label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

/**
 * O campo de procura com os tres atalhos dentro, e a folha que eles abrem.
 *
 * Vive numa peca propria porque a decisao — ha para onde mandar o que a AI devolve? — e a
 * mesma para os tres, e espalhada pelo ecra fazia dele um no de condicoes. Sem refeicao e
 * sem dia nao ha destino: e quem abriu a procura para escolher um ingrediente, e ai o
 * microfone e a camara nao levam a lado nenhum.
 */
@Composable
private fun ProcuraComAtalhos(
    texto: String,
    onTexto: (String) -> Unit,
    onLerCodigo: (() -> Unit)?,
    slot: MealSlot?,
    epochDay: Long?,
    modo: AiMode?,
    onModo: (AiMode?) -> Unit,
    ditadoInicial: String,
) {
    // O que o microfone ouviu, a espera da folha. Nao entra na caixa de procura pela mesma
    // razao da 2.17.0: uma frase de refeicao entregue a uma pesquisa de catalogo nao
    // encontra nada, e ve-la escrita no campo por tras da folha diria o contrario.
    var ditado by remember { mutableStateOf(ditadoInicial) }
    val comDestino = slot != null && epochDay != null

    CampoDeProcura(
        texto = texto,
        onTexto = onTexto,
        onLerCodigo = onLerCodigo,
        onIa = if (comDestino) {
            { escolhido, ouvido -> ditado = ouvido; onModo(escolhido) }
        } else {
            null
        },
    )

    if (slot != null && epochDay != null && modo != null) {
        AiFoodSheet(
            mode = modo,
            mealSlot = slot,
            epochDay = epochDay,
            initialText = ditado,
            onDismiss = { ditado = ""; onModo(null) },
        )
    }
}

/**
 * O campo de procura, com as três outras maneiras de dizer o que se comeu ao lado.
 *
 * **O rótulo diz «procurar ou descrever»** porque o campo passou a servir as duas coisas: o
 * que se escreve procura no catálogo, o que se dita vai para o interpretador. Chamar-lhe
 * «pesquisar alimento» com um microfone ao lado era prometer uma pesquisa a quem vai dizer
 * «dois ovos e uma torrada».
 *
 * A voz não vai para a pesquisa — é a correção que a 2.17.0 fez na barra rápida, e este
 * campo nasce já do lado certo. Os três ícones só existem a registar num dia: sem saber onde
 * pôr o resultado, descrever um prato não leva a lado nenhum.
 */
@Composable
private fun CampoDeProcura(
    texto: String,
    onTexto: (String) -> Unit,
    onLerCodigo: (() -> Unit)?,

    // Nulo quando nao ha para onde mandar o resultado. Um microfone que abre uma folha sem
    // destino e um botao que nao faz nada, e isso e pior do que nao haver botao.
    onIa: ((AiMode, String) -> Unit)?,
) {
    val voz = rememberVoiceInput { ouvido -> onIa?.invoke(AiMode.TEXT, ouvido) }
    val pedido = stringResource(Res.string.quick_log_voice_prompt)

    OutlinedTextField(
        value = texto,
        onValueChange = onTexto,
        label = { Text(stringResource(Res.string.search_hint)) },
        // Decorativo: a lupa repete o rótulo do campo de pesquisa.
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            Row {
                if (onIa != null && voz.available) {
                    IconButton(onClick = { voz.start(pedido) }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = stringResource(Res.string.quick_log_voice),
                        )
                    }
                }
                if (onIa != null) {
                    IconButton(onClick = { onIa(AiMode.PHOTO, "") }) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = stringResource(Res.string.ai_photo),
                        )
                    }
                }
                if (onLerCodigo != null) {
                    IconButton(onClick = onLerCodigo) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(Res.string.search_scan),
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        singleLine = true,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsDeSugestao(sugestoes: List<FoodEntity>, onEscolher: (String) -> Unit) {
    if (sugestoes.isEmpty()) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (food in sugestoes) {
            AssistChip(
                onClick = { onEscolher(food.id) },
                label = {
                    Text(
                        food.namePt.ifBlank { food.nameEn },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun AvisoDaProcuraEmLinha(onResponder: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(Res.string.search_off_notice_title)) },
        text = { Text(stringResource(Res.string.search_off_notice_body)) },
        confirmButton = {
            SecondaryButton(
                text = stringResource(Res.string.search_off_notice_yes),
                onClick = { onResponder(true) },
            )
        },
        dismissButton = {
            SecondaryButton(
                text = stringResource(Res.string.search_off_notice_no),
                onClick = { onResponder(false) },
            )
        },
    )
}

@Composable
private fun CorpoDoSeparador(
    state: FoodSearchState,
    viewModel: FoodSearchViewModel,
    multiSelect: Boolean,
    pickMode: Boolean,
    onCreateCustom: (String) -> Unit,
    onFoodSelected: (String) -> Unit,
    navegacao: NavegacaoDaPesquisa,
) {
    val onRecipeSelected = navegacao.abrirReceita
    val onEditRecipe = navegacao.editarReceita
    val onNewRecipe = navegacao.novaReceita
    // As seis listas são deste bloco e de mais nenhum. Recolhê-las aqui tira-as do ecrã, que
    // as recebia só para as voltar a passar.
    val mostLogged by viewModel.mostLogged.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val myFoods by viewModel.myFoods.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val habituais by viewModel.porcoesHabituais.collectAsState()

        when (state.tab) {
            SearchTab.TUDO -> if (state.query.length < 2) {

                // Com a caixa vazia, as três secções do esboço 03, por esta ordem: as tuas
                // refeições, o que comes mais, os recentes. **As refeições vêm primeiro** —
                // são a resposta mais rápida a «o que é que eu vou comer», e o esboço
                // desenha-as no topo. Já estiveram aqui e saíram por engano meu, ao fechar
                // a porta errada de duas que o estudo dizia serem uma a mais.
                YourStuff(
                    favoritos = state.favoritos,
                    foods = mostLogged,
                    recentes = recents,
                    selectable = multiSelect,
                    selectedIds = state.selected,
                    onToggle = viewModel::toggleSelect,
                    onFood = { onFoodSelected(it.id) },
                    habituais = habituais,
                    refeicoes = if (pickMode) emptyList() else juntarRefeicoes(templates, recipes),
                    onRefeicao = { refeicao ->
                        when (refeicao) {
                            is RefeicaoGuardada.DoDiario -> viewModel.verModelo(refeicao.resumo)
                            is RefeicaoGuardada.DeIngredientes ->
                                onRecipeSelected(refeicao.resumo.recipe.id)
                        }
                    },
                )
            } else {
                SearchResults(
                    state = state,
                    selectable = multiSelect,
                    onToggle = viewModel::toggleSelect,
                    onLocal = { onFoodSelected(it.id) },
                    onOnline = viewModel::selectOnline,
                    onEstados = viewModel::alternarEstados,
                    onCriar = { onCreateCustom(state.query.trim()) },
                )
            }

            // Os favoritos passam a separador próprio, como o esboço os desenha. Eram uma
            // secção do «Tudo», e uma secção que se percorre depois de duas outras não é o
            // sítio de quem marcou uma coisa precisamente para lhe voltar depressa.
            SearchTab.FAVORITOS -> ListaDeAlimentos(
                lista = favorites,
                state = state,
                multiSelect = multiSelect,
                viewModel = viewModel,
                onFoodSelected = onFoodSelected,
            )

            SearchTab.MEUS -> ListaDeAlimentos(
                lista = myFoods,
                state = state,
                multiSelect = multiSelect,
                viewModel = viewModel,
                onFoodSelected = onFoodSelected,
            )
        }
}

/** Uma lista de alimentos e nada mais. Os dois separadores da direita são a mesma coisa. */
@Composable
private fun ListaDeAlimentos(
    lista: List<FoodEntity>,
    state: FoodSearchState,
    multiSelect: Boolean,
    viewModel: FoodSearchViewModel,
    onFoodSelected: (String) -> Unit,
) {
    if (lista.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.search_empty_title),
            subtitle = stringResource(Res.string.search_empty_subtitle),
        )
        return
    }
    ListaAdaptavel(
        modifier = Modifier.fillMaxSize(),
        contentPadding = SEM_MARGEM,
        espaco = 0.dp,
    ) {
        items(lista, key = { it.id }) { food ->
            FoodRow(
                food = food,
                favorito = food.id in state.favoritos,
                selectable = multiSelect,
                selected = food.id in state.selected,
                onToggle = { viewModel.toggleSelect(food.id) },
                onClick = { onFoodSelected(food.id) },
            )
        }
    }
}
@Composable
private fun YourStuff(
    foods: List<FoodEntity>,
    favoritos: Set<String>,
    selectable: Boolean,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onFood: (FoodEntity) -> Unit,
    habituais: Map<String, Double> = emptyMap(),

    // As refeições já montadas, e vêm **primeiro**. É onde o esboço 03 as põe, e a razão
    // está no título dele: abrir no que se come. Quem já guardou o almoço de sempre não
    // quer procurá-lo — quer vê-lo.
    refeicoes: List<RefeicaoGuardada> = emptyList(),
    onRefeicao: (RefeicaoGuardada) -> Unit = {},

    // Os que eram separadores próprios. Chegam aqui como listas porque a pergunta que
    // respondem é a mesma, e separá-las obrigava a escolher entre elas antes de escrever.
    recentes: List<FoodEntity> = emptyList(),
) {
    val vazio = listOf(foods, recentes).all { it.isEmpty() } && refeicoes.isEmpty()
    if (vazio) {
        // Descreve o estado da lista, e não o campo de procura. A frase antiga — «escreve
        // pelo menos 2 letras» — aparecia a quem ainda não tinha registado nada, mesmo com
        // uma letra já escrita: dizia o que fazer com o campo e nada sobre o que faltava
        // aqui. A área 03 do estudo apanhou-a como estado vazio enganador.
        EmptyState(title = stringResource(Res.string.search_sem_historico))
        return
    }
    ListaAdaptavel(modifier = Modifier.fillMaxSize(), contentPadding = SEM_MARGEM, espaco = 0.dp) {
        if (refeicoes.isNotEmpty()) {
            linhaInteira {
                SectionHeader(
                    title = stringResource(Res.string.search_your_meals),
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
            items(refeicoes, key = { it.chave }) { refeicao ->
                LinhaDaRefeicao(refeicao) { onRefeicao(refeicao) }
            }
        }

        if (foods.isNotEmpty()) {
            linhaInteira {
                SectionHeader(
                    title = stringResource(Res.string.search_your_foods),
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
            items(foods, key = { "top-${it.id}" }) { food ->
                FoodRow(
                    food = food,
                    favorito = food.id in favoritos,
                    selectable = selectable,
                    selected = food.id in selectedIds,
                    onToggle = { onToggle(food.id) },
                    onClick = { onFood(food) },
                    habitualG = habituais[food.id],
                )
            }
        }

        secaoDeAlimentos(
            titulo = Res.string.search_secao_recentes,
            // Sem repetir o que já está nos mais registados: a mesma linha duas vezes no
            // mesmo ecrã lê-se como um defeito.
            alimentos = recentes.filterNot { r -> foods.any { it.id == r.id } },
            prefixo = "rec",
            favoritos = favoritos,
            selectable = selectable,
            selectedIds = selectedIds,
            onToggle = onToggle,
            onFood = onFood,
            habituais = habituais,
        )
    }
}

/**
 * Uma secção de alimentos dentro da lista do «Procurar» vazio.
 *
 * Existe porque as três secções são a mesma coisa com outro título, e três cópias do mesmo
 * bloco divergiriam à primeira alteração.
 */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.secaoDeAlimentos(
    titulo: org.jetbrains.compose.resources.StringResource,
    alimentos: List<FoodEntity>,
    prefixo: String,
    favoritos: Set<String>,
    selectable: Boolean,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onFood: (FoodEntity) -> Unit,
    habituais: Map<String, Double> = emptyMap(),
) {
    if (alimentos.isEmpty()) return
    linhaInteira {
        SectionHeader(title = stringResource(titulo), modifier = Modifier.padding(Spacing.sm))
    }
    items(alimentos, key = { "$prefixo-${it.id}" }) { food ->
        FoodRow(
            food = food,
            favorito = food.id in favoritos,
            selectable = selectable,
            selected = food.id in selectedIds,
            onToggle = { onToggle(food.id) },
            onClick = { onFood(food) },
            habitualG = habituais[food.id],
        )
    }
}

@Composable
private fun SearchResults(
    state: FoodSearchState,
    selectable: Boolean,
    onToggle: (String) -> Unit,
    onLocal: (FoodEntity) -> Unit,
    onOnline: (FoodEntity) -> Unit,
    onEstados: (String) -> Unit,

    // Criar o alimento que não se encontrou. Vive no fim da lista e não num botão
    // flutuante: é a proposta 5 do esboço 03, e leva o nome já escrito.
    onCriar: () -> Unit,
) {
    val nothing = state.results.isEmpty() && state.onlineResults.isEmpty() &&
        !state.searching && !state.searchingOnline
    if (nothing) {
        EmptyState(
            title = stringResource(Res.string.search_empty_title),
            subtitle = stringResource(Res.string.search_empty_subtitle),
        )
        return
    }

    ListaAdaptavel(modifier = Modifier.fillMaxSize(), contentPadding = SEM_MARGEM, espaco = 0.dp) {
        if (state.results.isNotEmpty()) {
            linhaInteira {
                SectionHeader(
                    title = stringResource(Res.string.search_section_local),
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
            for (grupo in state.grupos) {
                val aberto = grupo.principal.id in state.estadosAbertos

                item(key = "local-${grupo.principal.id}") {
                    FoodRow(
                        food = grupo.principal,
                        favorito = grupo.principal.id in state.favoritos,
                        selectable = selectable,
                        selected = grupo.principal.id in state.selected,
                        onToggle = { onToggle(grupo.principal.id) },
                        onClick = { onLocal(grupo.principal) },
                        // «+ 2 estados», e o que faz abri-los. Um alimento sozinho nao
                        // mostra nada — que e a esmagadora maioria do catalogo.
                        outrosEstados = grupo.quantosOutros,
                        estadosAbertos = aberto,
                        onEstados = { onEstados(grupo.principal.id) },
                    )
                }

                if (aberto) {
                    items(grupo.outros, key = { "estado-${it.id}" }) { outro ->
                        FoodRow(
                            food = outro,
                            favorito = outro.id in state.favoritos,
                            selectable = selectable,
                            selected = outro.id in state.selected,
                            onToggle = { onToggle(outro.id) },
                            onClick = { onLocal(outro) },
                            recuado = true,
                        )
                    }
                }
            }
        }
        if (state.onlineResults.isNotEmpty() || state.searchingOnline) {
            linhaInteira {
                SectionHeader(
                    title = stringResource(Res.string.search_section_online),
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
        }
        items(state.onlineResults, key = { "off-${it.id}" }) { food ->
            FoodRow(food = food, online = true, onClick = { onOnline(food) })
        }
        if (state.searchingOnline) {
            linhaInteira { LoadingState() }
        }

        // Dito e não escondido: sem esta linha, uma procura que não sai do telemóvel
        // lê-se como um catálogo que não tem o produto — e a pessoa passa a duvidar do
        // catálogo em vez de se lembrar do interruptor que ela própria desligou.
        if (state.pesquisaDesligada) {
            linhaInteira {
                Text(
                    stringResource(Res.string.search_online_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
        }

        // «Não encontraste? Criar «arroz basmati»» — no fim da lista, com o nome já
        // escrito. É a proposta 5 do esboço 03: quem chega ao fim dos resultados sem
        // encontrar é quem quer criar, e é aí que a oferta chega na hora certa.
        linhaInteira {
            SecondaryButton(
                text = stringResource(Res.string.search_criar_este, state.query.trim()),
                onClick = onCriar,
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            )
        }
    }
}


/**
 * Uma refeição já montada, como o esboço 05 desenha a linha: nome, o que lá está dentro, e
 * uma seta a dizer que abre.
 */
@Composable
private fun LinhaDaRefeicao(refeicao: RefeicaoGuardada, onClick: () -> Unit) {
    LinhaDaLista(
        // Uma receita pode não ter nome: a linha dela nasce no instante em que se abre a
        // folha de ingredientes ou a de passos, porque eles precisam de um pai onde se
        // agarrar — e quem recua sem escrever nada deixa-a lá.
        titulo = refeicao.nome.ifBlank { stringResource(Res.string.recipe_sem_nome) },
        subtitulo = subtituloDaRefeicao(refeicao),
        onClick = onClick,
        aoLado = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                // Decorativo: a linha inteira é o alvo, e a seta só diz que ela abre.
                contentDescription = null,
            )
        },
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
    )
}



@Composable
private fun FoodRow(
    food: FoodEntity,
    favorito: Boolean = false,
    online: Boolean = false,
    selectable: Boolean = false,
    selected: Boolean = false,
    onToggle: () -> Unit = {},
    // Quantos outros estados do mesmo alimento estao por baixo desta linha. Zero esconde
    // tudo o que se segue, e e o caso da esmagadora maioria do catalogo.
    outrosEstados: Int = 0,
    estadosAbertos: Boolean = false,
    onEstados: () -> Unit = {},
    // Uma linha que e o estado de outra recua, para se ler como pertencendo a de cima.
    recuado: Boolean = false,

    // O que a pessoa costuma registar deste alimento. Nulo em quase todos: são precisos
    // três registos para o [UsualPortion] chamar hábito a alguma coisa.
    habitualG: Double? = null,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (recuado) Spacing.xl else Spacing.lg,
                end = Spacing.lg,
                top = Spacing.xs,
                bottom = Spacing.xs,
            ),
    ) {

        val curatedPt = FoodProvenance.of(food.source, food.id) == FoodProvenance.CURATED
        ListItem(
            // A fotografia do produto, só onde ela existe — o que na prática quer dizer os
            // produtos de embalagem que vêm da Open Food Facts. O catálogo que a app traz não
            // tem fotografias de nada, e por isso a esmagadora maioria das linhas não muda.
            leadingContent = food.imagemUrl?.let { url ->
                {
                    AsyncImage(
                        model = url,
                        // Decorativo: o nome do produto está na linha ao lado, e um leitor
                        // de ecrã a dizer «fotografia de X» a seguir a «X» repete-se.
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(MINIATURA)
                            .clip(RoundedCornerShape(Spacing.xs)),
                    )
                }
            },
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(food.namePt.ifBlank { food.nameEn }, maxLines = 2, modifier = Modifier.weight(1f, fill = false))

                    if (curatedPt) SourceBadge(stringResource(Res.string.search_badge_pt))
                }
            },
            supportingContent = {
                // A porção ao lado das kcal, quando o alimento tem uma. Vale para 26 % do
                // catálogo — não para a maioria, como o plano dizia — e nos outros a linha
                // fica como estava, a dizer 100 g. Uma linha que às vezes tem duas coisas e
                // às vezes uma é pior do que uma que tem sempre a mesma forma.
                // **O habitual ganha à porção da fonte**, e é a proposta 3 do esboço 03:
                // «180 g habituais» é o número com que esta pessoa regista este alimento,
                // e a porção da tabela é o número com que o mundo o regista. Onde os dois
                // existem, mostra-se o dela.
                val porcao = habitualG?.let { gramas ->
                    " · " + stringResource(Res.string.food_habitual, gramas.roundToInt())
                } ?: food.servingGrams?.let { gramas ->
                    val nome = food.servingName ?: stringResource(Res.string.food_serving)
                    " · $nome ${gramas.roundToInt()} ${stringResource(Res.string.common_grams_short)}"
                }.orEmpty()
                Text(
                    "${food.kcal} ${stringResource(Res.string.common_kcal)} / 100 " +
                        "${stringResource(Res.string.common_grams_short)}$porcao",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // O contador vem antes dos icones: e o unico sinal de que ha mais
                    // comida escondida, e ao lado da caixa de selecao passava por parte
                    // dela.
                    if (outrosEstados > 0) {
                        TextButton(onClick = onEstados) {
                            Text(
                                pluralStringResource(
                                    Res.plurals.search_outros_estados,
                                    outrosEstados,
                                    outrosEstados,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            // Decorativo: a seta acompanha o «+ 2 estados» que está ao lado,
                            // e o leitor de ecrã já lê o botão inteiro por esse texto.
                            Icon(
                                if (estadosAbertos) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                            )
                        }
                    }
                    when {
                        online -> Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = stringResource(Res.string.search_section_online),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        favorito -> Icon(
                            Icons.Default.Star,
                            // A estrela é a única coisa que diz que este alimento é
                            // favorito: nada no texto da linha o repete.
                            contentDescription = stringResource(Res.string.food_favorite),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    if (selectable) {

                        val nome = food.namePt.ifBlank { food.nameEn }
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onToggle() },
                            modifier = Modifier.semantics { contentDescription = nome },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun SourceBadge(label: String) {
    // **Terciária e não primária.** O selo diz de onde vem o alimento, e nesta app a cor
    // primária quer dizer «isto faz alguma coisa» — o anel, o botão de registar, o que está
    // por decidir. Com os contentores dos acentos a ganharem véu próprio, um selo em
    // primária passou a ler-se como um botão, e vê-se numa captura: o «PT» ficou vermelho ao
    // lado do nome do alimento. O verde-azulado é o que a app já usa para informação.
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 1.dp),
        )
    }
}

/**
 * As listas da pesquisa trazem a margem em cada linha, e não no contentor. Repeti-la aqui
 * daria margem a dobrar assim que a lista passou a ganhar colunas.
 */
private val SEM_MARGEM = PaddingValues(0.dp)

// Grande o suficiente para se distinguir um rótulo, pequena o suficiente para a linha não
// mudar de altura por causa dela.
private val MINIATURA = 44.dp

