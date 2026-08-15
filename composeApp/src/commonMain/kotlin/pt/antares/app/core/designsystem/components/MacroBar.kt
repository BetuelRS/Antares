package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Uma barra de macro. **A cor é sempre a do macro**, tenha-se comido pouco ou muito.
 *
 * A cor aqui é um código de categoria — proteína, hidratos, gordura — e não um estado. Antes
 * a barra travava na meta e passar dela não se via em lado nenhum a não ser no número; e como
 * a cor da proteína é avermelhada, uma barra cheia de proteína lia-se como um aviso ao lado
 * de duas barras que se liam como categorias. Duas convenções na mesma linha.
 *
 * O excesso passa a ver-se pela **forma**: a barra enche, a marca da meta fica onde ela está,
 * e o que passa dela aparece na mesma cor mais apagada. Nada muda de cor por estar alto.
 */
@Composable
fun MacroBar(
    label: String,
    grams: Double,
    targetGrams: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val ratio = if (targetGrams > 0) (grams / targetGrams).toFloat() else 0f
    val passou = ratio > 1f

    // Acima da meta a barra representa o **consumido**, e a meta passa a ser uma fração dela.
    // Abaixo, representa a meta. É o que mantém a marca da meta no sítio certo nos dois casos.
    val ateAMeta = if (passou) (1f / ratio) else ratio.coerceIn(0f, 1f)

    Column(modifier = modifier) {
        SplitRow(
            leading = { Text(label, style = MaterialTheme.typography.labelSmall) },
            trailing = {
                Text(
                    "${grams.roundToInt()} / $targetGrams g",
                    style = MaterialTheme.typography.labelSmall,
                    // Negrito e não vermelho: chama a atenção sem dizer que é mau.
                    fontWeight = if (passou) FontWeight.Bold else null,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT_DP.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(BAR_RADIUS_DP.dp)),
        ) {
            if (passou) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(color.copy(alpha = EXCESS_ALPHA), RoundedCornerShape(BAR_RADIUS_DP.dp)),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(ateAMeta)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(BAR_RADIUS_DP.dp)),
            )
        }
    }
}

private const val BAR_HEIGHT_DP = 8
private const val BAR_RADIUS_DP = 4

// O excesso na mesma cor, mais apagado. Uma cor diferente voltava a ser um estado.
private const val EXCESS_ALPHA = 0.35f
