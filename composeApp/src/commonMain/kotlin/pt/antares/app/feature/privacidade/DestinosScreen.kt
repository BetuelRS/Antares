package pt.antares.app.feature.privacidade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScreen
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.SectionHeader
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.outgoing_footer
import pt.antares.app.generated.resources.outgoing_intro
import pt.antares.app.generated.resources.outgoing_section_device
import pt.antares.app.generated.resources.outgoing_section_network
import pt.antares.app.generated.resources.outgoing_title
import pt.antares.app.generated.resources.outgoing_what_label
import pt.antares.app.generated.resources.outgoing_when_label

/**
 * O ecrã que responde de uma vez à pergunta que a app nunca respondia: o que é que sai
 * daqui, e quando.
 *
 * Havia cinco destinos escritos na documentação do repositório e zero dentro da app. Quem
 * usa a app não lê o repositório.
 */
@Composable
fun DestinosScreen(onBack: () -> Unit) {
    AntaresScreen(
        topBar = { AntaresTopBar(title = stringResource(Res.string.outgoing_title), onBack = onBack) },
        espaco = Spacing.md,
        margem = PaddingValues(Spacing.lg),
    ) {
        Text(
            stringResource(Res.string.outgoing_intro),
            style = MaterialTheme.typography.bodyMedium,
        )

        SectionHeader(title = stringResource(Res.string.outgoing_section_network))
        DESTINOS_DE_REDE.forEach { CartaoDoDestino(it) }

        SectionHeader(title = stringResource(Res.string.outgoing_section_device))
        DESTINOS_NO_APARELHO.forEach { CartaoDoDestino(it) }

        Text(
            stringResource(Res.string.outgoing_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun CartaoDoDestino(destino: Destino) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                stringResource(destino.titulo),
                style = MaterialTheme.typography.titleMedium,
            )
            LinhaComRotulo(Res.string.outgoing_what_label, destino.oQueVai)
            LinhaComRotulo(Res.string.outgoing_when_label, destino.quando)
        }
    }
}

/**
 * O rótulo à esquerda, com largura fixa, para as duas linhas de cada cartão ficarem
 * alinhadas entre si e entre cartões — é o alinhamento que faz a lista ler-se como tabela
 * sem ser uma tabela, que num telemóvel estreito não caberia.
 */
@Composable
private fun LinhaComRotulo(rotulo: StringResource, valor: StringResource) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            stringResource(rotulo),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(LARGURA_DO_ROTULO),
        )
        Text(
            stringResource(valor),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

// Cabe «O que vai» e «Quando» sem partir a palavra, e deixa o resto da linha para o texto.
private val LARGURA_DO_ROTULO = 76.dp
