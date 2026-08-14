package pt.antares.app.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.antares.app.core.designsystem.AntaresMotion
import pt.antares.app.core.designsystem.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Um macro no anel. Traz os textos já formatados e traduzidos: o desenho não sabe formatar
 * números nem escolher idioma.
 */
data class MacroArc(
    val value: Float,
    val goal: Float,
    val color: Color,
    val initial: String,
    val consumedLabel: String,
    val goalLabel: String,
)

// Arco aberto em baixo, e não círculo fechado: a abertura é onde entram as etiquetas, e um
// anel completo não tem princípio nem fim visíveis para se ler o progresso.
private const val START_ANGLE = 130f
private const val SWEEP_TOTAL = 280f

@Composable
fun AntaresRing(
    centerValue: String,
    centerLabel: String,
    protein: MacroArc,
    carbs: MacroArc,
    fat: MacroArc,
    modifier: Modifier = Modifier,
    centerColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 216.dp,
    strokeWidth: Dp = 8.dp,
    gap: Dp = 10.dp,
    coreColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val reduce = LocalReduceMotion.current
    val arcs = listOf(protein, carbs, fat)

    val anim = remember { arcs.map { Animatable(0f) } }
    LaunchedEffect(protein.value, protein.goal, carbs.value, carbs.goal, fat.value, fat.goal, reduce) {
        arcs.forEachIndexed { i, arc ->
            val target = if (arc.goal > 0f) (arc.value / arc.goal).coerceIn(0f, 1f) else 0f
            if (reduce) {
                anim[i].snapTo(target)
            } else {
                anim[i].animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = AntaresMotion.RingFillMs,
                        delayMillis = i * AntaresMotion.RingStaggerMs,
                        easing = AntaresMotion.Orbit,
                    ),
                )
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size)) {
                val strokePx = strokeWidth.toPx()
                val gapPx = gap.toPx()
                val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
                val center = Offset(this.size.width / 2f, this.size.height / 2f)

                val innerInset = strokePx / 2f + 2 * (strokePx + gapPx)
                val innerRadius = this.size.width / 2f - innerInset
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(coreColor.copy(alpha = 0.07f), Color.Transparent),
                        center = center,
                        radius = innerRadius * 0.85f,
                    ),
                    radius = innerRadius * 0.85f,
                    center = center,
                )

                arcs.forEachIndexed { i, arc ->
                    val inset = strokePx / 2f + i * (strokePx + gapPx)
                    val topLeft = Offset(inset, inset)
                    val arcSize = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
                    val radius = (this.size.width - 2 * inset) / 2f
                    val progress = anim[i].value
                    val over = if (arc.goal > 0f) (arc.value / arc.goal) else 0f

                    val hot = if (over > 1f) lerp(arc.color, Color.White, (over - 1f).coerceAtMost(0.35f)) else arc.color

                    drawArc(
                        color = arc.color.copy(alpha = 0.16f),
                        startAngle = START_ANGLE,
                        sweepAngle = SWEEP_TOTAL,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )

                    if (progress > 0f) {
                        val sweep = SWEEP_TOTAL * progress
                        val frac = (sweep / 360f).coerceIn(0.001f, 1f)
                        val brush = Brush.sweepGradient(
                            0f to hot.copy(alpha = 0.72f),
                            frac to hot,
                            1f to hot,
                            center = center,
                        )
                        rotate(degrees = START_ANGLE, pivot = center) {
                            drawArc(
                                brush = brush,
                                startAngle = 0f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = stroke,
                            )
                        }
                        drawLeadingPoint(center, radius, progress, hot, strokePx)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerValue,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1).sp,
                        color = centerColor,
                        textAlign = TextAlign.Center,
                    ),
                )
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MacroLegend(protein)
            MacroLegend(carbs)
            MacroLegend(fat)
        }
    }
}

private fun DrawScope.drawLeadingPoint(center: Offset, radius: Float, progress: Float, color: Color, strokePx: Float) {
    val theta = (START_ANGLE + SWEEP_TOTAL * progress) * (PI.toFloat() / 180f)
    val p = Offset(center.x + radius * cos(theta), center.y + radius * sin(theta))
    drawCircle(color = color.copy(alpha = 0.22f), radius = strokePx * 0.78f, center = p)
    drawCircle(color = lerp(color, Color.White, 0.75f), radius = strokePx * 0.28f, center = p)
}

@Composable
private fun MacroLegend(arc: MacroArc) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            arc.initial,
            style = MaterialTheme.typography.labelMedium,
            color = arc.color,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${arc.consumedLabel}/${arc.goalLabel}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
        )
    }
}
