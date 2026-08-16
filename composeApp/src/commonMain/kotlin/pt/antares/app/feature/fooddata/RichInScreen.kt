package pt.antares.app.feature.fooddata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.nutrition.microLabelRes
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichInScreen(
    onFoodSelected: (String) -> Unit,
    onBack: () -> Unit,

    initialKey: String? = null,
    viewModel: RichInViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var keys by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        keys = viewModel.searchableKeys()

        if (initialKey != null) viewModel.pick(initialKey)
    }

    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.rich_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(Res.string.rich_pick),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                keys.forEach { key ->
                    FilterChip(
                        selected = state.key == key,
                        onClick = { viewModel.pick(key) },
                        label = { Text(stringResource(microLabelRes(key))) },
                    )
                }
            }

            when {
                state.loading -> LoadingState()
                state.key == null -> Unit
                state.results.isEmpty() -> EmptyState(title = stringResource(Res.string.rich_empty))
                else -> {
                    Text(
                        stringResource(Res.string.rich_explain),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // A coluna à volta já dá a margem lateral; repeti-la aqui afastava as
                    // linhas da lista das opções que estão por cima delas.
                    ListaAdaptavel(contentPadding = PaddingValues(), espaco = Spacing.xs) {
                        items(state.results, key = { it.foodId }) { r ->
                            Card(
                                onClick = { onFoodSelected(r.foodId) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                ListItem(
                                    headlineContent = { Text(r.name, maxLines = 2) },
                                    supportingContent = {
                                        Text(
                                            stringResource(Res.string.rich_row, r.perKcalPct, r.per100gPct),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
