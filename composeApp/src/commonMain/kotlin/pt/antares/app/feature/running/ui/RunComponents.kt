package pt.antares.app.feature.running.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AutoShrinkText
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.feature.running.domain.Split
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_pace_unit
import pt.antares.app.generated.resources.run_summary_splits

/**
 * Os parciais ficam em quilómetros mesmo com o imperial escolhido, e o título di-lo.
 *
 * São medidos e gravados por quilómetro pelo motor da corrida: convertê-los para milhas era
 * recalcular os parciais a partir do percurso, e não mudar um rótulo. Mostrar «parcial 1» com
 * ritmo por milha seria pior do que isto — seria uma tabela com duas unidades ao mesmo tempo.
 */
@Composable
fun SplitsTable(splits: List<Split>) {
    // Menos de um quilómetro não tem parciais para mostrar, e uma tabela com um cabeçalho e
    // nada por baixo é pior do que não haver tabela.
    if (splits.isEmpty()) return
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.run_summary_splits), style = MaterialTheme.typography.titleSmall)
        splits.forEach { s ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${s.index}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    RunFormat.pace(s.paceSecPerKm, UnitSystem.METRIC) +
                        " ${stringResource(Res.string.run_pace_unit)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("${s.kcal} kcal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PrTile(label: String, value: String, modifier: Modifier = Modifier) {
    AntaresCard(modifier = modifier) {
        // Uma linha só, e o texto a encolher até caber: três destes lado a lado num telemóvel
        // de 360 dp partiam «107:56:02» em «107:56:0» e «2» na linha seguinte, e «630.21 mi»
        // em duas. Um total lê-se de relance ou não serve de nada.
        AutoShrinkText(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

