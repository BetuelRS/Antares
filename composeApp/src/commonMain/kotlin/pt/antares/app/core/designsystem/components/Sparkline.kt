package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun Sparkline(
    primary: List<Double>,
    modifier: Modifier = Modifier,
    secondary: List<Double> = emptyList(),
    primaryColor: Color = MaterialTheme.colorScheme.outline,
    secondaryColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier) {
        val all = primary + secondary
        if (primary.size < 2) return@Canvas
        val min = all.min()
        val max = all.max()

        // As duas séries partilham a escala, calculada sobre as duas juntas: com escalas
        // separadas, uma linha plana e uma a subir apareciam iguais.
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0

        fun buildPath(points: List<Double>): Path {
            val path = Path()
            val stepX = size.width / (points.size - 1).coerceAtLeast(1)
            points.forEachIndexed { i, v ->
                val x = i * stepX

                val norm = ((v - min) / span).toFloat()
                // Deixa 8% de margem em cima e em baixo: com o traço grosso e as pontas
                // redondas, os extremos ficavam cortados contra a borda.
                val y = size.height * (0.92f - norm * 0.84f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        // O preenchimento fecha a linha contra o fundo do desenho e leva um gradiente que
        // se desvanece: dá volume sem competir com o traço.
        val primaryPath = buildPath(primary)
        val fillPath = Path().apply {
            addPath(primaryPath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.18f), Color.Transparent),
            ),
        )
        drawPath(
            path = primaryPath,
            color = primaryColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )

        // A segunda série é mais grossa e vai por cima, como no [AntaresChart]: é sempre a
        // tendência, e é a que se deve ler primeiro.
        if (secondary.size >= 2) {
            drawPath(
                path = buildPath(secondary),
                color = secondaryColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round),
            )
        }
    }
}
