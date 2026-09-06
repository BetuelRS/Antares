package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.feature.workout.model.WorkoutTaxonomy
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun ExerciseLibraryScreen(
    pickMode: Boolean,
    onExercise: (String) -> Unit,
    onCreateCustom: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExerciseLibraryViewModel = koinViewModel(),

    /**
     * O que já está na rotina que se está a montar. Só marca — **não esconde**: há quem
     * repita o mesmo exercício de propósito na mesma rotina, e esconder tirava-lhe isso sem
     * o dizer. É a mesma postura do aviso de alimento duplicado, que avisa e deixa gravar.
     */
    jaNaRotina: Set<String> = emptySet(),
) {
    val state by viewModel.state.collectAsState()
    val filters = state.filtros

    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(if (pickMode) Res.string.exlib_pick_title else Res.string.exlib_title),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            if (!pickMode) {
                ExtendedFloatingActionButton(
                    onClick = onCreateCustom,
                    // Decorativo: o botão traz o texto ao lado do ícone.
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.exlib_create_custom)) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg)) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setQuery,
                label = { Text(stringResource(Res.string.exlib_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )

            BarraDeFiltros(filters, viewModel)

            if (state.resultados.isEmpty() && !state.mostrarTeus) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Res.string.exlib_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                ListaAdaptavel(
                    modifier = Modifier.fillMaxWidth(),
                    // O botão de criar exercício flutua por cima da lista, e sem esta folga
                    // tapava a última linha — que é sempre um exercício que existe.
                    contentPadding = PaddingValues(bottom = FOLGA_DO_BOTAO_DP.dp),
                    espaco = Spacing.xs,
                ) {
                    if (state.mostrarTeus) {
                        seccao(Res.string.exlib_favoritos, state.favoritos, "fav", jaNaRotina, viewModel, onExercise)
                        seccao(Res.string.exlib_mais_feitos, state.maisFeitos, "uso", jaNaRotina, viewModel, onExercise)
                        linhaInteira(key = "todos-titulo") {
                            TituloDeSeccao(stringResource(Res.string.exlib_todos))
                        }
                    }
                    items(state.resultados, key = { it.exercicio.id }) { linha ->
                        LinhaDoExercicio(linha, linha.exercicio.id in jaNaRotina, viewModel, onExercise)
                    }
                }
            }
        }
    }
}

/**
 * Os três filtros numa linha que **quebra** em vez de cortar.
 *
 * Antes eram três chips de largura repartida com o texto a `maxLines = 1` e sem `overflow`:
 * o valor por omissão é `Clip`, que corta a letra a meio. Lia-se «Equipame» já a 100 % de
 * escala de letra, e «Mús · Equi · Nível» a 200 %. É a família que o
 * `estudo/transversal/03-acessibilidade.md` §3.1 nomeia, e a correcção é a mesma das outras
 * três vezes: tirar a largura fixa, e não afiná-la.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BarraDeFiltros(filters: LibraryFilters, viewModel: ExerciseLibraryViewModel) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        FilterDropdown(
            label = Res.string.exlib_filter_muscle,
            selected = filters.muscle,
            options = WorkoutTaxonomy.muscles,
            optionLabel = ::muscleLabel,
            onSelect = viewModel::setMuscle,
        )
        FilterDropdown(
            label = Res.string.exlib_filter_equipment,
            selected = filters.equipment,
            options = WorkoutTaxonomy.equipment,
            optionLabel = ::equipmentLabel,
            onSelect = viewModel::setEquipment,
        )
        FilterChip(
            selected = filters.soMeus,
            onClick = { viewModel.setSoMeus(!filters.soMeus) },
            label = { Text(stringResource(Res.string.exlib_filter_mine)) },
        )
    }
}

@Composable
private fun TituloDeSeccao(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
    )
}

private fun LazyGridScope.seccao(
    titulo: StringResource,
    linhas: List<ExercicioNaLista>,
    prefixo: String,
    jaNaRotina: Set<String>,
    viewModel: ExerciseLibraryViewModel,
    onExercise: (String) -> Unit,
) {
    if (linhas.isEmpty()) return
    linhaInteira(key = "$prefixo-titulo") { TituloDeSeccao(stringResource(titulo)) }
    items(linhas, key = { "$prefixo-${it.exercicio.id}" }) { linha ->
        LinhaDoExercicio(linha, linha.exercicio.id in jaNaRotina, viewModel, onExercise)
    }
}

@Composable
private fun FilterDropdown(
    label: StringResource,
    selected: String?,
    options: List<String>,
    optionLabel: (String) -> StringResource,
    onSelect: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { open = true },
            label = {
                Text(
                    selected?.let { stringResource(optionLabel(it)) } ?: stringResource(label),
                    maxLines = 1,
                    // Reticências e não corte: um rótulo cortado a meio da letra parece a app
                    // avariada; um com reticências diz que há mais palavra.
                    overflow = TextOverflow.Ellipsis,
                )
            },
            // Decorativo: a seta acompanha um campo que já se apresenta pelo rótulo.
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.exlib_filter_all)) },
                onClick = { onSelect(null); open = false },
            )
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(stringResource(optionLabel(opt))) },
                    onClick = { onSelect(opt); open = false },
                )
            }
        }
    }
}

@Composable
private fun LinhaDoExercicio(
    linha: ExercicioNaLista,
    jaNaRotina: Boolean,
    viewModel: ExerciseLibraryViewModel,
    onExercise: (String) -> Unit,
) {
    val ex = linha.exercicio
    val muscleText = ex.primaryMuscles.firstOrNull()?.let { stringResource(muscleLabel(it)) }
    val equipText = ex.equipment?.let { stringResource(equipmentLabel(it)) }
    val jaText = if (jaNaRotina) stringResource(Res.string.exlib_ja_na_rotina) else null
    val subtitle = listOfNotNull(muscleText, equipText, jaText).joinToString(" · ")

    LinhaDaLista(
        titulo = ex.displayName,
        subtitulo = subtitle.takeIf { it.isNotEmpty() },
        // Sem texto no erro: numa miniatura de 56 dp uma frase não cabe, e o ícone sozinho
        // já distingue «ainda a carregar» de «não veio».
        inicio = {
            ExerciseImage(
                url = ex.imageUrls.firstOrNull(),
                exerciseName = ex.displayName,
                contentScale = ContentScale.Crop,
                comTexto = false,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
            )
        },
        aoLado = {
            IconButton(onClick = { viewModel.alternarFavorito(ex.id, !linha.favorito) }) {
                Icon(
                    if (linha.favorito) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(
                        if (linha.favorito) Res.string.exlib_desmarcar else Res.string.exlib_marcar,
                    ),
                    tint = if (linha.favorito) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        onClick = { onExercise(ex.id) },
    )
}

// Altura do botão flutuante estendido mais a margem que ele guarda do fundo.
private const val FOLGA_DO_BOTAO_DP = 88
