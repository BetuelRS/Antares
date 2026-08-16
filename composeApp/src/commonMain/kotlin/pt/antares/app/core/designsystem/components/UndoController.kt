package pt.antares.app.core.designsystem.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.undo_action
import pt.antares.app.generated.resources.undo_deleted

/**
 * O aviso com anulação, um só para a app inteira.
 *
 * Não havia desfazer em lado nenhum: apagar uma pesagem, um registo ou um treino era
 * definitivo à primeira, sem confirmação e sem volta. E a base **apaga por marcação** — o
 * dado continuava lá, só que inalcançável.
 *
 * É um controlador e não um componente por ecrã porque a alternativa era um `SnackbarHost`
 * em cada um dos catorze sítios que apagam, e o décimo quarto ficaria por fazer.
 */
class UndoController internal constructor(
    private val host: SnackbarHostState,
    private val scope: CoroutineScope,
) {

    /**
     * Mostra o aviso. [onUndo] corre só se a pessoa tocar em desfazer — deixar passar é a
     * confirmação, e é por isso que não há diálogo a perguntar antes.
     */
    fun show(mensagem: String, rotuloDesfazer: String, onUndo: () -> Unit) {
        scope.launch {
            // Substitui o aviso anterior em vez de os empilhar: apagar três linhas seguidas
            // dava três avisos, e o primeiro a desaparecer levava a atenção com ele.
            host.currentSnackbarData?.dismiss()
            val resultado = host.showSnackbar(
                message = mensagem,
                actionLabel = rotuloDesfazer,
                withDismissAction = false,
                duration = SnackbarDuration.Short,
            )
            if (resultado == SnackbarResult.ActionPerformed) onUndo()
        }
    }
}

/**
 * O controlador do ecrã. Fora da árvore que o instala devolve um que não faz nada: um ecrã
 * de teste sem `MainScaffold` à volta não pode rebentar por causa de um aviso.
 */
val LocalUndo = staticCompositionLocalOf<UndoController?> { null }

@Composable
fun rememberUndoController(host: SnackbarHostState): UndoController {
    val scope = rememberCoroutineScope()
    return remember(host, scope) { UndoController(host, scope) }
}

/**
 * Apagar com direito a desfazer, para os catorze sítios que apagam usarem a mesma frase e o
 * mesmo gesto.
 *
 * Devolve uma função em vez de ser um componente: quem apaga é um botão dentro de uma lista,
 * e um componente obrigaria cada lista a arranjar sítio para ele.
 */
@Composable
fun rememberApagarComDesfazer(): (apagar: () -> Unit, desfazer: () -> Unit) -> Unit {
    val undo = LocalUndo.current
    val mensagem = stringResource(Res.string.undo_deleted)
    val rotulo = stringResource(Res.string.undo_action)
    return remember(undo, mensagem, rotulo) {
        { apagar, desfazer ->
            apagar()
            // Sem controlador — num teste, ou fora da árvore da app — apaga na mesma. O
            // aviso é o extra; perder o apagamento por falta dele seria pior.
            undo?.show(mensagem, rotulo, desfazer)
        }
    }
}
