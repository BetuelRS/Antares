package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.calc.ChartScale
import pt.antares.app.core.calc.SeriesPorMusculo
import pt.antares.app.core.calc.TimeAxis
import pt.antares.app.core.calc.StatsPeriod
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.AntaresChart
import pt.antares.app.core.designsystem.fmtG
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.loadWithUnit
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.weightUnitLabel
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.axisDate
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.feature.workout.data.MusculoNaSemana
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

/**
 * As estatísticas do treino.
 *
 * Eram duas listas sem tempo nenhum — o volume por músculo «desta semana», sem dizer o que
 * isso era, e os recordes sem data. A `estudo/areas/10` chama-lhe a maior distância entre o
 * que está guardado e o que se vê, e o esboço 10 desenha o que substitui as duas.
 *
 * **A semana é a ISO em toda a parte.** O ecrã contava sete dias para trás a partir de agora,
 * e o cartão «Esta semana» do painel de treino, dois toques atrás, conta de segunda a
 * domingo. Era a mesma palavra a querer dizer duas coisas dentro do mesmo separador.
 */
@Composable
fun WorkoutStatsScreen(
    onBack: () -> Unit,
    viewModel: WorkoutStatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()

    AntaresScaffold(
        topBar = { AntaresTopBar(title = stringResource(Res.string.workout_stats_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .larguraDeLeitura()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {

            // Em linha corrida e não numa `Row`, pela mesma razão do ecrã da nutrição: quatro
            // períodos não cabem na largura de um telemóvel estreito, e o quarto ficava
            // cortado em vez de descer.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                StatsPeriod.entries.forEach { periodo ->
                    FilterChip(
                        selected = state.period == periodo,
                        onClick = { viewModel.setPeriod(periodo) },
                        label = { Text(stringResource(rotuloDoPeriodo(periodo))) },
                    )
                }
            }

            Frequencia(state)
            VolumePorSemana(state, unidades)
            SeriesDosMusculos(state, unidades)
            Recordes(state, unidades)
        }
    }
}

@Composable
private fun Frequencia(state: WorkoutStatsState) {
    Text(
        stringResource(Res.string.workout_stats_frequency_title),
        style = MaterialTheme.typography.titleMedium,
    )
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        val semanas = state.estatisticas.treinosPorSemana
        Text(
            stringResource(
                Res.string.workout_stats_frequency_count,
                state.estatisticas.treinosNoPeriodo,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )

        // A média e a linha só aparecem com semanas que cheguem. Dois pontos não são uma
        // tendência — são um segmento de recta que convida a ler uma direcção que ninguém
        // mediu, e é a mesma recusa do `GoalProjection`.
        if (state.period.temSerie && semanas.isNotEmpty()) {

            // E a média cala-se quando arredonda a zero: um treino num ano dá «0,0 de média»
            // por cima de «1 no período escolhido», e as duas frases não podem ser lidas
            // juntas. A contagem, essa, é um facto e fica sempre.
            if (state.estatisticas.mediaDeTreinos >= MEDIA_MINIMA_LEGIVEL) {
                Text(
                    stringResource(
                        Res.string.workout_stats_frequency_avg,
                        fmtG(state.estatisticas.mediaDeTreinos),
                        semanas.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GraficoSemanal(
                dias = state.estatisticas.semanasEpochDay,
                valores = semanas.map { it.toDouble() },
                unidade = stringResource(Res.string.workout_stats_frequency_unit),
            )
        }
    }
}

/**
 * Uma série por semana ISO, com o gráfico da app — o mesmo do peso e das medidas.
 *
 * O esboço 10 pede-o pelo nome: um `Sparkline` desenha a forma e não diz onde ela começa nem
 * acaba, e uma paragem de um mês fica indistinguível de uma semana fraca. Com eixo temporal,
 * cada semana ocupa no traço o espaço que ocupou na vida.
 */
@Composable
private fun GraficoSemanal(dias: List<Long>, valores: List<Double>, unidade: String) {
    if (dias.size != valores.size || dias.isEmpty()) return
    AntaresChart(
        points = dias.zip(valores),
        height = ALTURA_DO_GRAFICO_DP,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        // As duas séries contam coisas, e nenhuma delas existe abaixo de zero: sem o chão, a
        // folga da escala punha o eixo a começar em «−0,5 treinos» numa semana sem treino
        // nenhum. Visto no aparelho, escrito na etiqueta.
        chaoDaEscala = 0.0,
        labels = { escala, eixo -> EixoDoGrafico(escala, eixo, unidade) },
    )
}

@Composable
private fun EixoDoGrafico(escala: ChartScale, eixo: TimeAxis, unidade: String) {
    // Os extremos numa linha só e longe das datas, como no Progresso: postos um em cada
    // ponta por cima delas, lê-se o valor de um lado com a data do outro.
    Text(
        stringResource(Res.string.chart_vertical_range, fmtG(escala.min), fmtG(escala.max), unidade),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        eixo.tickDays().forEach { dia ->
            Text(
                axisDate(dia, eixo.spanDays),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VolumePorSemana(state: WorkoutStatsState, unidades: UnitSystem) {
    val volumes = state.estatisticas.volumePorSemana
    if (!state.period.temSerie || volumes.all { it <= 0.0 }) return

    Text(
        stringResource(Res.string.workout_stats_volume_title),
        style = MaterialTheme.typography.titleMedium,
    )
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        // O número é o da semana corrente, que é a última da série: é ao lado dela que se lê
        // a linha, e escrever o total do período misturava duas leituras no mesmo cartão.
        Text(weightWithUnit(volumes.last(), unidades), style = MaterialTheme.typography.bodyMedium)
        GraficoSemanal(
            dias = state.estatisticas.semanasEpochDay,
            valores = volumes,
            unidade = stringResource(weightUnitLabel(unidades)),
        )
    }
}

@Composable
private fun SeriesDosMusculos(state: WorkoutStatsState, unidades: UnitSystem) {
    Text(
        stringResource(Res.string.workout_stats_series_title),
        style = MaterialTheme.typography.titleMedium,
    )
    val musculos = state.estatisticas.musculos
    if (musculos.isEmpty()) {
        Text(
            stringResource(Res.string.workout_stats_no_series),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    // Sem uma semana inteira não há média semanal, e por isso também não há faixa: ela é
    // semanal, e pô-la ao lado de um dia era comparar coisas diferentes.
    val temSemana = musculos.any { it.porSemana != null }
    Text(
        stringResource(
            if (temSemana) {
                Res.string.workout_stats_series_week_sub
            } else {
                Res.string.workout_stats_series_period_sub
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        // Com a faixa desenhada, o topo da escala nunca fica abaixo do fim dela: senão um
        // músculo com cinco séries enchia a barra toda e parecia estar no sítio.
        //
        // **Sem faixa, a escala é só o maior dos músculos.** Travá-la nas vinte séries
        // semanais quando o período é um dia media as barras contra uma referência que não
        // está no ecrã e que não vale para um dia — o comprimento passava a dizer uma coisa
        // que ninguém podia ler.
        val maiorValor = musculos.maxOf { it.porSemana ?: it.series }
        val maximo = if (temSemana) {
            maiorValor.coerceAtLeast(SeriesPorMusculo.FAIXA_MAX)
        } else {
            maiorValor
        }
        musculos.forEach { BarraDoMusculo(it, maximo, temSemana, unidades) }
    }

    if (temSemana) {
        Text(
            stringResource(
                Res.string.workout_stats_series_band,
                SeriesPorMusculo.FAIXA_MIN,
                SeriesPorMusculo.FAIXA_MAX,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BarraDoMusculo(
    musculo: MusculoNaSemana,
    maximo: Int,
    comFaixa: Boolean,
    unidades: UnitSystem,
) {
    val valor = musculo.porSemana ?: musculo.series
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {

        // Em linha corrida e não numa `Row`: a 200 % de escala de letra «menos de 1 série por
        // semana · 600 kg» não cabe ao lado do nome do músculo, e num `Row` com
        // `SpaceBetween` os dois textos entram um no outro em vez de o segundo descer. É a
        // mesma correcção da 2.19.0, e o `estudo/transversal/03` §3.1 nomeia a família.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                stringResource(muscleLabel(musculo.musculo)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = Spacing.md),
            )
            // O volume fica ao lado e em letra pequena: a `estudo/areas/10` chama-lhe «a
            // estatística certa» para o equilíbrio, e é por isso que não desaparece — o que
            // muda é que deixou de ser ele a decidir o comprimento da barra.
            //
            // O plural é o mesmo do painel de treino: uma série não são «1 sets», e a app já
            // tinha a chave — foi esse o defeito que a corrida da 2.20.0 apanhou no aparelho.
            //
            // E abaixo de uma por semana escreve-se «menos de 1», que é o que ela é: uma
            // série num mês arredondava a zero, e a linha lia-se «0 séries · 600 kg» — zero
            // séries com seiscentos quilos é uma contradição dentro da mesma linha. É o
            // mesmo vocabulário do «< 0,5 mg» dos micronutrientes.
            Text(
                if (valor == 0 && musculo.series > 0) {
                    stringResource(Res.string.workout_stats_series_under_one)
                } else {
                    pluralStringResource(Res.plurals.workout_hub_series, valor, valor)
                } + SEPARADOR +
                    weightWithUnit(musculo.volumeDaJanela ?: musculo.volume, unidades),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BarraComFaixa(valor = valor, maximo = maximo, comFaixa = comFaixa)
    }
}

/**
 * Uma barra de séries com a faixa de referência desenhada por trás, e a barra em cor de
 * atenção quando fica abaixo dela.
 *
 * **A cor é o que o esboço 10 pede**, com a razão dele: *«a única forma de descobrir um
 * desequilíbrio é alguém o apontar»*. Cheguei a decidir o contrário — que a faixa fosse só
 * forma —, e a razão que dei era a regra dos micronutrientes: lá, uma barra curta pode ser
 * carência **ou** falta de análise, e a cor não distingue as duas. Aqui não há a segunda
 * leitura: a contagem é medida, e abaixo da faixa quer dizer uma coisa só.
 *
 * O `UmaConvencaoDeCorTest` guarda que a cor não diga **duas** coisas ao mesmo tempo. Nestas
 * barras não dizia nenhuma; passa a dizer uma.
 *
 * E o que impede isto de virar juízo é a frase por baixo do cartão, que diz que a faixa vem
 * da literatura e não é um alvo calculado para esta pessoa.
 */
@Composable
private fun BarraComFaixa(valor: Int, maximo: Int, comFaixa: Boolean) {
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .padding(top = Spacing.xs)
            .height(ALTURA_DA_BARRA_DP.dp)
            .clip(RoundedCornerShape(RAIO_DA_BARRA_DP.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // Mede a própria caixa em vez de assumir a largura da janela: esta barra vive dentro
        // de um cartão que já perdeu margens para os lados, e num painel de tablet perde mais.
        val largura = maxWidth
        if (comFaixa) {
            Box(
                Modifier
                    .offset(x = largura * SeriesPorMusculo.FAIXA_MIN / maximo)
                    // Não é uma largura fixa: é uma fração da caixa que acabou de ser medida,
                    // e cresce com ela. Está escrita assim, e não com `fillMaxWidth(fração)`,
                    // porque esta barra começa a meio — o `fillMaxWidth` mede sempre da
                    // esquerda, e a faixa tem de saber onde acaba **e** onde começa. Não
                    // segura texto nenhum, portanto a letra grande não lhe toca.
                    .width(
                        largura * (SeriesPorMusculo.FAIXA_MAX.coerceAtMost(maximo) -
                            SeriesPorMusculo.FAIXA_MIN) / maximo,
                    )
                    .fillMaxHeight()
                    // Neutra, e não da família do âmbar: a barra abaixo da faixa é âmbar, e
                    // as duas ficam encostadas uma à outra — âmbar vivo ao lado de âmbar
                    // apagado lê-se como uma só mancha.
                    .background(MaterialTheme.colorScheme.outline),
            )
        }
        // **Âmbar e não vermelho.** O `error` do tema é `#FF6B6B` e a primária é `#FF5A4A`:
        // lado a lado não se distinguem, e a barra abaixo da faixa ficava igual às outras.
        // Além disso, «nesta app o vermelho quer dizer isto faz alguma coisa» — foi a razão
        // escrita na 2.18.2 para o selo da origem deixar de o ser. O âmbar já é a cor de
        // atenção sem acção: é a da suposição da AI e a do aviso da cópia em atraso.
        val abaixo = comFaixa && valor < SeriesPorMusculo.FAIXA_MIN
        Box(
            Modifier
                .fillMaxWidth(valor.coerceAtMost(maximo).toFloat() / maximo)
                .fillMaxHeight()
                .clip(RoundedCornerShape(RAIO_DA_BARRA_DP.dp))
                .background(
                    if (abaixo) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
        )
    }
}

@Composable
private fun Recordes(state: WorkoutStatsState, unidades: UnitSystem) {
    Text(
        stringResource(Res.string.workout_stats_records),
        style = MaterialTheme.typography.titleMedium,
    )
    if (state.records.isEmpty()) {
        Text(
            stringResource(Res.string.workout_stats_no_records),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val maisRecente = state.recordeMaisRecente
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        state.records.forEach { r ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f, fill = false).padding(end = Spacing.md)) {
                    Text(r.name, style = MaterialTheme.typography.bodyMedium)

                    // A data é a do dia em que o recorde aconteceu. Sem ela, um de 2024
                    // aparecia igual a um de ontem — é o defeito concreto 4 da área 10.
                    val quando = dayShortDated(r.epochDay)
                    Text(
                        if (r.epochDay == maisRecente) {
                            stringResource(
                                Res.string.workout_stats_record_when,
                                quando,
                                stringResource(Res.string.workout_stats_record_new),
                            )
                        } else {
                            quando
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    loadWithUnit(r.oneRm, unidades),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun rotuloDoPeriodo(p: StatsPeriod): StringResource = when (p) {
    StatsPeriod.DAY -> Res.string.stat_period_day
    StatsPeriod.WEEK -> Res.string.stat_period_week
    StatsPeriod.MONTH -> Res.string.stat_period_month
    StatsPeriod.YEAR -> Res.string.stat_period_year
}

/** Abaixo disto a média arredonda a zero, e um zero ao lado de uma contagem contradi-la. */
private const val MEDIA_MINIMA_LEGIVEL = 0.1

private const val SEPARADOR = " · "
private const val ALTURA_DO_GRAFICO_DP = 56
private const val ALTURA_DA_BARRA_DP = 12
private const val RAIO_DA_BARRA_DP = 6
