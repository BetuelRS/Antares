package pt.antares.app.feature.fasting.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.fasting_hour_short
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FastingTimerRing(
    fraction: Float,
    totalHours: Int,
    centerValue: String,
    centerSubtitle: String,
    reachedGoal: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    strokeWidth: Dp = 18.dp,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = if (reachedGoal) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val markerColor = MaterialTheme.colorScheme.onSurfaceVariant

    // As marcas das horas eram três pontos cinzentos sem nada que dissesse a que horas
    // correspondiam. Um ponto sem legenda é decoração; com a hora ao lado passa a dizer
    // onde é que o jejum vai.
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = markerColor)
    val markers = listOf(4, 8, 12)
        .filter { it < totalHours }
        .map { it to measurer.measure(stringResource(Res.string.fasting_hour_short, it), labelStyle) }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        // O desenho é decorativo: o tempo decorrido e o que falta estão nos textos do
        // centro, e são esses que o leitor de ecrã anuncia.
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke,
            )

            val radius = (size.toPx() - strokeWidth.toPx()) / 2f
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val dotR = strokeWidth.toPx() / 4f
            markers.forEach { (hour, rotulo) ->
                val angle = (-90f + 360f * (hour.toFloat() / totalHours)) * (PI / 180f)
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()
                drawCircle(
                    color = markerColor,
                    radius = dotR,
                    center = Offset(center.x + radius * cosA, center.y + radius * sinA),
                )
                // A legenda fica por dentro do anel: por fora saía da caixa, que tem o
                // tamanho do anel e mais nada.
                val rotuloRadius = radius - strokeWidth.toPx() - rotulo.size.height / 2f
                drawText(
                    textLayoutResult = rotulo,
                    topLeft = Offset(
                        center.x + rotuloRadius * cosA - rotulo.size.width / 2f,
                        center.y + rotuloRadius * sinA - rotulo.size.height / 2f,
                    ),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerValue, style = MaterialTheme.typography.displayMedium)
            Text(
                centerSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (reachedGoal) progressColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
