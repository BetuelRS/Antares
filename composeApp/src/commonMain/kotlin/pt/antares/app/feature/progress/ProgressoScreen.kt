package pt.antares.app.feature.progress

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresScreen
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.nav_progress

/**
 * O progresso, agora com separador próprio.
 *
 * Vivia dentro do «Eu», atrás de um ícone de pessoa. O `estudo/areas/14-progresso.md` dá-lhe
 * **17 em 20** — a nota mais alta do estudo — e escreve o problema numa linha: *«o melhor
 * ecrã da app está atrás de um nome que não o descreve»*.
 *
 * O ecrã é fino de propósito: o conteúdo é o [ProgressSections], que já existia e não muda.
 * O que mudou foi onde se chega a ele.
 */
@Composable
fun ProgressoScreen(
    onWeightHistory: () -> Unit,
    onPhotos: () -> Unit,
) {
    AntaresScreen(
        topBar = { AntaresTopBar(title = stringResource(Res.string.nav_progress)) },
        espaco = Spacing.sm,
        margem = PaddingValues(Spacing.lg),
    ) {
        ProgressSections(
            onWeightHistory = onWeightHistory,
            onPhotos = onPhotos,
        )
    }
}
