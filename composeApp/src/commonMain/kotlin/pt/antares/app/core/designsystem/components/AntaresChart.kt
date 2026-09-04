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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.calc.ChartScale
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.chart_cd
import pt.antares.app.core.calc.TimeAxis

/**
 * O gráfico de peso e medidas. Desenha duas séries sobrepostas: os valores registados a
 * traço fino e esbatido, e a tendência a cheio por cima. É essa distinção que impede a
 * pessoa de reagir a uma pesagem isolada.
 *
 * As etiquetas ficam fora do canvas e recebem a escala, para o texto ser desenhado pelo
 * sistema — com fonte, idioma e leitura por voz — em vez de pintado à mão.
 */
@Composable
fun AntaresChart(

    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,

    trend: List<Pair<Long, Double>> = emptyList(),

    // Linha tracejada do objetivo, quando existe.
    targetValue: Double? = null,
    height: Int = DEFAULT_HEIGHT_DP,
    pointColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trendColor: Color = MaterialTheme.colorScheme.primary,
    targetColor: Color = MaterialTheme.colorScheme.tertiary,
    gridColor: Color = MaterialTheme.colorScheme.outline,

    /**
     * O valor abaixo do qual a grandeza não existe, quando existe um.
     *
     * O gráfico do peso não passa nada: um peso nunca chega perto do zero, e a folga da
     * escala nunca o atravessa. Uma contagem chega — os treinos por semana começam em zero, e
     * o eixo escrevia «−0,5 treinos».
     */
    chaoDaEscala: Double? = null,

    labels: @Composable (ChartScale, TimeAxis) -> Unit = { _, _ -> },
) {
    if (points.isEmpty()) return
    val crus = points.sortedBy { it.first }
    val linha = trend.sortedBy { it.first }
    // A escala considera as duas séries e o objetivo ao mesmo tempo: calculada só sobre os
    // pontos, a linha do objetivo podia cair fora do gráfico.
    val scale = ChartScale.of(
        crus.map { it.second } + linha.map { it.second } + listOfNotNull(targetValue),
        chao = chaoDaEscala,
    )
    val eixo = TimeAxis.of(crus.map { it.first } + linha.map { it.first }) ?: return

    // O gráfico do peso é o ecrã inteiro para quem o abre, e não tinha nada que o
    // anunciasse. Os rótulos por baixo dizem as datas; falta dizer o que a linha faz.
    val descricao = stringResource(
        Res.string.chart_cd,
        crus.size,
        fmtG(scale.min),
        fmtG(scale.max),
    )

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height.dp)
                .semantics { contentDescription = descricao },
        ) {
            // O eixo vertical é invertido: no canvas o zero é em cima, e sem esta
            // subtração o gráfico saía de cabeça para baixo.
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

            // A tendência desenha-se depois dos pontos crus e mais grossa, para ficar por
            // cima: é a linha que a pessoa deve ler.
            if (linha.isNotEmpty()) {
                drawSeries(linha, ::x, ::y, trendColor, strokeWidth = 5f)
                // Um ponto no fim marca onde está hoje, que é o que se procura primeiro.
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

    // Um ponto só não faz linha nenhuma: desenha-se um círculo, ou o gráfico ficava vazio
    // para quem tem uma única pesagem.
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
