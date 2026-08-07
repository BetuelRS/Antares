package pt.antares.app.feature.crash

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.util.rememberFileSharer
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.crash_clear
import pt.antares.app.generated.resources.crash_culprit
import pt.antares.app.generated.resources.crash_empty
import pt.antares.app.generated.resources.crash_explain
import pt.antares.app.generated.resources.crash_share
import pt.antares.app.generated.resources.crash_title

@Composable
fun CrashScreen(
    onBack: () -> Unit,
    viewModel: CrashViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val partilhar = rememberFileSharer()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.crash_title), onBack = onBack) },
    ) { padding ->
        if (state.aLer) {
            LoadingState(Modifier.padding(padding))
            return@AntaresScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(Res.string.crash_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val relatorio = state.relatorio
            if (relatorio == null) {
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(Res.string.crash_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                return@Column
            }

            state.culpado?.let { culpado ->
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(Res.string.crash_culprit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        culpado,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            AntaresCard(modifier = Modifier.fillMaxWidth()) {

                Text(
                    relatorio,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryButton(
                    text = stringResource(Res.string.crash_share),
                    onClick = { partilhar("antares-crash.txt", "text/plain", relatorio) },
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(Res.string.crash_clear),
                    onClick = viewModel::limpar,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
