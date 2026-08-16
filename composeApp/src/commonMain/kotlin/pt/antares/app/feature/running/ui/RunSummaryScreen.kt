package pt.antares.app.feature.running.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.designsystem.paceUnitLabel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_summary_discard
import pt.antares.app.generated.resources.run_summary_name_hint
import pt.antares.app.generated.resources.run_summary_save
import pt.antares.app.generated.resources.run_summary_no_map
import pt.antares.app.generated.resources.run_summary_title

@Composable
fun RunSummaryScreen(
    onSaved: () -> Unit,
    onDiscarded: () -> Unit,
    viewModel: RunSummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val m = state.metrics
    val unidades = rememberUnitSystem()
    val virgula = virgulaDecimal()
    var name by remember { mutableStateOf("") }

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.run_summary_title)) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .larguraDeLeitura().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.path.isNotEmpty()) {
                RunMap(path = state.path, modifier = Modifier.fillMaxWidth().height(220.dp), follow = false)
            } else {
                // Sem percurso, o resumo abria com o buraco onde o mapa devia estar e não
                // dizia porquê. A distância e o ritmo continuam a valer: vieram do GPS o
                // suficiente para os medir, não o suficiente para desenhar a linha.
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(Res.string.run_summary_no_map),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${RunFormat.distance(m.distanceM, unidades, virgula)} " +
                        stringResource(distanceUnitLabel(unidades)),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${RunFormat.clock(m.movingMs)}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${RunFormat.pace(m.avgPaceSecPerKm, unidades)} ${stringResource(paceUnitLabel(unidades))}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text("${m.kcal} kcal", style = MaterialTheme.typography.bodyLarge)
                }
            }
            SplitsTable(viewModel.splits())
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.run_summary_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            PrimaryButton(stringResource(Res.string.run_summary_save), { viewModel.save(name, "", onSaved) }, Modifier.fillMaxWidth())
            SecondaryButton(stringResource(Res.string.run_summary_discard), { viewModel.discard(onDiscarded) }, Modifier.fillMaxWidth())
        }
    }
}
