package pt.antares.app.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresGhostCard
import pt.antares.app.core.designsystem.components.AntaresHeroCard
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.PrimaryButton
import pt.antares.app.core.designsystem.components.SecondaryButton
import pt.antares.app.core.designsystem.components.SemanaEmPontos
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.core.util.formatDurationMin
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.running.ui.RunFormat
import pt.antares.app.feature.workout.data.CentroDeTreino
import pt.antares.app.feature.workout.data.CorridaNaSemana
import pt.antares.app.feature.workout.data.DestaqueDoTreino
import pt.antares.app.feature.workout.data.RotinaEmDestaque
import pt.antares.app.feature.workout.data.RotinaNaLista
import pt.antares.app.feature.workout.data.TreinoNaLista
import pt.antares.app.feature.workout.ui.WorkoutHubViewModel
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

/**
 * O centro de treino.
 *
 * Era um menu: cinco botões cinzentos iguais e uma lista de nomes, sem um único número. Passa
 * a responder à pergunta que se faz ao abrir este separador — **treino o quê hoje, e como é
 * que começo?** — e nenhum dos dados que mostra é novo: todos já eram calculados noutro sítio
 * da app e nunca chegavam aqui.
 *
 * Os quatro caminhos que se visitam uma vez por mês — plano da semana, biblioteca, histórico
 * e estatísticas — passam para o menu do topo, que é o espaço que eles ocupavam.
 */
