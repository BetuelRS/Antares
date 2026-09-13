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
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import pt.antares.app.core.designsystem.Spacing

/** O fundo vermelho do gesto, para o teste saber se ele está desenhado. */
internal const val FUNDO_DO_GESTO = "fundo-do-gesto"

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
 * **O estado do gesto não é guardável, e é de propósito.** O `rememberSwipeToDismissBoxState`
 * guarda-o com `rememberSaveable`, e uma lista com chave guarda o estado de cada item mesmo
 * depois de ele sair. A linha reposta pelo desfazer tem a mesma chave, voltava **deslizada** —
 * fora do ecrã — e o efeito de baixo apagava-a outra vez: o desfazer de um deslizar nunca
 * funcionava. Perde-se o gesto a meio numa rotação, que não faz falta a ninguém.
 *
 * **O gesto não se prova por `adb`** — nenhuma das formas de o injetar reproduz um deslizar
 * a sério, é a mesma lição do `ListaArrastavel` — e por isso vive coberto por um teste de
 * interface que constrói o gesto passo a passo, um deles dentro de uma lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeslizarParaApagar(
    onApagar: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val densidade = LocalDensity.current
    val limiar = SwipeToDismissBoxDefaults.positionalThreshold
    val estado = remember(densidade) {
        SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, densidade, { true }, limiar)
    }

    LaunchedEffect(estado.currentValue) {
        if (estado.currentValue != SwipeToDismissBoxValue.Settled) onApagar()
    }

    SwipeToDismissBox(
        state = estado,
        modifier = modifier,
        backgroundContent = {
            // Só durante o gesto. Desenhado sempre, o vermelho aparecia nos cantos redondos do
            // cartão de cada registo, com a linha parada — visto no aparelho.
            if (estado.dismissDirection == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(FUNDO_DO_GESTO)
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
                    // Decorativo: o fundo está sempre composto por baixo da linha, e com
                    // descrição o leitor de ecrã anunciava um «Apagar» que não se toca em cada
                    // registo. Quem usa o leitor apaga pelo menu da linha, que chama o mesmo.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = { content() },
    )
}
