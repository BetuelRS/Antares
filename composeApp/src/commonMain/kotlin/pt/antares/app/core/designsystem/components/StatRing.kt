package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StatRing(
    progress: Float,
    centerValue: String,
    modifier: Modifier = Modifier,
    centerTitle: String? = null,
    size: Dp = 160.dp,
    strokeWidth: Dp = 14.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    // Trava em 100%: passar a meta dava mais de uma volta e o anel voltava a parecer vazio.
    // Quem passou vê-o cheio, e o número ao centro é que diz quanto.
    val clamped = progress.coerceIn(0f, 1f)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        // O desenho é decorativo: o valor e o rótulo estão nos textos do centro, e são
        // esses que o leitor de ecrã anuncia. Descrever o anel repetia-os.
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            // -90° põe o início no topo: a zero graus o arco começava à direita.
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = centerValue, style = MaterialTheme.typography.displayLarge)
            if (centerTitle != null) {
                Text(text = centerTitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
