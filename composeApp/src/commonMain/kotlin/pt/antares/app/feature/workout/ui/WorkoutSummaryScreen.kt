package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.abs
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.rememberCartaoPartilhavel
import pt.antares.app.core.util.formatDurationMin
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun WorkoutSummaryScreen(
    sessionId: String,
    onDone: () -> Unit,
    viewModel: WorkoutSummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    val cartao = rememberCartaoPartilhavel(stringResource(Res.string.workout_summary_share_filename))

    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                // O nome da rotina no título, como o esboço 10 desenha: quem acabou de treinar
                // sabe que treinou, e o que o ecrã lhe pode dizer é **o quê**.
                //
                // **Só o nome**, e não «Corpo inteiro A · terminado» como o esboço escreve:
                // a barra é de uma linha, e a 200 % de escala de letra o «· terminado» era o
                // que se perdia nas reticências — dizer-lhe que acabou, num ecrã a que só se
                // chega tocando em «Terminar», custava o nome da rotina.
                title = state.nomeDaRotina ?: stringResource(Res.string.workout_summary_title),
                actions = {
                    IconButton(onClick = cartao.partilhar) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(Res.string.workout_summary_share),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).larguraDeLeitura().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AntaresCard(modifier = Modifier.fillMaxWidth().then(cartao.modifier)) {
                // Os três números lado a lado, e a diferença **por baixo de cada um** — que é
                // como o esboço 10 os desenha. Empilhados, a linha de diferenças ficava
                // debaixo dos três e ninguém sabia qual pertencia a qual: visto a correr.
                val ultima = state.comparacao.ultimaVez
                if (ultima != null) {
                    Text(
                        stringResource(Res.string.workout_summary_vs_ultima),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // O mesmo formato do painel de treino e da linha do histórico. Estava
                    // aqui a terceira cópia do `%d min` sem conversão, e um treino esquecido
                    // aberto de um dia para o outro fechava com «2715 min» — visto a correr.
                    Stat(
                        label = stringResource(Res.string.workout_summary_duration),
                        value = formatDurationMin(state.durationMin),
                        diferenca = ultima?.let {
                            diferencaTexto(it.duracaoMin, formatDurationMin(abs(it.duracaoMin)))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Stat(
                        label = stringResource(Res.string.workout_summary_volume),
                        value = weightWithUnit(state.volume, unidades),
                        diferenca = ultima?.let {
                            // O sinal sai do valor e não do arredondamento: meio quilo de
                            // diferença mostra-se como «+0 kg», e escrever «=» era dizer que
                            // não mudou nada quando mudou.
                            diferencaTexto(
                                it.volume.compareTo(0.0),
                                weightWithUnit(abs(it.volume), unidades),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Stat(
                        label = stringResource(Res.string.workout_summary_sets),
                        value = pluralStringResource(
                            Res.plurals.workout_hub_series,
                            state.setCount,
                            state.setCount,
                        ),
                        diferenca = ultima?.let {
                            diferencaTexto(
                                it.series,
                                pluralStringResource(
                                    Res.plurals.workout_hub_series,
                                    abs(it.series),
                                    abs(it.series),
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Comparacao(state)
            }

            // A secção dos recordes só existe quando há um. Dizer «sem recordes» a seguir a
            // cada treino normal transforma a ausência de recorde num facto negativo repetido,
            // e é o que a `estudo/areas/10` põe em «o que é inútil».
            if (state.prLabels.isNotEmpty()) {
                Text(stringResource(Res.string.workout_summary_prs), style = MaterialTheme.typography.titleMedium)
                state.prLabels.forEach { label ->
                    Text("🌟 ${stringResource(Res.string.workout_summary_pr_line, label)}")
                }
            }

            PrimaryButton(
                text = stringResource(Res.string.workout_summary_done),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}


/**
 * O que fica por baixo dos três números: a média das últimas três, ou a razão de não haver
 * comparação nenhuma.
 *
 * As duas ausências dizem coisas diferentes e por isso têm frases diferentes: um **treino
 * livre** não tem rotina, e a primeira vez de uma rotina ainda **não tem passado**. Calar as
 * duas era deixar quem olha a perguntar-se se a app se esqueceu.
 */
@Composable
private fun Comparacao(state: WorkoutSummaryState) {
    if (state.loading) return
    val unidades = rememberUnitSystem()

    if (state.comparacao.ultimaVez == null) {
        Text(
            stringResource(
                if (state.treinoLivre) {
                    Res.string.workout_summary_sem_rotina
                } else {
                    Res.string.workout_summary_primeira_vez
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    // A média fica numa linha só, e não numa segunda fila de três colunas: é o segundo termo
    // de comparação, e dar-lhe o mesmo peso do primeiro fazia o ecrã pedir duas leituras a
    // quem acabou de treinar.
    //
    // **E aqui só entra o que mudou.** Nas colunas de cima o `=` tem o rótulo por cima e
    // sabe-se de que métrica fala; nesta linha ficava «= · +73 kg · =», que é posicional e
    // não se lê — foi como saiu no aparelho. Sem nada para dizer, a linha di-lo por extenso.
    state.comparacao.media?.let { media ->
        val mudou = listOfNotNull(
            media.duracaoMin.takeIf { it != 0 }
                ?.let { diferencaTexto(it, formatDurationMin(abs(it))) },
            media.volume.takeIf { it != 0.0 }
                ?.let { diferencaTexto(it.compareTo(0.0), weightWithUnit(abs(it), unidades)) },
            media.series.takeIf { it != 0 }?.let {
                diferencaTexto(
                    it,
                    pluralStringResource(Res.plurals.workout_hub_series, abs(it), abs(it)),
                )
            },
        )
        Text(
            text = if (mudou.isEmpty()) {
                stringResource(Res.string.workout_summary_vs_media_igual)
            } else {
                stringResource(Res.string.workout_summary_vs_media) + ": " +
                    mudou.joinToString(" · ")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}

/**
 * Uma diferença, com o sinal à frente — e um `=` quando não houve nenhuma.
 *
 * O `=` é o que o esboço 10 escreve, e diz mais do que «0 séries»: quem repete a mesma rotina
 * com o mesmo trabalho quer ler *igual*, e não um zero que se confunde com uma contagem.
 *
 * **E vai sozinho, sem o número.** Escrito «= 0 série» — que foi como saiu no aparelho —, o
 * zero não acrescenta nada e o plural do português dá-lhe o singular; o rótulo por cima já diz
 * de que métrica se está a falar.
 *
 * **Sem cor**, e é uma divergência do esboço com razão. Ele pinta «−4 min» e «+436 kg» de
 * verde, o que é chamar bom a um treino mais curto — e mais curto não é melhor nem pior, é
 * mais curto. Nesta app a cor diz categoria e a forma diz estado, e um juízo sobre um único
 * treino é exactamente o que o motor dela se recusa a fazer com menos de cinco dias.
 */
private fun diferencaTexto(sinal: Int, texto: String): String =
    if (sinal == 0) "=" else "${if (sinal > 0) "+" else "−"}$texto"

/**
 * Um dos três números do treino: o rótulo, o valor, e a diferença para a última vez.
 *
 * A diferença vive **debaixo do valor a que pertence**, e não numa linha à parte com as três
 * juntas: assim não é preciso contar colunas para saber qual é qual.
 */
@Composable
private fun Stat(
    label: String,
    value: String,
    diferenca: String?,
    modifier: Modifier = Modifier,
) {
    // Centradas, como o esboço 10 §3 as desenha: três colunas de larguras iguais com o texto
    // encostado à esquerda leem-se como três coisas soltas, e isto é uma leitura só.
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (diferenca != null) {
            Text(
                diferenca,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
