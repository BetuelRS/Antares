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
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun MacroBar(
    label: String,
    grams: Double,
    targetGrams: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // A barra trava na meta, mas os números por cima dela não: é aí que se vê o excesso.
    val progress = if (targetGrams > 0) (grams / targetGrams).toFloat().coerceIn(0f, 1f) else 0f
    Column(modifier = modifier) {
        SplitRow(
            leading = { Text(label, style = MaterialTheme.typography.labelSmall) },
            trailing = {
                Text(
                    "${grams.roundToInt()} / ${targetGrams} g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(4.dp)),
            )
        }
    }
}
