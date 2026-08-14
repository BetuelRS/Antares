package pt.antares.app.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val baseAlpha: Float,
    val gold: Boolean,
    val phase: Float,
)

/**
 * Fundo estrelado dos ecrãs vazios. Decoração e nada mais — não desenha dado nenhum.
 */
@Composable
fun StarField(
    modifier: Modifier = Modifier,
    starCount: Int = 42,
    twinkle: Boolean = true,
    // Semente fixa: as estrelas ficam no mesmo sítio a cada recomposição, e uma constelação
    // que salta a cada mudança de ecrã chamaria a atenção para si.
    seed: Long = 42L,
) {
    val stars = remember(starCount, seed) {
        val rng = Random(seed)
        List(starCount) {
            Star(
                xFrac = rng.nextFloat(),
                yFrac = rng.nextFloat(),
                radius = 0.6f + rng.nextFloat() * 1.8f,
                baseAlpha = 0.20f + rng.nextFloat() * 0.5f,
                gold = rng.nextFloat() < 0.30f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "starfield")
    val t by if (twinkle) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "twinkle",
        )
    } else {
        remember { androidx.compose.runtime.mutableStateOf(0f) }
    }

    val estrela = MaterialTheme.colorScheme.onSurface
    val gold = Color(0xFFFFB86B)

    Canvas(modifier = modifier) {
        stars.forEach { s ->

            val a = (s.baseAlpha * (0.65f + 0.35f * sin(t + s.phase))).coerceIn(0f, 1f)
            drawCircle(
                color = (if (s.gold) gold else estrela).copy(alpha = a),
                radius = s.radius,
                center = Offset(s.xFrac * size.width, s.yFrac * size.height),
            )
        }
    }
}
