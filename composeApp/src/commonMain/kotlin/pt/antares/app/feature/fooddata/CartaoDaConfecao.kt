package pt.antares.app.feature.fooddata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.confecao_com_molho
import pt.antares.app.generated.resources.confecao_cru
import pt.antares.app.generated.resources.confecao_de_outra_carne
import pt.antares.app.generated.resources.confecao_explicacao
import pt.antares.app.generated.resources.confecao_falta_peso
import pt.antares.app.generated.resources.confecao_peso_cozinhado
import pt.antares.app.generated.resources.confecao_pesa
import pt.antares.app.generated.resources.confecao_rendimento
import pt.antares.app.generated.resources.confecao_sem_molho
import pt.antares.app.generated.resources.confecao_titulo
import kotlin.math.roundToInt

private const val PERCENTAGEM = 100.0

/**
 * «E se for cozinhado?»
 *
 * Escolher um método muda **o que se vê e o que se grava**, e não só o que se vê: o diário
 * copia a nutrição no momento do registo, e a cópia tem de ser a que esteve no ecrã.
 *
 * O cartão não aparece a metade do catálogo, e é de propósito — um pão já foi ao forno, um
 * gelado não vai, um prato composto é comida feita. Um alimento sem família de confeção não
 * tem nada a oferecer aqui, e é melhor não aparecer do que aparecer vazio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CartaoDaConfecao(
    state: PortionState,
    onMetodo: (String?) -> Unit,
    onPesoCozinhado: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metodos = state.metodosPossiveis
    if (metodos.isEmpty()) return

    val linha = state.metodo?.let { state.tabelaDeConfecao.linha(state.food?.familia, it) }

    SectionHeader(title = stringResource(Res.string.confecao_titulo))
    AntaresCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(Res.string.confecao_explicacao),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = state.metodo == null,
                    onClick = { onMetodo(null) },
                    label = { Text(stringResource(Res.string.confecao_cru)) },
                )
                for (m in metodos) {
                    FilterChip(
                        selected = state.metodo == m.id,
                        onClick = { onMetodo(m.id) },
                        label = { Text(nomeDoMetodo(m.id, m.nome)) },
                    )
                }
            }

            if (linha != null) Explicacao(state, linha.rendimento, linha.comMolho, linha.rendimentoDeOutraCarne)

            // O campo só aparece com um método escolhido: sem isso é uma pergunta sem
            // resposta possível, e um campo que não faz nada é pior do que não haver campo.
            if (state.metodo != null) {
                OutlinedTextField(
                    value = state.gramasCozinhadasText,
                    onValueChange = onPesoCozinhado,
                    label = { Text(stringResource(Res.string.confecao_peso_cozinhado)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Explicacao(
    state: PortionState,
    rendimento: Double?,
    comMolho: Boolean,
    deOutraCarne: Boolean,
) {
    val fraco = MaterialTheme.colorScheme.onSurfaceVariant

    if (state.faltaOPeso) {
        Text(
            stringResource(Res.string.confecao_falta_peso),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }

    rendimento?.let {
        Text(
            stringResource(Res.string.confecao_rendimento, (it * PERCENTAGEM).roundToInt().toString()),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (deOutraCarne) {
            Text(
                stringResource(Res.string.confecao_de_outra_carne),
                style = MaterialTheme.typography.bodySmall,
                color = fraco,
            )
        }
    }

    Text(
        stringResource(if (comMolho) Res.string.confecao_com_molho else Res.string.confecao_sem_molho),
        style = MaterialTheme.typography.bodySmall,
        color = fraco,
    )
    Text(
        stringResource(Res.string.confecao_pesa),
        style = MaterialTheme.typography.bodySmall,
        color = fraco,
    )
}
