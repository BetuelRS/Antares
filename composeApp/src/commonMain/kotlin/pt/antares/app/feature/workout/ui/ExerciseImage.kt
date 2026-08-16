package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.exercise_image_cd
import pt.antares.app.generated.resources.exercise_image_offline

/**
 * A imagem de um exercício, com os três estados que ela tem de facto.
 *
 * Era um `AsyncImage` cru: sem esboço, sem erro e sem indicador. Numa app que se apresenta
 * como offline, isso dava um retângulo vazio sem explicação — e um retângulo vazio lê-se
 * como uma app partida, não como uma imagem que precisa de ligação.
 *
 * O esboço é uma superfície da paleta e não um indicador a girar: a imagem chega da cache em
 * disco quase sempre, e um indicador que aparece e desaparece num instante pisca mais do que
 * informa.
 */
@Composable
fun ExerciseImage(
    url: String?,
    exerciseName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    comTexto: Boolean = true,
) {
    if (url == null) {
        Esboco(modifier)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = stringResource(Res.string.exercise_image_cd, exerciseName),
        contentScale = contentScale,
        modifier = modifier,
    ) {
        // O `Empty` conta como carregar: é o instante antes de o pedido arrancar, e tratá-lo
        // como erro piscava a mensagem de falta de ligação em cada imagem que abre bem.
        when (painter.state.collectAsState().value) {
            is AsyncImagePainter.State.Error -> SemLigacao(comTexto)
            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            else -> Esboco(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Esboco(modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
private fun SemLigacao(comTexto: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CloudOff,
            // Decorativo quando o texto o acompanha; sozinho, é ele que diz o que aconteceu,
            // e a descrição da imagem já está no `SubcomposeAsyncImage` à volta.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ICONE_DP.dp),
        )
        if (comTexto) {
            Text(
                stringResource(Res.string.exercise_image_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.xs, start = Spacing.sm, end = Spacing.sm),
            )
        }
    }
}

private const val ICONE_DP = 24
