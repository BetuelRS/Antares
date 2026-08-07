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
import pt.antares.app.feature.running.domain.Split
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_summary_splits

@Composable
fun SplitsTable(splits: List<Split>) {
    if (splits.isEmpty()) return
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.run_summary_splits), style = MaterialTheme.typography.titleSmall)
        splits.forEach { s ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${s.index}", style = MaterialTheme.typography.bodyMedium)
                Text(RunFormat.pace(s.paceSecPerKm), style = MaterialTheme.typography.bodyMedium)
                Text("${s.kcal} kcal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PrTile(label: String, value: String, modifier: Modifier = Modifier) {
    AntaresCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
