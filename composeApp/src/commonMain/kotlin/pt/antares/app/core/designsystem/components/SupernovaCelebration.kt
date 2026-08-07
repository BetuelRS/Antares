package pt.antares.app.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import pt.antares.app.core.designsystem.AntaresColors
import pt.antares.app.core.designsystem.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SupernovaCelebration(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val burst = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduce) {
            delay(2600)
            onDismiss()
        } else {
            burst.animateTo(1f, animationSpec = tween(durationMillis = 1500, easing = pt.antares.app.core.designsystem.AntaresMotion.Orbit))
            delay(1300)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC05050A))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            StarField(modifier = Modifier.fillMaxSize(), starCount = 70, seed = 7L)

            if (!reduce) {
                val t = burst.value
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height * 0.42f)
                    val maxR = size.minDimension * 0.55f

                    val flashR = maxR * (0.15f + 0.85f * t)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (1f - t) * 0.9f),
                                AntaresColors.secondaryDark.copy(alpha = (1f - t) * 0.7f),
                                AntaresColors.primaryDark.copy(alpha = (1f - t) * 0.4f),
                                Color.Transparent,
                            ),
                            center = c,
                            radius = flashR,
                        ),
                        radius = flashR,
                        center = c,
                    )

                    val ringR = maxR * t
                    drawCircle(
                        color = AntaresColors.secondaryDark.copy(alpha = (1f - t) * 0.8f),
                        radius = ringR,
                        center = c,
                        style = Stroke(width = (6f * (1f - t)).coerceAtLeast(1f)),
                    )

                    val rays = 14
                    for (i in 0 until rays) {
                        val ang = (i.toFloat() / rays) * 2f * PI.toFloat()
                        val inner = maxR * 0.12f * (1f + t)
                        val outer = inner + maxR * 0.9f * t
                        val col = if (i % 2 == 0) AntaresColors.primaryDark else AntaresColors.secondaryDark
                        drawLine(
                            color = col.copy(alpha = (1f - t) * 0.9f),
                            start = Offset(c.x + inner * cos(ang), c.y + inner * sin(ang)),
                            end = Offset(c.x + outer * cos(ang), c.y + outer * sin(ang)),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AntaresColors.secondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
