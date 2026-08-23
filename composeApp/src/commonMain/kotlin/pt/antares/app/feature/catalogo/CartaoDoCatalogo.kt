package pt.antares.app.feature.catalogo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.settings_catalogo
import pt.antares.app.generated.resources.settings_catalogo_a_descarregar
import pt.antares.app.generated.resources.settings_catalogo_a_ver
import pt.antares.app.generated.resources.settings_catalogo_desc
import pt.antares.app.generated.resources.settings_catalogo_descarregar
import pt.antares.app.generated.resources.settings_catalogo_em_dia
import pt.antares.app.generated.resources.settings_catalogo_ilegivel
import pt.antares.app.generated.resources.settings_catalogo_instalada
import pt.antares.app.generated.resources.settings_catalogo_instalado
import pt.antares.app.generated.resources.settings_catalogo_nao_avanca
import pt.antares.app.generated.resources.settings_catalogo_nao_se_guardou
import pt.antares.app.generated.resources.settings_catalogo_novidade
import pt.antares.app.generated.resources.settings_catalogo_procurar
import pt.antares.app.generated.resources.settings_catalogo_resumo_nao_bate
import pt.antares.app.generated.resources.settings_catalogo_sem_resposta

private val TAMANHO_DA_RODA = 18.dp

/**
 * O cartão do catálogo, nas definições.
 *
 * Diz sempre a versão que está instalada, mesmo antes de alguém procurar — é a única coisa
 * que se sabe sem ir à rede, e é o que torna o resto legível.
 *
 * **Cada recusa diz o que aconteceu ao catálogo que já lá estava.** Um ecrã que diz «erro»
 * deixa quem o lê sem saber se perdeu os alimentos que tinha, e a resposta é sempre a
 * mesma: não perdeu.
 */
@Composable
fun CartaoDoCatalogo(viewModel: CatalogoViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()

    SectionHeader(title = stringResource(Res.string.settings_catalogo))
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(Res.string.settings_catalogo_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.settings_catalogo_instalada, estado.instalada),
                style = MaterialTheme.typography.bodyMedium,
            )

            estado.novidade?.let { manifesto ->
                Text(
                    stringResource(
                        Res.string.settings_catalogo_novidade,
                        manifesto.versao,
                        manifesto.alimentos,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            estado.recado?.let { recado ->
                Text(
                    frase(recado, estado),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (estado.ocupado) {
                Text(
                    stringResource(
                        if (estado.aDescarregar) {
                            Res.string.settings_catalogo_a_descarregar
                        } else {
                            Res.string.settings_catalogo_a_ver
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                CircularProgressIndicator(Modifier.size(TAMANHO_DA_RODA))
            } else if (estado.novidade == null) {
                OutlinedButton(onClick = viewModel::procurar) {
                    Text(stringResource(Res.string.settings_catalogo_procurar))
                }
            } else {
                OutlinedButton(onClick = viewModel::descarregar) {
                    Text(stringResource(Res.string.settings_catalogo_descarregar))
                }
            }
        }
    }
}

@Composable
private fun frase(recado: RecadoDoCatalogo, estado: EstadoDoCatalogo): String = when (recado) {
    RecadoDoCatalogo.EM_DIA -> stringResource(Res.string.settings_catalogo_em_dia)
    RecadoDoCatalogo.INSTALADO -> stringResource(
        Res.string.settings_catalogo_instalado,
        estado.instalada,
        estado.alimentosInstalados,
    )
    RecadoDoCatalogo.SEM_RESPOSTA -> stringResource(Res.string.settings_catalogo_sem_resposta)
    RecadoDoCatalogo.RESUMO_NAO_BATE ->
        stringResource(Res.string.settings_catalogo_resumo_nao_bate)
    RecadoDoCatalogo.FICHEIRO_ILEGIVEL -> stringResource(Res.string.settings_catalogo_ilegivel)
    RecadoDoCatalogo.NAO_AVANCA -> stringResource(Res.string.settings_catalogo_nao_avanca)
    RecadoDoCatalogo.NAO_SE_GUARDOU ->
        stringResource(Res.string.settings_catalogo_nao_se_guardou)
}
