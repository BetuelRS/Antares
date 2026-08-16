package pt.antares.app.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.inlineBold
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.about_changelog_lang
import pt.antares.app.generated.resources.about_current
import pt.antares.app.generated.resources.about_previous
import pt.antares.app.generated.resources.about_title

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val current = AppChangelog.versions.first()
    val previous = AppChangelog.versions.drop(1)

    val english = stringResource(Res.string.about_changelog_lang) == "en"

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.about_title), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).larguraDeLeitura().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text(stringResource(Res.string.about_current), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VersionCard(current, highlighted = true, english = english)
            }
            item { SectionHeader(title = stringResource(Res.string.about_previous)) }

            items(previous) { VersionCard(it, highlighted = false, english = english) }
        }
    }
}

@Composable
private fun VersionCard(version: AppVersion, highlighted: Boolean, english: Boolean) {
    AntaresCard(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
        Text(
            "v${version.name} · ${version.title(english)}",
            style = if (highlighted) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        version.highlights(english).forEach { line ->
            Text(
                inlineBold("• $line"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}
