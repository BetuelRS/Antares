package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import pt.antares.app.core.calc.BodyComposition
import pt.antares.app.core.designsystem.success

/**
 * O IMC numa régua de zonas, em vez de um número solto. Mostra onde a pessoa está e
 * quanto falta para a zona seguinte — sozinho, um número como 27,4 não diz nada.
 */
@Composable
fun BmiScale(
    bmi: Double,
    modifier: Modifier = Modifier,
) {

    // As cores são lidas fora do canvas porque `MaterialTheme` é composição e o corpo de um
    // `Canvas` já não o é.
    val caution = MaterialTheme.colorScheme.secondary
    val healthy = MaterialTheme.success
    val obese = MaterialTheme.colorScheme.error
    val marker = MaterialTheme.colorScheme.onSurface
    val trackAlpha = 0.55f

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(TRACK_HEIGHT_DP.dp),
        ) {
            val w = size.width
            val h = size.height
            fun x(value: Double): Float =
                (((value - MIN_BMI) / (MAX_BMI - MIN_BMI)).coerceIn(0.0, 1.0) * w).toFloat()

            val zones = listOf(
                Triple(MIN_BMI, BodyComposition.BMI_UNDERWEIGHT, caution),
                Triple(BodyComposition.BMI_UNDERWEIGHT, BodyComposition.BMI_OVERWEIGHT, healthy),
                Triple(BodyComposition.BMI_OVERWEIGHT, BodyComposition.BMI_OBESE, caution),
                Triple(BodyComposition.BMI_OBESE, MAX_BMI, obese),
            )
            for ((from, to, color) in zones) {
                drawRect(
                    color = color.copy(alpha = trackAlpha),
                    topLeft = Offset(x(from), 0f),
                    size = Size(x(to) - x(from), h),
                )
            }

            val mx = x(bmi)
            drawLine(
                color = marker,
                start = Offset(mx, 0f),
                end = Offset(mx, h),
                strokeWidth = MARKER_WIDTH_PX,
            )
            drawRect(
                color = marker,
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                style = Stroke(width = 1f),
            )
        }
    }
}

private const val MIN_BMI = 15.0
private const val MAX_BMI = 40.0
private const val TRACK_HEIGHT_DP = 12
private const val MARKER_WIDTH_PX = 4f
