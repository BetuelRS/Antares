package pt.antares.app.core.designsystem.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Um número que encolhe até caber, em vez de se partir ao meio.
 *
 * Três totais lado a lado num telemóvel de 360 dp davam «107:56:0» com o «2» na linha
 * seguinte — um tempo partido lê-se como outro tempo. Cortar com reticências seria pior:
 * «630.2…» é um número errado com ar de certo.
 *
 * Mede-se e volta a desenhar-se um degrau mais pequeno até caber ou até ao mínimo. O
 * `drawText = false` da primeira passagem evita o piscar do tamanho grande.
 */
@Composable
fun AutoShrinkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    minScale: Float = 0.6f,
) {
    var escala by remember(text, style) { mutableStateOf(1f) }
    var medido by remember(text, style) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = style.fontSize * escala),
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        onTextLayout = { resultado ->
            if (!medido && resultado.hasVisualOverflow && escala > minScale) {
                escala = (escala - DEGRAU).coerceAtLeast(minScale)
            } else {
                medido = true
            }
        },
    )
}

// Cinco por cento de cada vez: passos maiores saltam o tamanho que caberia por pouco.
private const val DEGRAU = 0.05f
