package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import pt.antares.app.core.designsystem.Spacing

@Composable
fun AntaresCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),

    // O clique e o papel entraram na 2.3.0. Sem eles, vinte ficheiros usavam o `Card` do
    // Material só para poderem ser tocados — e levavam com eles cantos, elevação e
    // espaçamento a diferir de ecrã para ecrã sem ninguém ter decidido isso.
    onClick: (() -> Unit)? = null,

    // Nulo quando não há clique. Um cartão tocável sem papel é indistinguível de um que não
    // se toca para quem usa leitor de ecrã, e o `AccessibilityTest` chumba por isso.
    role: Role? = if (onClick != null) Role.Button else null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val comAccao = if (onClick != null) {
        modifier.clickable(role = role, onClick = onClick)
    } else {
        modifier
    }
    Card(modifier = comAccao) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/**
 * Um cartão para o que **ainda não aconteceu**: sugestões, propostas, o que a app oferece.
 *
 * Tracejado e sem fundo, ao contrário do [AntaresCard], que é sólido e é para o que já foi
 * registado. A diferença é a única coisa que separa uma sugestão de um facto quando as duas
 * estão na mesma lista — e no diário estavam com a mesma forma, o que fazia um dia vazio
 * parecer um dia cheio.
 */
@Composable
fun AntaresGhostCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val cor = MaterialTheme.colorScheme.outline
    val forma = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .drawBehind {
                drawOutline(
                    outline = forma.createOutline(size, layoutDirection, this),
                    color = cor,
                    style = Stroke(
                        width = GHOST_STROKE_DP.dp.toPx(),
                        // O traço e o intervalo em pontos do ecrã, não em pixels: num ecrã
                        // denso um traço de doze pixels era quase contínuo.
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(GHOST_DASH_DP.dp.toPx(), GHOST_GAP_DP.dp.toPx()),
                        ),
                    ),
                )
            }
            .padding(contentPadding),
        content = content,
    )
}

private const val GHOST_STROKE_DP = 1
private const val GHOST_DASH_DP = 6
private const val GHOST_GAP_DP = 4

@Composable
fun SplitRow(
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f, fill = false)) { leading() }
        Spacer(Modifier.width(Spacing.md))
        Box(Modifier.weight(1f, fill = false)) { trailing() }
    }
}
