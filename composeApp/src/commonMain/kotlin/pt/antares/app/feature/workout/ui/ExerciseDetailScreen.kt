package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.ConfirmDialog
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.Sparkline
import pt.antares.app.core.designsystem.loadWithUnit
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    if (state.aConfirmarApagar) {
        ConfirmDialog(
            title = stringResource(Res.string.excustom_delete_title),
            message = if (state.rotinasCom > 0) {
                pluralStringResource(Res.plurals.excustom_delete_em_rotinas, state.rotinasCom, state.rotinasCom)
            } else {
                stringResource(Res.string.excustom_delete_sem_rotinas)
            },
            confirmLabel = stringResource(Res.string.excustom_delete),
            dismissLabel = stringResource(Res.string.common_cancel),
            onConfirm = viewModel::confirmarApagar,
            onDismiss = viewModel::cancelarApagar,
        )
    }

    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                title = state.exercise?.displayName ?: "",
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::alternarFavorito) {
                        Icon(
                            if (state.favorito) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(
                                if (state.favorito) Res.string.exlib_desmarcar else Res.string.exlib_marcar,
                            ),
                            tint = if (state.favorito) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (state.exercise?.isCustom == true) {
                        IconButton(onClick = viewModel::pedirParaApagar) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.excustom_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val ex = state.exercise
        if (state.loading || ex == null) {
            LoadingState(Modifier.padding(padding))
            return@AntaresScaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (ex.imageUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(ex.imageUrls, key = { it }) { url ->
                        ExerciseImage(
                            url = url,
                            exerciseName = ex.displayName,
                            modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1.4f).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            }

            val meta = listOfNotNull(
                stringResource(categoryLabel(ex.category)),
                ex.equipment?.let { stringResource(equipmentLabel(it)) },
                stringResource(levelLabel(ex.level)),
            ).joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            CartaoDeDesempenho(state, rememberUnitSystem())

            if (ex.primaryMuscles.isNotEmpty()) {
                MuscleRow(Res.string.exdetail_primary, ex.primaryMuscles)
            }
            if (ex.secondaryMuscles.isNotEmpty()) {
                MuscleRow(Res.string.exdetail_secondary, ex.secondaryMuscles)
            }

            if (state.progress.size >= 2) {
                Text(stringResource(Res.string.exdetail_progress), style = MaterialTheme.typography.titleMedium)
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Sparkline(values = state.progress)
                }
            }

            Text(stringResource(Res.string.exdetail_instructions), style = MaterialTheme.typography.titleMedium)
            if (ex.instructionsUntranslated) {
                Text(
                    stringResource(Res.string.exdetail_untranslated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (ex.instructions.isEmpty()) {
                Text(stringResource(Res.string.exdetail_no_instructions), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                ex.instructions.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * O que a pessoa já fez neste exercício.
 *
 * **Não aparece a zeros:** sem desempenho não há cartão. «Nunca fizeste isto» e «fizeste e
 * deu zero» são coisas diferentes, e a segunda não acontece — é a mesma regra que faz a água
 * da comida calar-se em vez de fingir um zero.
 *
 * Em `FlowRow` e não em colunas de largura repartida: são quatro números com rótulo, e a
 * 200 % de escala de letra quatro colunas fixas dão quatro palavras na vertical.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoDeDesempenho(state: ExerciseDetailState, unidades: UnitSystem) {
    val d = state.desempenho ?: return

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.exdetail_desempenho),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Numero(
                stringResource(Res.string.exdetail_melhor_serie),
                stringResource(
                    Res.string.exdetail_melhor_serie_valor,
                    loadWithUnit(d.melhorPesoKg, unidades),
                    d.melhorReps,
                ),
            )
            if (d.umRmKg != null) {
                Numero(
                    stringResource(Res.string.exdetail_um_rm),
                    loadWithUnit(d.umRmKg, unidades),
                )
            }
            Numero(
                stringResource(Res.string.exdetail_vezes),
                d.vezes.toString(),
            )
            Numero(
                stringResource(Res.string.exdetail_ultima),
                dayShortDated(epochMillisToLocalDate(d.ultimaEm).toEpochDays().toLong()),
            )
        }
    }
}

@Composable
private fun Numero(rotulo: String, valor: String) {
    Column {
        Text(
            rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MuscleRow(label: org.jetbrains.compose.resources.StringResource, muscles: List<String>) {
    // O `map` com um `stringResource` lá dentro era o defeito concreto 1 da `estudo/areas/09`:
    // uma chamada composable por elemento dentro de uma lambda, que é o padrão que o resto da
    // app evita. Os rótulos passam a resolver-se numa passagem só, fora do `joinToString`.
    val nomes = mutableListOf<String>()
    for (m in muscles) nomes += stringResource(muscleLabel(m))
    val names = nomes.joinToString(", ")

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(names, style = MaterialTheme.typography.bodyLarge)
    }
}
