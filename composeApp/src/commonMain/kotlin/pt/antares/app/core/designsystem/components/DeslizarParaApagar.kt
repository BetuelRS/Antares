package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_delete

/**
 * Uma linha que se apaga a deslizar, para qualquer um dos dois lados.
 *
 * Não substitui o menu de apagar — soma-se a ele. E o desfazer é de quem chama: o
 * `onApagar` é a mesma função que o menu já usa, para os dois gestos acabarem no mesmo
 * aviso de desfazer.
 *
 * **Reage ao `currentValue` assente, não ao `confirmValueChange`.** O `confirmValueChange`
 * corre durante o próprio gesto — a confirmar o alvo a cada âncora que se cruza — e chamava
 * o apagar duas vezes por um deslizar só. O assentar acontece uma vez por gesto completo.
 *
 * **O gesto não se prova por `adb`** — nenhuma das formas de o injetar reproduz um deslizar
 * a sério, é a mesma lição do `ListaArrastavel` — e por isso vive coberto por um teste de
 * interface que constrói o gesto passo a passo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeslizarParaApagar(
    onApagar: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val estado = rememberSwipeToDismissBoxState()

    LaunchedEffect(estado.currentValue) {
        if (estado.currentValue != SwipeToDismissBoxValue.Settled) onApagar()
    }

    SwipeToDismissBox(
        state = estado,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = Spacing.lg),
                contentAlignment = if (estado.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                },
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.common_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = { content() },
    )
}
