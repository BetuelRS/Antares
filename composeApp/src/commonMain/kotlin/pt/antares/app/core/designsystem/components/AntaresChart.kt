package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import pt.antares.app.core.calc.ChartScale
import pt.antares.app.core.calc.TimeAxis

@Composable
fun AntaresChart(

    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,

    trend: List<Pair<Long, Double>> = emptyList(),

    targetValue: Double? = null,
    height: Int = DEFAULT_HEIGHT_DP,
    pointColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trendColor: Color = MaterialTheme.colorScheme.primary,
    targetColor: Color = MaterialTheme.colorScheme.tertiary,
    gridColor: Color = MaterialTheme.colorScheme.outline,

    labels: @Composable (ChartScale, TimeAxis) -> Unit = { _, _ -> },
) {
    if (points.isEmpty()) return
    val crus = points.sortedBy { it.first }
    val linha = trend.sortedBy { it.first }
    val scale = ChartScale.of(crus.map { it.second } + linha.map { it.second } + listOfNotNull(targetValue))
    val eixo = TimeAxis.of(crus.map { it.first } + linha.map { it.first }) ?: return

    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height.dp)) {
            fun y(value: Double): Float =
                size.height - (scale.fraction(value) * size.height).toFloat()
            fun x(day: Long): Float = (eixo.fraction(day) * size.width).toFloat()

            for (tick in scale.ticks) {
                val ty = y(tick)
                drawLine(
                    color = gridColor.copy(alpha = GRID_ALPHA),
                    start = Offset(0f, ty),
                    end = Offset(size.width, ty),
                    strokeWidth = 1f,
                )
            }

            for (dia in eixo.tickDays()) {
                val tx = x(dia)
                drawLine(
                    color = gridColor.copy(alpha = GRID_ALPHA / 2),
                    start = Offset(tx, 0f),
                    end = Offset(tx, size.height),
                    strokeWidth = 1f,
                )
            }

            targetValue?.let {
                val ty = y(it)
                drawLine(
                    color = targetColor,
                    start = Offset(0f, ty),
                    end = Offset(size.width, ty),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                )
            }

            drawSeries(crus, ::x, ::y, pointColor.copy(alpha = RAW_ALPHA), strokeWidth = 2.5f)

            if (linha.isNotEmpty()) {
                drawSeries(linha, ::x, ::y, trendColor, strokeWidth = 5f)
                val ultimo = linha.last()
                drawCircle(trendColor, radius = 7f, center = Offset(x(ultimo.first), y(ultimo.second)))
            }
        }
        labels(scale, eixo)
    }
}

private fun DrawScope.drawSeries(
    values: List<Pair<Long, Double>>,
    x: (Long) -> Float,
    y: (Double) -> Float,
    color: Color,
    strokeWidth: Float,
) {
    if (values.isEmpty()) return

    if (values.size == 1) {
        drawCircle(
            color,
            radius = strokeWidth * 1.5f,
            center = Offset(x(values[0].first), y(values[0].second)),
        )
        return
    }
    val path = Path()
    values.forEachIndexed { i, (dia, valor) ->
        val px = x(dia)
        val py = y(valor)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth))
}

private const val DEFAULT_HEIGHT_DP = 160
private const val GRID_ALPHA = 0.35f
private const val RAW_ALPHA = 0.45f
