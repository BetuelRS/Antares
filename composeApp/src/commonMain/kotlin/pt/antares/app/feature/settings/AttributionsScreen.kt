package pt.antares.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun AttributionsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.attrib_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(Res.string.attrib_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                stringResource(Res.string.attrib_coverage, CATALOGUE_FOODS, CATALOGUE_MICRO_PCT),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Source(Res.string.attrib_tca_title, Res.string.attrib_tca_desc)
            Source(Res.string.attrib_ciqual_title, Res.string.attrib_ciqual_desc)
            Source(Res.string.attrib_usda_title, Res.string.attrib_usda_desc)
            Source(Res.string.attrib_off_title, Res.string.attrib_off_desc)
            Source(Res.string.attrib_efsa_title, Res.string.attrib_efsa_desc)
        }
    }
}

@Composable
private fun Source(title: StringResource, desc: StringResource) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val CATALOGUE_FOODS = 6688
private const val CATALOGUE_MICRO_PCT = 96