@Composable
fun WorkoutScreen(
    menu: MenuDoTreino,
    onRoutine: (String) -> Unit,
    onStartRoutine: (String) -> Unit,
    onStartEmpty: () -> Unit,
    onResume: () -> Unit,
    onWorkout: (String) -> Unit,
    onRun: () -> Unit,
    viewModel: WorkoutHubViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val unidades = rememberUnitSystem()
    val hoje = todayEpochDay()

    LazyColumn(
        modifier = Modifier.fillMaxSize().larguraDeLeitura().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { Cabecalho(menu) }

        // Uma sessão a decorrer ganha ao treino de hoje: já se está a treinar, e a pergunta
        // do ecrã passou a ser outra.
        //
        // E o cartão de destaque **desaparece** enquanto ela dura, em vez de ficar ao lado a
        // oferecer «Começar»: o `startOrResume` devolve a sessão aberta e ignora a rotina que
        // se lhe pede, portanto esse botão dizia «Começar Full Body A» e levava ao treino que
        // já estava a decorrer. Um botão que faz outra coisa é pior do que um botão a menos.
        state.sessaoActivaDesde?.let { desde ->
            item { Retomar(desde, onResume) }
        }
        if (state.sessaoActivaDesde == null) {
            item {
                Destaque(
                    destaque = state.destaque,
                    hoje = hoje,
                    onStart = onStartRoutine,
                    onSchedule = menu.plano,
                )
            }
        }

        if (state.carregado) {
            item { CartaoDaSemana(state, hoje, unidades) }
        }

        // O «＋» vive no cabeçalho da secção, como o esboço o desenha, e não num botão de
        // largura toda no fim da lista: criar uma rotina é raro ao pé de começar uma, e o
        // botão lá em baixo ficava fora do ecrã com seis rotinas — que é o caso normal, com
        // sete semeadas.
        item {
            val nomeNovo = stringResource(Res.string.workout_hub_new_routine)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.workout_hub_routines),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.createRoutine(nomeNovo, onCreated = onRoutine) }) {
                    Icon(Icons.Default.Add, contentDescription = nomeNovo)
                }
            }
        }

        items(state.rotinas, key = { it.id }) { rotina ->
            LinhaDaRotina(
                rotina = rotina,
                hoje = hoje,
                // Pela mesma razão do cartão: com um treino aberto, o ▶ levaria ao que está
                // a decorrer e não à rotina que se tocou.
                podeComecar = state.sessaoActivaDesde == null,
                onOpen = onRoutine,
                onStart = onStartRoutine,
            )
        }

        if (state.carregado && state.rotinas.isEmpty()) {
            item {
                Text(
                    stringResource(Res.string.workout_hub_no_routines),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.ultimos.isNotEmpty()) {
            item {
                Text(
                    stringResource(Res.string.workout_hub_recent),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
            items(state.ultimos, key = { it.id }) { treino ->
                LinhaDoTreino(treino, hoje, unidades, onOpen = onWorkout)
            }
        }

        // A corrida vive aqui desde que deixou de ter separador próprio: são os dois
        // atividade, e ela ocupava um quinto da barra para uma coisa que se faz umas vezes
        // por mês. São dois factos e um caminho — o hub dela continua a ser o hub dela.
        item {
            Text(
                stringResource(Res.string.nav_run),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
        item { LinhaDaCorrida(state.corrida, hoje, unidades, onRun) }

        // O treino vazio é a acção mais rara da área, e por isso é a última coisa do ecrã —
        // era a terceira, no lugar mais visível.
        //
        // E desaparece com um treino aberto, **pela mesma razão do cartão de destaque e do
        // ▶ das rotinas**: o `startOrResume` devolve a sessão que já está a decorrer, por
        // isso este botão abria o treino que estava a meio em vez de um vazio. A 2.20.0
        // escondeu os outros dois e deixou este — o `CentroDeTreinoUiTest` chegou a escrever
        // que ele «está sempre no ecrã», para escolher a asserção, e ninguém foi ver o que
        // ele fazia.
        if (state.sessaoActivaDesde == null) {
            item {
                SecondaryButton(
                    text = stringResource(Res.string.workout_hub_start_empty),
                    onClick = onStartEmpty,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                )
            }
        }
    }
}

/**
 * O botão de retomar, com o tempo que o treino já leva.
 *
 * O tempo está aqui porque «Retomar» sozinho retoma às cegas — é a queixa da área 06 sobre o
 * `hasActiveSession` ser um booleano —, e conta-se ao minuto e não ao segundo: o número tem
 * minutos, e acordar de segundo a segundo redesenhava o ecrã sessenta vezes para ele mudar
 * uma. É o mesmo relógio do ecrã do jejum, pela mesma razão.
 */
@Composable
private fun Retomar(desdeMs: Long, onResume: () -> Unit) {
    var agora by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(desdeMs) {
        while (true) {
            delay(MS_POR_MINUTO - agora % MS_POR_MINUTO)
            agora = Clock.System.now().toEpochMilliseconds()
        }
    }

    val minutos = ((agora - desdeMs) / MS_POR_MINUTO).toInt().coerceAtLeast(0)
    PrimaryButton(
        // `%d min` sem conversão dava «2618 min» a um treino esquecido aberto de um dia para
        // o outro — e a barra da sessão, no mesmo treino, dizia `43:39:17`. Duas formas do
        // mesmo facto, e a deste ecrã era a que não se lia. O `formatDurationMin` é o que a
        // janela alimentar e o jejum já usam.
        text = stringResource(Res.string.workout_hub_resume_running, formatDurationMin(minutos)),
        onClick = onResume,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Cabecalho(menu: MenuDoTreino) {
    var aberto by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.workout_hub_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { aberto = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.workout_hub_more))
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.schedule_title)) },
                onClick = { aberto = false; menu.plano() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.workout_hub_library)) },
                onClick = { aberto = false; menu.biblioteca() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.workout_history_title)) },
                onClick = { aberto = false; menu.historico() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.workout_stats_title)) },
                onClick = { aberto = false; menu.estatisticas() },
            )
        }
    }
}

/**
 * O cartão principal. O plano da semana ganha sempre à dedução a partir do histórico: é uma
 * decisão escrita pela pessoa, e a outra é um palpite da app.
 */
