package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.antares.app.core.designsystem.Spacing

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    starfield: Boolean = true,
    action: @Composable (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // O campo de estrelas por trás do vazio é decoração e desliga-se: dentro de uma
        // lista curta ou de um cartão pequeno, ocupa mais atenção do que a mensagem.
        if (starfield) {
            StarField(modifier = Modifier.fillMaxSize())

            val glow = MaterialTheme.colorScheme.primary
            // Decoração pura: o brilho por trás do estado vazio não acrescenta nada à
            // frase que está por cima dele.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension * 0.42f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.14f), androidx.compose.ui.graphics.Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    // Decorativo: a frase por baixo diz o que falta, e o ícone só lhe dá
                    // forma. Anunciá-lo era ler duas vezes a mesma ausência.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(48.dp).padding(bottom = Spacing.sm),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            action?.invoke()
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}
