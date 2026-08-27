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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import pt.antares.app.generated.resources.ai_describe
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
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.rememberApagarComDesfazer
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
            aoAbrirCom(initialMode, onScan) { aiMode = it }
        }
    }
    val suggestions by viewModel.suggestions.collectAsState()
    val openFood by viewModel.openFood.collectAsState()
    val templateApplied by viewModel.templateApplied.collectAsState()

    val multiSelect = !pickMode && aiSlot != null && aiEpochDay != null

    val marcados = state.selected.size
    val apagar = rememberApagarComDesfazer()

    LaunchedEffect(openFood) {
        openFood?.let {
            onFoodSelected(it)
            viewModel.consumeOpenFood()
        }
    }

    LaunchedEffect(templateApplied) {
        if (templateApplied) {
            viewModel.consumeTemplateApplied()
            onBack()
        }
    }

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.search_title), onBack = onBack) },
        floatingActionButton = {
            BotaoFlutuante(
                marcados = marcados,
                podeCriar = !pickMode,
                onRegistar = if (aiSlot != null && aiEpochDay != null) {
                    { viewModel.logSelected(aiSlot, aiEpochDay) }
                } else {
                    null
                },
                onCriar = { onCreateCustom(state.query.trim()) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CampoDeProcura(
                texto = state.query,
                onTexto = viewModel::setQuery,
                onLerCodigo = onScan.takeIf { !pickMode },
            )

            if (!pickMode && aiSlot != null && aiEpochDay != null) {
                AtalhosDaIa(
                    modo = aiMode,
                    slot = aiSlot,
                    epochDay = aiEpochDay,
                    textoInicial = initialQuery.takeIf { initialMode == "DESCRIBE" }.orEmpty(),
                    onModo = { aiMode = it },
                )
            }

            if (state.tab == SearchTab.SEARCH) {
                ChipsDeSugestao(suggestions, onFoodSelected)
            }

            // Três, e não seis. O «Refeições» só existe fora do modo de escolha: escolher
            // uma receita para dentro de outra receita não é uma coisa que se faça.
            val tabs = buildList {
                add(SearchTab.SEARCH to Res.string.search_tab_search)
                add(SearchTab.MINE to Res.string.search_tab_mine)
                if (!pickMode) add(SearchTab.REFEICOES to Res.string.search_tab_refeicoes)
            }

            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == state.tab }.coerceAtLeast(0),
                edgePadding = Spacing.sm,
            ) {
                tabs.forEach { (tab, label) ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
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

            CorpoDoSeparador(
                state = state,
                viewModel = viewModel,
                multiSelect = multiSelect,
                aiSlot = aiSlot,
                aiEpochDay = aiEpochDay,
                apagar = apagar,
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
private fun aoAbrirCom(modo: String, onLerCodigo: () -> Unit, onIa: (AiMode) -> Unit) {
    when (modo) {
        "SCAN" -> onLerCodigo()
        "PHOTO" -> onIa(AiMode.PHOTO)
        "DESCRIBE" -> onIa(AiMode.TEXT)
        else -> Unit
    }
}

@Composable
private fun BotaoFlutuante(
    marcados: Int,
    podeCriar: Boolean,
    onRegistar: (() -> Unit)?,
    onCriar: () -> Unit,
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
    if (podeCriar) {
        ExtendedFloatingActionButton(
            onClick = onCriar,
            // Decorativo: o botão traz o texto ao lado do ícone.
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(Res.string.food_create_cta)) },
        )
    }
}

@Composable
private fun AtalhosDaIa(
    modo: AiMode?,
    slot: MealSlot,
    epochDay: Long,

    // O que se ditou, quando se chegou aqui pelo microfone. Vazio em todos os outros casos.
    textoInicial: String,
    onModo: (AiMode?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SecondaryButton(
            text = "✨ " + stringResource(Res.string.ai_describe),
            onClick = { onModo(AiMode.TEXT) },
            modifier = Modifier.weight(1f),
        )
        SecondaryButton(
            text = "📷 " + stringResource(Res.string.ai_photo),
            onClick = { onModo(AiMode.PHOTO) },
            modifier = Modifier.weight(1f),
        )
    }

    modo?.let { m ->
        AiFoodSheet(
            mode = m,
            mealSlot = slot,
            epochDay = epochDay,
            initialText = textoInicial,
            onDismiss = { onModo(null) },
        )
    }
}

@Composable
private fun CampoDeProcura(texto: String, onTexto: (String) -> Unit, onLerCodigo: (() -> Unit)?) {
    OutlinedTextField(
        value = texto,
        onValueChange = onTexto,
        label = { Text(stringResource(Res.string.search_hint)) },
        // Decorativo: a lupa repete o rótulo do campo de pesquisa.
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = onLerCodigo?.let { ler ->
            {
                IconButton(onClick = ler) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(Res.string.search_scan),
                    )
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
    aiSlot: MealSlot?,
    aiEpochDay: Long?,
    apagar: (() -> Unit, () -> Unit) -> Unit,
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

        when (state.tab) {
            SearchTab.SEARCH -> if (state.query.length < 2) {

                // Com a caixa vazia, as três respostas a «o que é que eu já comi?» —
                // que era o que quatro separadores diziam, cada um no seu sítio.
                YourStuff(
                    favoritos = state.favoritos,
                    foods = mostLogged,
                    recentes = recents,
                    marcados = favorites,
                    selectable = multiSelect,
                    selectedIds = state.selected,
                    onToggle = viewModel::toggleSelect,
                    templates = if (aiSlot != null && aiEpochDay != null) templates.take(5) else emptyList(),
                    onFood = { onFoodSelected(it.id) },
                    onTemplate = { id ->
                        if (aiSlot != null && aiEpochDay != null) {
                            viewModel.applyTemplate(id, aiSlot, aiEpochDay)
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
                )
            }
            // Receitas e modelos no mesmo separador, um a seguir ao outro. São a mesma
            // pergunta — «o que é que eu já montei?» — e a 2.18.0 junta-os também por
            // dentro.
            SearchTab.REFEICOES -> RefeicoesTab(
                recipes = recipes,
                templates = if (aiSlot != null && aiEpochDay != null) templates else emptyList(),
                onNew = onNewRecipe,
                onSelect = onRecipeSelected,
                onEdit = onEditRecipe,
                onApply = { id ->
                    if (aiSlot != null && aiEpochDay != null) {
                        viewModel.applyTemplate(id, aiSlot, aiEpochDay)
                    }
                },
                onDelete = { id ->
                    apagar({ viewModel.deleteTemplate(id) }, { viewModel.restoreTemplate(id) })
                },
            )
            SearchTab.MINE -> {
                val list = myFoods
                if (list.isEmpty()) {
                    EmptyState(
                        title = stringResource(Res.string.search_empty_title),
                        subtitle = stringResource(Res.string.search_empty_subtitle),
                    )
                } else {
                    ListaAdaptavel(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = SEM_MARGEM,
                        espaco = 0.dp,
                    ) {
                        items(list, key = { it.id }) { food ->
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
    templates: List<pt.antares.app.core.database.entities.MealTemplateEntity>,
    onFood: (FoodEntity) -> Unit,
    onTemplate: (String) -> Unit,
    // Os que eram separadores próprios. Chegam aqui como listas porque a pergunta que
    // respondem é a mesma, e separá-las obrigava a escolher entre elas antes de escrever.
    recentes: List<FoodEntity> = emptyList(),
    marcados: List<FoodEntity> = emptyList(),
) {
    val vazio = listOf(foods, templates, recentes, marcados).all { it.isEmpty() }
    if (vazio) {
        EmptyState(title = stringResource(Res.string.search_min_chars))
        return
    }
    ListaAdaptavel(modifier = Modifier.fillMaxSize(), contentPadding = SEM_MARGEM, espaco = 0.dp) {
        if (templates.isNotEmpty()) {
            linhaInteira {
                SectionHeader(
                    title = stringResource(Res.string.search_your_meals),
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
            items(templates, key = { "tpl-${it.id}" }) { template ->
                LinhaDaLista(
                    titulo = template.name,
                    subtitulo = mealSlotLabel(template.slot),
                    onClick = { onTemplate(template.id) },
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
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
                )
            }
        }

        // Os recentes e os favoritos, que eram dois separadores. A ordem é a de quem procura
        // sem saber o que quer: o que come sempre, o que comeu há pouco, o que marcou.
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
        )
        secaoDeAlimentos(
            titulo = Res.string.search_secao_favoritos,
            alimentos = marcados.filterNot { m ->
                foods.any { it.id == m.id } || recentes.any { it.id == m.id }
            },
            prefixo = "fav",
            favoritos = favoritos,
            selectable = selectable,
            selectedIds = selectedIds,
            onToggle = onToggle,
            onFood = onFood,
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
    }
}

/**
 * Receitas e modelos, um a seguir ao outro.
 *
 * Eram dois separadores, e a pergunta é a mesma: «o que é que eu já montei?». A diferença
 * entre uma receita e um modelo é como se construiu — a receita soma ingredientes, o modelo
 * guarda um dia — e essa distinção não vale uma escolha antes de olhar para as duas listas.
 *
 * Os modelos vêm primeiro por serem menos: uma lista curta em cima de uma comprida encontra-se
 * de relance, ao contrário do inverso.
 */
@Composable
private fun RefeicoesTab(
    recipes: List<RecipeSummary>,
    templates: List<pt.antares.app.core.database.entities.MealTemplateEntity>,
    onNew: () -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (String) -> Unit,
    onApply: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    ListaAdaptavel(modifier = Modifier.fillMaxSize(), contentPadding = SEM_MARGEM, espaco = 0.dp) {
        linhaInteira {
            SecondaryButton(
                text = stringResource(Res.string.recipe_new),
                onClick = onNew,
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            )
        }

        if (templates.isNotEmpty()) {
            linhaInteira {
                SectionHeader(
                    title = stringResource(Res.string.search_your_meals),
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
            items(templates, key = { "tpl-${it.id}" }) { template ->
                LinhaDaLista(
                    titulo = template.name,
                    subtitulo = mealSlotLabel(template.slot),
                    onClick = { onApply(template.id) },
                    aoLado = {
                        IconButton(onClick = { onDelete(template.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.templates_delete),
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
        }

        if (recipes.isNotEmpty()) {
            linhaInteira {
                SectionHeader(
                    title = stringResource(Res.string.search_tab_recipes),
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
            items(recipes, key = { "rec-${it.recipe.id}" }) { summary ->
                LinhaDaLista(
                    titulo = summary.recipe.name,
                    subtitulo = "${summary.nutrition.kcalPer100} ${stringResource(Res.string.common_kcal)} / 100 " +
                        "${stringResource(Res.string.common_grams_short)} · " +
                        "${summary.ingredientCount} ${stringResource(Res.string.recipe_ingredients)}",
                    onClick = { onSelect(summary.recipe.id) },
                    aoLado = {
                        IconButton(onClick = { onEdit(summary.recipe.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.recipe_edit))
                        }
                    },
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
        }

        if (recipes.isEmpty() && templates.isEmpty()) {
            linhaInteira { EmptyState(title = stringResource(Res.string.templates_empty_title)) }
        }
    }
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
                val porcao = food.servingGrams?.let { gramas ->
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
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