@Composable
private fun Destaque(
    destaque: DestaqueDoTreino,
    hoje: Long,
    onStart: (String) -> Unit,
    onSchedule: () -> Unit,
) {
    when (destaque) {
        is DestaqueDoTreino.DeHoje -> CartaoDeDestaque(
            etiqueta = stringResource(Res.string.workout_hub_from_plan),
            rotina = destaque.rotina,
            onStart = onStart,
        )

        // A data por extenso, e não «há N dias»: «há 1 dias» é o que sai de um contador sem
        // plural, e o `dayShortDated` já resolve o hoje, o ontem e o ano quando é preciso.
        is DestaqueDoTreino.Ultima -> CartaoDeDestaque(
            etiqueta = stringResource(
                Res.string.workout_hub_last_done,
                dayShortDated(destaque.ultimaVezEpochDay, hoje),
            ),
            rotina = destaque.rotina,
            onStart = onStart,
        )

        // Sem plano e sem histórico não há o que propor. A app semeia sete rotinas e nenhuma
        // delas é mais desta pessoa do que as outras — escolher uma seria fingir que sabe.
        //
        // Cartão tracejado, e não sólido: é uma proposta, não um facto, e essa é a mesma
        // distinção que separa a sugestão de repetir de um registo no diário.
        DestaqueDoTreino.Convite -> AntaresGhostCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.workout_hub_no_plan_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(Res.string.workout_hub_no_plan_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            PrimaryButton(
                text = stringResource(Res.string.workout_hub_no_plan_action),
                onClick = onSchedule,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
            )
        }
    }
}

