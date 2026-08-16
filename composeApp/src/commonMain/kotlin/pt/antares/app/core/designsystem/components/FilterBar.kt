// A `FilterOption` mora aqui, com os dois componentes que a usam e mais ninguém. Um ficheiro
// só para uma linha dava uma importação a mais em cada ecrã e nada em troca.
@file:Suppress("MatchingDeclarationName")

package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.filter_all

/** Uma escolha de filtro: o valor e o que se lê. */
data class FilterOption<T>(val value: T, val label: String)

/**
 * Um filtro em menu, com «todos» sempre à cabeça.
 *
 * Os históricos de treino e de corrida não tinham forma nenhuma de filtrar: com duzentos
 * treinos gravados, chegar ao de fevereiro era percorrer duzentos cartões. Os dois usam este
 * mesmo componente porque a única diferença entre eles é o que está na lista.
 */
@Composable
fun <T> FilterDropdownChip(
    label: String,
    selected: T?,
    options: List<FilterOption<T>>,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    val escolhido = options.firstOrNull { it.value == selected }

    Box(modifier) {
        FilterChip(
            selected = selected != null,
            onClick = { aberto = true },
            label = { Text(escolhido?.label ?: label, maxLines = 1) },
            trailingIcon = {
                // Decorativo: a seta acompanha um chip que já diz o que filtra.
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
        )
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.filter_all)) },
                onClick = { onSelect(null); aberto = false },
            )
            options.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(opcao.label) },
                    onClick = { onSelect(opcao.value); aberto = false },
                )
            }
        }
    }
}

/** A linha onde os filtros vivem. Em linha corrida: dois filtros longos não cabem lado a lado. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        content()
    }
}
