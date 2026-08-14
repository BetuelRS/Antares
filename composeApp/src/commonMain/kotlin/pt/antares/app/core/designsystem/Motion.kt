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

// Lido da definição de acessibilidade do sistema. Todas as animações deste ficheiro o
// respeitam: quem pediu menos movimento vê o ecrã já montado, sem transição nenhuma.
val LocalReduceMotion = staticCompositionLocalOf { false }

object AntaresMotion {

    // Curva que arranca depressa e desacelera muito no fim. Dá a sensação de peso que trava
    // sozinho, em vez do movimento uniforme que se lê como mecânico.
    val Orbit: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    const val RingFillMs = 900

    const val RingStaggerMs = 110

    const val CardEnterMs = 420

    const val CardStaggerMs = 70
}

/**
 * Faz os cartões de uma lista aparecerem em cascata, um a seguir ao outro. O `index` é a
 * posição na lista e é o que dá o atraso de cada um.
 */
@Composable
fun Modifier.cascadeIn(index: Int): Modifier {
    // Sai antes de criar estado nenhum: com movimento reduzido isto tem de ser um
    // modificador vazio, e não uma animação instantânea.
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
    // Anima na camada gráfica e não no esquema: opacidade e deslocação assim não obrigam a
    // remedir nem a reposicionar a lista a cada quadro.
    return this.graphicsLayer { this.alpha = alpha; translationY = translateY }
}
