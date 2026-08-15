package pt.antares.app.feature.diary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*
import pt.antares.app.core.util.formatMinuteOfDay
import pt.antares.app.core.calc.Janela
import pt.antares.app.core.util.formatDurationMin

/**
 * O que o diário diz sobre o dia sem ser a comida: a janela alimentar e o aviso de se ter
 * comido com o jejum a decorrer.
 */
@Composable
internal fun JanelaAlimentarLinha(janela: Janela) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.diary_window),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(
                Res.string.diary_window_value,
                formatMinuteOfDay(janela.primeiraMin),
                formatMinuteOfDay(janela.ultimaMin),
                formatDurationMin(janela.duracaoMin),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(Res.string.diary_window_fasting, formatDurationMin(janela.jejumMin)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (janela.semHora > 0) {
            Text(
                stringResource(Res.string.diary_window_partial, janela.semHora),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Diz que se comeu com o jejum a correr.
 *
 * Não julga e não propõe nada: o contador do jejum continua a subir, e a app limita-se a
 * pôr as duas coisas na mesma frase para a pessoa decidir. Quem quiser terminar o jejum
 * tem o botão no ecrã dele — repeti-lo aqui era transformar um facto numa ordem.
 */
@Composable
internal fun QuebraDoJejumCartao(quebra: QuebraDoJejum) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.diary_fast_clash_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(
                Res.string.diary_fast_clash_body,
                formatMinuteOfDay(quebra.inicioMin),
                quebra.registos,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// O registo rápido à espera de saber a que refeição pertence. Guarda o pedido inteiro para
// a resposta não obrigar a pessoa a escrever tudo outra vez.
