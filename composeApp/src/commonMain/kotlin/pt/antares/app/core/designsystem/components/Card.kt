package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
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
 * O cartão que responde à pergunta com que se abriu a app.
 *
 * É o `.card.hero` dos esboços, e a única superfície da app com gradiente: um véu da cor
 * primária a esvair-se para o fundo do cartão, com a linha de contorno da mesma cor. O resto
 * do sistema é plano de propósito — **um destaque que aparece três vezes no mesmo ecrã deixa
 * de destacar** —, e por isso este componente existe em vez de um parâmetro no [AntaresCard]:
 * quem o usar tem de o escolher, e escolhe-o uma vez por ecrã.
 *
 * O véu é fraco por medida: sete por cento sobre a superfície. A cor aqui não é estado nem
 * categoria — é hierarquia —, e um gradiente que se lê como aviso mentiria sobre o que está
 * lá dentro.
 */
@Composable
fun AntaresHeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val primaria = MaterialTheme.colorScheme.primary
    val superficie = MaterialTheme.colorScheme.surface
    val forma = MaterialTheme.shapes.medium

    Column(
        modifier = modifier
            .clip(forma)
            .background(
                Brush.verticalGradient(
                    listOf(primaria.copy(alpha = VEU_ALPHA).compositeOver(superficie), superficie),
                ),
            )
            .border(HERO_STROKE_DP.dp, primaria.copy(alpha = CONTORNO_ALPHA), forma)
            .padding(contentPadding),
        content = content,
    )
}

private const val VEU_ALPHA = 0.07f
private const val CONTORNO_ALPHA = 0.35f
private const val HERO_STROKE_DP = 1

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
