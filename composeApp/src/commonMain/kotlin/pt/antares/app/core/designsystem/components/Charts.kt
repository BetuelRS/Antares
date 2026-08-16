package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.chart_cd

@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    if (values.isEmpty()) return

    // Um gráfico desenhado não diz nada a um leitor de ecrã. Sem descrição do ecrã que o
    // usa, fica pelo menos a forma: quantos pontos, de onde a onde.
    val descricao = contentDescription ?: stringResource(
        Res.string.chart_cd,
        values.size,
        fmtG(values.min().toDouble()),
        fmtG(values.max().toDouble()),
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { this.contentDescription = descricao },
    ) {
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = if (values.size > 1) i * stepX else size.width / 2f

            val y = size.height - ((v - minV) / range) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 4f))

        val lastX = if (values.size > 1) (values.size - 1) * stepX else size.width / 2f
        val lastY = size.height - ((values.last() - minV) / range) * size.height
        drawCircle(color, radius = 6f, center = Offset(lastX, lastY))
    }
}

@Composable
fun LabeledBar(
    label: String,
    value: Float,
    maxValue: Float,
    valueText: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val fraction = if (maxValue > 0f) (value / maxValue).coerceIn(0f, 1f) else 0f
    Column(modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        SplitRow(
            leading = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            trailing = {
                Text(valueText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
        )
        Box(
            Modifier.fillMaxWidth().height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier.fillMaxWidth(fraction).height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color),
            )
        }
    }
}
