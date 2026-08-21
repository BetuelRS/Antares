package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import pt.antares.app.core.designsystem.Spacing

/**
 * Uma linha de lista: ícone opcional, título, subtítulo opcional, alguma coisa à direita, e
 * uma acção opcional.
 *
 * Nasceu de duas duplicações que eram cópias exactas uma da outra, comentário incluído — o
 * `MenuItem` e o `MeItem`, o `ToggleRow` e o `SettingSwitchRow`. Duas linhas iguais escritas
 * em dois ficheiros divergem no dia em que alguém corrige uma só, e a partir daí a app tem
 * duas maneiras de mostrar a mesma coisa sem que ninguém tenha decidido isso.
 *
 * **Não substitui as linhas de domínio.** Uma linha do diário com deslizar para apagar, uma
 * de peso com a diferença para a anterior, uma de série com repetições e carga — essas têm
 * afordâncias que um componente geral não tem como oferecer sem se tornar um formulário de
 * vinte parâmetros. Foram vinte e quatro composables de linha contados, e só quatro eram
 * mesmo repetição.
 */
@Composable
fun LinhaDaLista(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    icone: ImageVector? = null,

    // O que fica à direita: um valor, um interruptor, uma seta. Entra como conteúdo e não
    // como texto porque metade dos casos não é texto nenhum.
    aoLado: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,

    // Em cartão quando a linha é um destino, sem cartão quando é uma linha dentro de um.
    // Aninhar cartões dá duas sombras e uma margem que ninguém pediu.
    emCartao: Boolean = true,
) {
    val corpo: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Decorativo e sem descrição de propósito: o nome da linha está escrito ao lado,
            // e um leitor de ecrã que anuncie os dois lê tudo a dobrar.
            if (icone != null) Icon(icone, contentDescription = null)

            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyLarge)
                if (subtitulo != null) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            aoLado?.invoke()
        }
    }

    if (emCartao) {
        AntaresCard(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            // O papel diz ao leitor de ecrã que isto se toca. Sem ele, o `AccessibilityTest`
            // chumba — e com razão: um cartão tocável indistinguível de um cartão que não se
            // toca é um cartão que ninguém toca.
            role = if (onClick != null) Role.Button else null,
        ) { corpo() }
    } else {
        Row(modifier = modifier.fillMaxWidth()) { corpo() }
    }
}
