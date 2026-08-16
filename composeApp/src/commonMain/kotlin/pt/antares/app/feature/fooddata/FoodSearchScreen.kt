package pt.antares.app.feature.fooddata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.database.entities.FoodEntity
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.feature.recipe.RecipeSummary
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodSearchScreen(
    onBack: () -> Unit,
    onFoodSelected: (String) -> Unit,
    onCreateCustom: () -> Unit,
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

    var handledInitial by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!handledInitial) {
            handledInitial = true

            if (initialQuery.isNotBlank()) viewModel.setQuery(initialQuery)
            when (initialMode) {
                "SCAN" -> onScan()
                "PHOTO" -> aiMode = AiMode.PHOTO
                "DESCRIBE" -> aiMode = AiMode.TEXT
                else -> {}
            }
        }
    }
    val recents by viewModel.recents.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val myFoods by viewModel.myFoods.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val mostLogged by viewModel.mostLogged.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val openFood by viewModel.openFood.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val templateApplied by viewModel.templateApplied.collectAsState()

    val multiSelect = !pickMode && aiSlot != null && aiEpochDay != null

    val marcados = state.selected.size

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

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.search_title), onBack = onBack) },
        floatingActionButton = {

            when {
                marcados > 0 && aiSlot != null && aiEpochDay != null ->
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.logSelected(aiSlot, aiEpochDay) },
                        // Decorativo: o botão traz o texto ao lado do ícone.
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(stringResource(Res.string.search_log_selected, marcados)) },
                    )
                !pickMode -> ExtendedFloatingActionButton(
                    onClick = onCreateCustom,
                    // Decorativo: o botão traz o texto ao lado do ícone.
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.food_create_cta)) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text(stringResource(Res.string.search_hint)) },
                // Decorativo: a lupa repete o rótulo do campo de pesquisa.
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (!pickMode) {
                        IconButton(onClick = onScan) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(Res.string.search_scan),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                singleLine = true,
            )

            if (!pickMode && aiSlot != null && aiEpochDay != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    SecondaryButton(
                        text = "✨ " + stringResource(Res.string.ai_describe),
                        onClick = { aiMode = AiMode.TEXT },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "📷 " + stringResource(Res.string.ai_photo),
                        onClick = { aiMode = AiMode.PHOTO },
                        modifier = Modifier.weight(1f),
                    )
                }

                aiMode?.let { mode ->
                    AiFoodSheet(
                        mode = mode,
                        mealSlot = aiSlot,
                        epochDay = aiEpochDay,
                        onDismiss = { aiMode = null },
                    )
                }
            }

            if (state.tab == SearchTab.SEARCH && suggestions.isNotEmpty() && state.results.isEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    suggestions.forEach { food ->
                        AssistChip(
                            onClick = { onFoodSelected(food.id) },
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

            val tabs = buildList {
                add(SearchTab.SEARCH to Res.string.search_tab_search)
                add(SearchTab.RECENTS to Res.string.search_tab_recents)
                add(SearchTab.FAVORITES to Res.string.search_tab_favorites)
                add(SearchTab.MINE to Res.string.search_tab_mine)
                if (!pickMode) add(SearchTab.RECIPES to Res.string.search_tab_recipes)

                if (!pickMode && aiSlot != null && aiEpochDay != null) {
                    add(SearchTab.TEMPLATES to Res.string.search_tab_templates)
                }
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

            when (state.tab) {
                SearchTab.SEARCH -> if (state.query.length < 2) {

                    YourStuff(
                        foods = mostLogged,
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
                    )
                }
                SearchTab.RECIPES -> RecipesTab(
                    recipes = recipes,
                    onNew = onNewRecipe,
                    onSelect = onRecipeSelected,
                    onEdit = onEditRecipe,
                )
                SearchTab.TEMPLATES -> TemplatesTab(
                    templates = templates,
                    onApply = { id ->
                        if (aiSlot != null && aiEpochDay != null) {
                            viewModel.applyTemplate(id, aiSlot, aiEpochDay)
                        }
                    },
                    onDelete = viewModel::deleteTemplate,
                )
                else -> {
                    val list = when (state.tab) {
                        SearchTab.RECENTS -> recents
                        SearchTab.FAVORITES -> favorites
                        SearchTab.MINE -> myFoods
                        else -> emptyList()
                    }
                    if (list.isEmpty()) {
                        EmptyState(
                            title = stringResource(Res.string.search_empty_title),
                            subtitle = stringResource(Res.string.search_empty_subtitle),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(list, key = { it.id }) { food ->
                                FoodRow(
                                    food = food,
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
    }
}

@Composable
private fun YourStuff(
    foods: List<FoodEntity>,
    selectable: Boolean,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    templates: List<pt.antares.app.core.database.entities.MealTemplateEntity>,
    onFood: (FoodEntity) -> Unit,
    onTemplate: (String) -> Unit,
) {
    if (foods.isEmpty() && templates.isEmpty()) {
        EmptyState(title = stringResource(Res.string.search_min_chars))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (templates.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(Res.string.search_your_meals),
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
            items(templates, key = { "tpl-${it.id}" }) { template ->
                Card(
                    onClick = { onTemplate(template.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                ) {
                    ListItem(
                        headlineContent = { Text(template.name, maxLines = 2) },
                        supportingContent = {
                            Text(
                                mealSlotLabel(template.slot),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        }
        if (foods.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(Res.string.search_your_foods),
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
            items(foods, key = { "top-${it.id}" }) { food ->
                FoodRow(
                    food = food,
                    selectable = selectable,
                    selected = food.id in selectedIds,
                    onToggle = { onToggle(food.id) },
                    onClick = { onFood(food) },
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: FoodSearchState,
    selectable: Boolean,
    onToggle: (String) -> Unit,
    onLocal: (FoodEntity) -> Unit,
    onOnline: (FoodEntity) -> Unit,
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

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (state.results.isNotEmpty()) {
            item { SectionHeader(title = stringResource(Res.string.search_section_local), modifier = Modifier.padding(Spacing.sm)) }
            items(state.results, key = { "local-${it.id}" }) { food ->
                FoodRow(
                    food = food,
                    selectable = selectable,
                    selected = food.id in state.selected,
                    onToggle = { onToggle(food.id) },
                    onClick = { onLocal(food) },
                )
            }
        }
        if (state.onlineResults.isNotEmpty() || state.searchingOnline) {
            item { SectionHeader(title = stringResource(Res.string.search_section_online), modifier = Modifier.padding(Spacing.sm)) }
        }
        items(state.onlineResults, key = { "off-${it.id}" }) { food ->
            FoodRow(food = food, online = true, onClick = { onOnline(food) })
        }
        if (state.searchingOnline) {
            item { LoadingState() }
        }
    }
}

@Composable
private fun RecipesTab(
    recipes: List<RecipeSummary>,
    onNew: () -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SecondaryButton(
                text = stringResource(Res.string.recipe_new),
                onClick = onNew,
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            )
        }
        if (recipes.isEmpty()) {
            item { EmptyState(title = stringResource(Res.string.search_empty_title)) }
        }
        items(recipes, key = { it.recipe.id }) { summary ->
            Card(
                onClick = { onSelect(summary.recipe.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            ) {
                ListItem(
                    headlineContent = { Text(summary.recipe.name, maxLines = 2) },
                    supportingContent = {
                        Text(
                            "${summary.nutrition.kcalPer100} ${stringResource(Res.string.common_kcal)} / 100 " +
                                "${stringResource(Res.string.common_grams_short)} · " +
                                "${summary.ingredientCount} ${stringResource(Res.string.recipe_ingredients)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { onEdit(summary.recipe.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.recipe_edit))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TemplatesTab(
    templates: List<pt.antares.app.core.database.entities.MealTemplateEntity>,
    onApply: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (templates.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.templates_empty_title),
            subtitle = stringResource(Res.string.templates_empty_subtitle),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(templates, key = { it.id }) { template ->
            Card(
                onClick = { onApply(template.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            ) {
                ListItem(
                    headlineContent = { Text(template.name, maxLines = 2) },
                    supportingContent = {
                        Text(
                            mealSlotLabel(template.slot),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { onDelete(template.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.templates_delete),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FoodRow(
    food: FoodEntity,
    online: Boolean = false,
    selectable: Boolean = false,
    selected: Boolean = false,
    onToggle: () -> Unit = {},
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
    ) {

        val curatedPt = FoodProvenance.of(food.source, food.id) == FoodProvenance.CURATED
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(food.namePt.ifBlank { food.nameEn }, maxLines = 2, modifier = Modifier.weight(1f, fill = false))

                    if (curatedPt) SourceBadge(stringResource(Res.string.search_badge_pt))
                }
            },
            supportingContent = {
                Text(
                    "${food.kcal} ${stringResource(Res.string.common_kcal)} / 100 ${stringResource(Res.string.common_grams_short)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        online -> Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = stringResource(Res.string.search_section_online),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        food.isFavorite -> Icon(
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
