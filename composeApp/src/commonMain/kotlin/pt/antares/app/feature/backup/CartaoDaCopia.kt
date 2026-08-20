package pt.antares.app.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.privacy.AutoBackup
import pt.antares.app.core.privacy.EstadoDaCopia
import pt.antares.app.core.util.rememberPedidoDePastaDeCopias
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.copia_a_correr
import pt.antares.app.generated.resources.copia_agora
import pt.antares.app.generated.resources.copia_atrasada_hoje
import pt.antares.app.generated.resources.copia_automatica
import pt.antares.app.generated.resources.copia_dar_permissao
import pt.antares.app.generated.resources.copia_falhou
import pt.antares.app.generated.resources.copia_ha_dias
import pt.antares.app.generated.resources.copia_hoje
import pt.antares.app.generated.resources.copia_nunca
import pt.antares.app.generated.resources.copia_nunca_hoje
import pt.antares.app.generated.resources.copia_ontem
import pt.antares.app.generated.resources.copia_onde
import pt.antares.app.generated.resources.copia_sem_permissao
import pt.antares.app.generated.resources.copia_titulo

/**
 * O cartão que diz há quantos dias foi a última cópia.
 *
 * Existe porque uma cópia automática silenciosa é indistinguível de nenhuma cópia: quem não
 * a vê nunca sabe se está protegido, e só descobre no dia em que precisa dela.
 */
@Composable
fun CartaoDaCopia(
    modifier: Modifier = Modifier,
    viewModel: CopiaViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val estado = state.copia ?: return
    val pedirPermissao = rememberPedidoDePastaDeCopias { concedida ->
        if (concedida) viewModel.copiarAgora() else viewModel.recarregar()
    }

    AntaresCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                stringResource(Res.string.copia_titulo),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                quandoFoi(estado),
                style = MaterialTheme.typography.bodyMedium,
                color = if (estado.atrasada) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                stringResource(
                    Res.string.copia_onde,
                    estado.quantas.toString(),
                    estado.pasta,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(
                    Res.string.copia_automatica,
                    AutoBackup.DIAS_ENTRE_COPIAS.toString(),
                    AutoBackup.MAX_COPIAS.toString(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!estado.podeEscrever) {
                Text(
                    stringResource(Res.string.copia_sem_permissao),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.falhou) {
                Text(
                    stringResource(Res.string.copia_falhou),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Sem permissão o botão pede-a e só depois copia; pedir a permissão a partir do
            // botão de copiar seria a app a fazer duas coisas com um toque só.
            val semPermissao = !estado.podeEscrever && pedirPermissao != null
            SecondaryButton(
                text = when {
                    semPermissao -> stringResource(Res.string.copia_dar_permissao)
                    state.aCopiar -> stringResource(Res.string.copia_a_correr)
                    else -> stringResource(Res.string.copia_agora)
                },
                onClick = {
                    if (semPermissao) pedirPermissao?.invoke() else viewModel.copiarAgora()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.aCopiar,
            )
        }
    }
}

/**
 * O aviso curto do Hoje. Só aparece quando a cópia está atrasada: um cartão permanente a
 * dizer que está tudo bem é o tipo de coisa que se deixa de ler, e no dia em que passa a
 * dizer outra coisa já ninguém olha para ele.
 */
/**
 * O aviso curto do Hoje. Só aparece quando a cópia está atrasada: um cartão permanente a
 * dizer que está tudo bem é o tipo de coisa que se deixa de ler, e no dia em que passa a
 * dizer outra coisa já ninguém olha para ele.
 */
@Composable
fun AvisoDeCopiaAtrasada(
    modifier: Modifier = Modifier,
    viewModel: CopiaViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val estado = state.copia ?: return
    if (!estado.atrasada) return
    val pedirPermissao = rememberPedidoDePastaDeCopias { concedida ->
        if (concedida) viewModel.copiarAgora() else viewModel.recarregar()
    }
    val semPermissao = !estado.podeEscrever && pedirPermissao != null

    AntaresCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                if (estado.diasDesde == null) {
                    stringResource(Res.string.copia_nunca_hoje)
                } else {
                    stringResource(Res.string.copia_atrasada_hoje, estado.diasDesde.toString())
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            if (state.falhou) {
                Text(
                    stringResource(Res.string.copia_falhou),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            // Copia aqui em vez de levar ao ecrã da cópia: quem vê este cartão está a meio
            // de outra coisa, e mandá-lo a outro ecrã para carregar noutro botão é como o
            // aviso deixa de ser seguido.
            SecondaryButton(
                text = when {
                    semPermissao -> stringResource(Res.string.copia_dar_permissao)
                    state.aCopiar -> stringResource(Res.string.copia_a_correr)
                    else -> stringResource(Res.string.copia_agora)
                },
                onClick = {
                    if (semPermissao) pedirPermissao?.invoke() else viewModel.copiarAgora()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.aCopiar,
            )
        }
    }
}

@Composable
private fun quandoFoi(estado: EstadoDaCopia): String = when (estado.diasDesde) {
    null -> stringResource(Res.string.copia_nunca)
    0 -> stringResource(Res.string.copia_hoje)
    1 -> stringResource(Res.string.copia_ontem)
    else -> stringResource(Res.string.copia_ha_dias, estado.diasDesde.toString())
}
