package pt.antares.app.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

val LocalReduceMotion = staticCompositionLocalOf { false }

object AntaresMotion {

    val Orbit: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    const val RingFillMs = 900

    const val RingStaggerMs = 110

    const val CardEnterMs = 420

    const val CardStaggerMs = 70
}

@Composable
fun Modifier.cascadeIn(index: Int): Modifier {
    if (LocalReduceMotion.current) return this
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.toLong() * AntaresMotion.CardStaggerMs)
        shown = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(AntaresMotion.CardEnterMs, easing = AntaresMotion.Orbit),
        label = "cascadeAlpha",
    )
    val translateY by animateFloatAsState(
        targetValue = if (shown) 0f else 28f,
        animationSpec = tween(AntaresMotion.CardEnterMs, easing = AntaresMotion.Orbit),
        label = "cascadeY",
    )
    return this.graphicsLayer { this.alpha = alpha; translationY = translateY }
}
