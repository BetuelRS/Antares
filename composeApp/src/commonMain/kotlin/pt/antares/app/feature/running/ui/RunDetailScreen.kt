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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.util.rememberFileSharer
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.designsystem.paceUnitLabel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.feature.running.domain.GpxWriter
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_detail_export
import pt.antares.app.generated.resources.run_summary_elev
import pt.antares.app.generated.resources.run_summary_title

@Composable
fun RunDetailScreen(
    runId: String,
    onBack: () -> Unit,
    viewModel: RunDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()
    LaunchedEffect(runId) { viewModel.load(runId) }
    val shareFile = rememberFileSharer()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = state.run?.name?.ifBlank { null } ?: stringResource(Res.string.run_summary_title), onBack = onBack) },
    ) { padding ->
        val run = state.run ?: return@AntaresScaffold
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .larguraDeLeitura().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.path.isNotEmpty()) {
                RunMap(path = state.path, modifier = Modifier.fillMaxWidth().height(220.dp), follow = false)
            }
            AntaresCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${RunFormat.distance(run.distanceM, unidades)} ${stringResource(distanceUnitLabel(unidades))}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(RunFormat.clock(run.movingS * 1000))
                    Text("${RunFormat.pace(run.avgPaceSecPerKm, unidades)} ${stringResource(paceUnitLabel(unidades))}")
                    Text("${run.kcal} kcal")
                }
                Text(
                    "${stringResource(Res.string.run_summary_elev)}: ${run.elevGainM.toInt()} m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            SplitsTable(state.splits)
            SecondaryButton(
                stringResource(Res.string.run_detail_export),
                {
                    val gpx = GpxWriter.write(run.name, run.type, run.startedAt, state.path)
                    shareFile("antares-${run.id}.gpx", "application/gpx+xml", gpx)
                },
                Modifier.fillMaxWidth(),
            )
        }
    }
}
