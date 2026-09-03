package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.FilterBar
import pt.antares.app.core.designsystem.components.FilterDropdownChip
import pt.antares.app.core.designsystem.components.FilterOption
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.dayShort
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.formatDurationMin
import pt.antares.app.core.util.mesLabel
import pt.antares.app.feature.workout.data.SessionSummary
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun WorkoutHistoryScreen(
    onSession: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: WorkoutHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.workout_history_title), onBack = onBack) },
    ) { padding ->
        if (state.todos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.workout_history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@AntaresScaffold
        }
        ListaAdaptavel(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
            espaco = Spacing.sm,
        ) {
            linhaInteira {
                FilterBar {
                    FilterDropdownChip(
                        label = stringResource(Res.string.filter_month),
                        selected = state.mes,
                        options = state.meses.map { FilterOption(it, mesLabel(it)) },
                        onSelect = viewModel::setMes,
                    )
                    FilterDropdownChip(
                        label = stringResource(Res.string.filter_routine),
                        selected = state.rotinaId,
                        options = state.rotinas.map { FilterOption(it.id, it.name) },
                        onSelect = viewModel::setRotina,
                    )
                }
            }

            // Uma lista vazia por causa do filtro não é um histórico vazio, e dizer-lhe o
            // mesmo era mandar a pessoa procurar treinos que ela tem.
            if (state.visiveis.isEmpty()) {
                linhaInteira {
                    Text(
                        stringResource(Res.string.filter_no_match),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.lg),
                    )
                }
            }

            items(state.visiveis, key = { it.id }) { s ->
                LinhaDoHistorico(s, unidades, onSession)
            }
        }
    }
}

/**
 * A linha do histórico.
 *
 * Tinha dois dados — a data e o volume — e por isso «sáb, 9 ago · 9 338 kg» e «ter, 12 ago ·
 * 9 340 kg» podiam ser um dia de pernas e um de braços, e liam-se como o mesmo treino. Passa
 * a ter quatro, mais a estrela quando houve recorde.
 *
 * **O volume continua à direita e continua a ser o quinto facto, não o primeiro.** Ele não é
 * comparável entre grupos musculares — um dia de pernas tem sempre mais do que um de braços —
 * e como única métrica visível fazia a alternância do plano parecer progressão.
 */
@Composable
private fun LinhaDoHistorico(
    s: SessionSummary,
    unidades: UnitSystem,
    onSession: (String) -> Unit,
) {
    LinhaDaLista(
        // Um treino livre não nasceu de rotina nenhuma, e escrever o nome de uma seria
        // inventá-lo — a mesma regra da linha do painel de treino.
        titulo = s.nomeDaRotina ?: stringResource(Res.string.workout_hub_free_workout),
        subtitulo = stringResource(
            Res.string.workout_history_line,
            dayShort(epochMillisToLocalDate(s.startedAt)),
            formatDurationMin(s.durationMin),
            pluralStringResource(Res.plurals.workout_hub_series, s.series, s.series),
        ),
        aoLado = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.temRecorde) {
                    // A estrela é o único facto da linha que não está escrito, e um leitor
                    // de ecrã não lê um emoji como palavra. O recurso lê-se aqui e não
                    // dentro do `semantics`, que não é um contexto de composição.
                    val recorde = stringResource(Res.string.workout_history_pr)
                    Text(
                        stringResource(Res.string.workout_history_pr_mark),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(end = Spacing.xs)
                            .semantics { contentDescription = recorde },
                    )
                }
                Text(
                    weightWithUnit(s.volume, unidades),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = { onSession(s.id) },
    )
}
