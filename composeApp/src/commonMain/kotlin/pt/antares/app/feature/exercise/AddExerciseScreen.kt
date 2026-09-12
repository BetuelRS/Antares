package pt.antares.app.feature.exercise

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.generated.resources.ai_describe
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.exercise.MetActivity
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun AddExerciseScreen(
    epochDay: Long,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddExerciseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    AntaresScaffold(
        topBar = {
            AntaresTopBar(title = stringResource(Res.string.exercise_add_title), onBack = onBack)
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@AntaresScaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text(stringResource(Res.string.exercise_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
            )

            SecondaryButton(
                text = "✨ " + stringResource(Res.string.ai_describe),
                onClick = viewModel::openAi,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )

            if (state.ai.open) {
                AiExerciseDialog(
                    state = state.ai,
                    onText = viewModel::setAiText,
                    onAnalyze = viewModel::analyzeAi,
                    onConfirm = { viewModel.confirmAi(epochDay) },
                    onDismiss = viewModel::closeAi,
                )
            }

            FilaDeCategorias(
                categorias = state.categories,
                escolhida = state.category,
                onCategoria = viewModel::setCategory,
            )

            ListaAdaptavel(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(),
                espaco = Spacing.xs,
            ) {
                // À cabeça da lista e não por cima da caixa de procura: num catálogo de 90
                // linhas o campo é a acção principal, e empurrá-lo para baixo custava mais
                // do que os recentes poupam.
                if (state.mostrarRecentes) {
                    linhaInteira(key = "recentes-titulo") {
                        Text(
                            stringResource(Res.string.exercise_recent),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                    }
                    items(state.recentes, key = { "recente-${it.id}" }) { activity ->
                        ActivityRow(
                            activity = activity,
                            selected = state.selected?.id == activity.id,
                            onClick = { viewModel.select(activity) },
                        )
                    }
                    linhaInteira(key = "recentes-fim") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                    }
                }

                if (state.results.isEmpty()) {
                    linhaInteira {
                        // O catálogo de METs é fixo e vem com a app: uma procura sem
                        // resultados é a palavra que não existe nele, e não uma falta de
                        // dados que se resolva registando qualquer coisa.
                        Text(
                            stringResource(Res.string.exercise_search_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.lg),
                        )
                    }
                }
                items(state.results, key = { it.id }) { activity ->
                    ActivityRow(
                        activity = activity,
                        selected = state.selected?.id == activity.id,
                        onClick = { viewModel.select(activity) },
                    )
                }
            }

            if (state.selected != null) {
                CartaoDeRegisto(
                    state = state,
                    onDuration = viewModel::setDuration,
                    onStep = viewModel::changeDuration,
                    onSave = { viewModel.save(epochDay) },
                )
            }
        }
    }
}

@Composable
private fun FilaDeCategorias(
    categorias: List<String>,
    escolhida: String?,
    onCategoria: (String?) -> Unit,
) {
    val listState = rememberLazyListState()
    // A `LazyRow` ancora ao item que já estava visível: sem isto, o chip «Todas» nasce à
    // esquerda de quem já lá estava e fica fora do ecrã, invisível. Mas voltar ao início não
    // pode levar o chip escolhido — é ele que diz o que filtra a lista — e com as categorias do
    // fim da fila os dois não cabem juntos: aí ganha o escolhido, encostado à direita, e o
    // «Todas» fica a uma rolagem.
    LaunchedEffect(escolhida) {
        val cat = escolhida ?: return@LaunchedEffect
        val alvo = categorias.indexOf(cat) + 1
        listState.scrollToItem(0)
        val vista = listState.layoutInfo
        val cabe = vista.visibleItemsInfo.any {
            it.index == alvo && it.offset + it.size <= vista.viewportEndOffset
        }
        if (!cabe) {
            listState.scrollToItem(alvo)
            val depois = listState.layoutInfo
            val item = depois.visibleItemsInfo.firstOrNull { it.index == alvo } ?: return@LaunchedEffect
            listState.scrollBy((item.offset + item.size - depois.viewportEndOffset).toFloat())
        }
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
    ) {
        // Só aparece com categoria escolhida: sem ela é o estado por omissão, e um chip que
        // está sempre lá e nunca faz nada é ruído — estudo/areas/13.
        if (escolhida != null) {
            item(key = "todas") {
                FilterChip(
                    selected = false,
                    onClick = { onCategoria(null) },
                    label = { Text(stringResource(Res.string.exercise_cat_all)) },
                )
            }
        }
        items(categorias, key = { it }) { cat ->
            FilterChip(
                selected = escolhida == cat,
                onClick = { onCategoria(cat) },
                label = { Text(stringResource(categoryLabel(cat))) },
            )
        }
    }
}

/**
 * O que se vai registar: a duração, as calorias que ela dá, e o botão.
 *
 * As calorias mudam com a duração **antes** de se gravar seja o que for — o mesmo padrão da
 * ficha do alimento, e a razão de o ecrã não precisar de uma pré-visualização à parte.
 */
@Composable
private fun CartaoDeRegisto(
    state: AddExerciseState,
    onDuration: (Int) -> Unit,
    onStep: (Int) -> Unit,
    onSave: () -> Unit,
) {
    AntaresCard(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
        state.selected?.let { activity ->
            // No detalhe é útil — diz de onde vem o número; na lista inteira era ruído
            // para quem não sabe o que é um MET. estudo/areas/13, «o que é inútil».
            Text(
                "MET ${activity.met}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CampoDeDuracao(durationMin = state.durationMin, onDuration = onDuration, onStep = onStep)
        Text(
            "${stringResource(Res.string.exercise_burned)}: ${state.previewKcal} " +
                stringResource(Res.string.common_kcal),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        PrimaryButton(
            text = stringResource(Res.string.exercise_save),
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun ActivityRow(activity: MetActivity, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            activity.namePt,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun categoryLabel(category: String): StringResource = when (category) {
    "walking" -> Res.string.exercise_cat_walking
    "running" -> Res.string.exercise_cat_running
    "cycling" -> Res.string.exercise_cat_cycling
    "strength" -> Res.string.exercise_cat_strength
    "swimming" -> Res.string.exercise_cat_swimming
    "sports" -> Res.string.exercise_cat_sports
    "cardio" -> Res.string.exercise_cat_cardio
    "daily" -> Res.string.exercise_cat_daily
    else -> Res.string.exercise_cat_all
}