@Composable
private fun CartaoDeDestaque(
    etiqueta: String,
    rotina: RotinaEmDestaque,
    onStart: (String) -> Unit,
) {
    AntaresHeroCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        // O nome não é tocável aqui, e é de propósito: a mesma rotina está na lista logo
        // abaixo, onde tocar no nome abre o editor com uma linha inteira por alvo. Duas
        // portas para a mesma acção é o que o estudo condena na área 03 — e esta seria uma
        // porta de vinte dp de altura.
        Text(
            rotina.nome,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        if (rotina.exercicios.isNotEmpty()) {
            Text(
                rotina.exercicios.joinToString(SEPARADOR),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            resumoDaRotina(rotina),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        PrimaryButton(
            text = stringResource(Res.string.workout_hub_start),
            onClick = { onStart(rotina.id) },
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
        )
    }
}

@Composable
private fun resumoDaRotina(rotina: RotinaEmDestaque): String {
    val exercicios = pluralStringResource(
        Res.plurals.workout_hub_exercicios,
        rotina.totalDeExercicios,
        rotina.totalDeExercicios,
    )
    // Pelo `formatDurationMin`, como os outros quatro sítios que a 2.24.0 converteu: um
    // treino esquecido aberto de um dia para o outro dava «~4236 min» aqui, e a linha do
    // histórico do mesmo treino dizia «70h 36m» dois toques à frente. Esta chave escapou
    // àquela correcção por se ter procurado o nome `workout_hub_minutes` e não o formato.
    val duracao = rotina.ultimaDuracaoMin ?: return exercicios
    return exercicios + SEPARADOR +
        stringResource(Res.string.workout_hub_last_duration, formatDurationMin(duracao))
}

@Composable
private fun CartaoDaSemana(state: CentroDeTreino, hoje: Long, unidades: UnitSystem) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        // Sem número ao lado do título: os sete pontos **são** a contagem, e essa é a razão
        // de o componente existir — «cinco dias seguidos e cinco alternados são semanas
        // diferentes, e o número não os distingue». Pôr o número de volta ao lado da forma
        // contradizia-o, e um «0» solto na cor da app lê-se como um alarme.
        Text(
            stringResource(Res.string.workout_hub_week),
            style = MaterialTheme.typography.titleMedium,
        )
        SemanaEmPontos(
            inicioEpochDay = state.semana.inicioEpochDay,
            diasMarcados = state.semana.diasComTreino,
            hoje = hoje,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Text(
            // Sem treinos, o volume e as séries seriam dois zeros a ocupar uma linha.
            if (state.semana.diasComTreino.isEmpty()) {
                stringResource(Res.string.workout_hub_week_none)
            } else {
                stringResource(
                    Res.string.workout_hub_week_summary,
                    weightWithUnit(state.semana.volume, unidades),
                    pluralStringResource(
                        Res.plurals.workout_hub_series,
                        state.semana.series,
                        state.semana.series,
                    ),
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}

/**
 * Tocar no nome abre o editor; tocar no ▶ treina. São duas acções e por isso são dois alvos —
 * até aqui o cartão inteiro levava ao editor, e começar a rotina de hoje custava três toques
 * e um percorrer até ao fundo.
 */
@Composable
private fun LinhaDaRotina(
    rotina: RotinaNaLista,
    hoje: Long,
    podeComecar: Boolean,
    onOpen: (String) -> Unit,
    onStart: (String) -> Unit,
) {
    LinhaDaLista(
        titulo = rotina.nome,
        subtitulo = subtituloDaRotina(rotina, hoje),
        onClick = { onOpen(rotina.id) },
        aoLado = if (!podeComecar) {
            null
        } else {
            {
                FilledIconButton(onClick = { onStart(rotina.id) }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(
                            Res.string.workout_hub_start_named,
                            rotina.nome,
                        ),
                    )
                }
            }
        },
    )
}

/**
 * A corrida no painel de treino — uma linha da lista, e não um cartão: o `CartaoDaCorrida`
 * do «Hoje» é outro composable, e dois com o mesmo nome fazem procurar duas vezes.
 *
 * Dois factos e um caminho. Sem corridas diz que não há, com o mesmo tom do cartão da semana
 * — o `estudo/areas/01-hoje.md` conta o custo do contrário: *«quem nunca correu tem um cartão
 * permanente a dizer que não há corridas»*. Aqui ele é uma porta, e não um relatório vazio.
 */
@Composable
private fun LinhaDaCorrida(
    corrida: CorridaNaSemana,
    hoje: Long,
    unidades: UnitSystem,
    onRun: () -> Unit,
) {
    val virgula = virgulaDecimal()
    val unidade = stringResource(distanceUnitLabel(unidades))

    LinhaDaLista(
        titulo = if (corrida.metrosNaSemana > 0) {
            stringResource(
                Res.string.workout_hub_run_week,
                "${RunFormat.distance(corrida.metrosNaSemana, unidades, virgula)} $unidade",
            )
        } else {
            stringResource(Res.string.workout_hub_run_none_week)
        },
        subtitulo = corrida.ultima?.let {
            stringResource(
                Res.string.workout_hub_run_last,
                it.nome,
                dayShortDated(it.epochDay, hoje),
                "${RunFormat.distance(it.metros, unidades, virgula)} $unidade",
            )
        } ?: stringResource(Res.string.workout_hub_run_none_ever),
        icone = Icons.AutoMirrored.Filled.DirectionsRun,
        onClick = onRun,
    )
}

@Composable
private fun subtituloDaRotina(rotina: RotinaNaLista, hoje: Long): String {
    val exercicios = pluralStringResource(
        Res.plurals.workout_hub_exercicios,
        rotina.totalDeExercicios,
        rotina.totalDeExercicios,
    )
    val quando = rotina.ultimaVezEpochDay
        ?: return exercicios + SEPARADOR + stringResource(Res.string.workout_hub_routine_never)
    return exercicios + SEPARADOR + dayShortDated(quando, hoje)
}

@Composable
private fun LinhaDoTreino(
    treino: TreinoNaLista,
    hoje: Long,
    unidades: UnitSystem,
    onOpen: (String) -> Unit,
) {
    LinhaDaLista(
        // Um treino livre não nasceu de rotina nenhuma, e escrever o nome de uma seria
        // inventá-lo. A linha diz o que ele é.
        titulo = treino.nomeDaRotina ?: stringResource(Res.string.workout_hub_free_workout),
        subtitulo = stringResource(
            Res.string.workout_hub_workout_line,
            dayShortDated(treino.epochDay, hoje),
            formatDurationMin(treino.duracaoMin),
            pluralStringResource(Res.plurals.workout_hub_series, treino.series, treino.series),
        ),
        aoLado = {
            Text(
                weightWithUnit(treino.volume, unidades),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = { onOpen(treino.id) },
    )
}

/**
 * Os quatro caminhos do menu do canto, num objecto só.
 *
 * São um grupo no ecrã — vivem todos no mesmo ⋮ — e por isso são um grupo na assinatura:
 * soltos, eram quatro lambdas iguais que ninguém distingue ao passá-las por engano.
 */
class MenuDoTreino(
    val biblioteca: () -> Unit,
    val historico: () -> Unit,
    val estatisticas: () -> Unit,
    val plano: () -> Unit,
)

// O ponto médio separa factos na mesma linha em toda a app — na linha do diário, na do
// alimento, na do exercício.
private const val SEPARADOR = " · "

private const val MS_POR_MINUTO = 60_000L
