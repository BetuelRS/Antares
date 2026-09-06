package pt.antares.app.feature.running.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.TravarRecuo
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.feature.running.domain.Split
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_acquiring_gps
import pt.antares.app.generated.resources.run_acquiring_hint
import pt.antares.app.generated.resources.run_goal_reached
import pt.antares.app.generated.resources.run_live_distance
import pt.antares.app.generated.resources.run_live_finish
import pt.antares.app.generated.resources.run_live_kcal
import pt.antares.app.generated.resources.run_live_lock
import pt.antares.app.generated.resources.run_live_pace_avg
import pt.antares.app.generated.resources.run_live_pace_km
import pt.antares.app.generated.resources.run_live_pause
import pt.antares.app.generated.resources.run_live_resume
import pt.antares.app.generated.resources.run_live_split
import pt.antares.app.generated.resources.run_live_locked
import pt.antares.app.generated.resources.run_live_paused
import pt.antares.app.generated.resources.run_live_paused_manual
import pt.antares.app.generated.resources.run_live_time
import pt.antares.app.generated.resources.run_live_unlock
import pt.antares.app.generated.resources.run_unit_km

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RunLiveScreen(
    onFinish: () -> Unit,
    viewModel: RunViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val m = state.metrics
    val unidades = rememberUnitSystem()
    val virgula = virgulaDecimal()
    val goalType by viewModel.goalType.collectAsState()
    val goalValue by viewModel.goalValue.collectAsState()
    // `rememberSaveable` e não `remember`: a corrida vive no serviço e sobrevive à morte do
    // ecrã. Um cadeado que se abrisse sozinho ao rodar o telemóvel dentro do bolso — que é
    // exactamente onde ele serve — não é cadeado nenhum.
    var locked by rememberSaveable { mutableStateOf(false) }

    val goalFraction: Float? = when (goalType) {
        RunGoalType.DISTANCE -> if (goalValue > 0) (m.distanceM / goalValue).toFloat().coerceIn(0f, 1f) else null
        RunGoalType.TIME -> if (goalValue > 0) (m.movingMs / (goalValue * 1000.0)).toFloat().coerceIn(0f, 1f) else null
        RunGoalType.NONE -> null
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            RunMap(
                path = state.path,
                modifier = Modifier.fillMaxSize(),
                follow = true,
            )
            if (state.active && !state.hasFix) {
                AcquiringGpsOverlay()
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            // A pausa manual diz **o que está parado**, e a automática só se anuncia. São dois
            // avisos e não um: a automática acontece sem se pedir e desfaz-se ao primeiro
            // passo; a manual foi pedida, e quem a pediu quer saber que ela pegou.
            val aviso = when {
                locked -> Res.string.run_live_locked
                m.pausaManual -> Res.string.run_live_paused_manual
                m.paused -> Res.string.run_live_paused
                else -> null
            }
            aviso?.let {
                Text(
                    stringResource(it),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                "${RunFormat.distance(m.distanceM, unidades, virgula)} ${stringResource(distanceUnitLabel(unidades))}",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.run_live_distance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            goalFraction?.let { frac ->
                Spacer(Modifier.height(Spacing.sm))
                LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
                if (frac >= 1f) {
                    Text(
                        stringResource(Res.string.run_goal_reached),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Metric(RunFormat.clock(m.movingMs), stringResource(Res.string.run_live_time))
                Metric(RunFormat.pace(m.avgPaceSecPerKm, unidades), stringResource(Res.string.run_live_pace_avg))
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // O ritmo **deste quilómetro**, e não a velocidade do último segmento entre
                // âncoras: essa ia de 4:10 a 6:50 e voltava de poucos em poucos segundos, e
                // um número que oscila assim não se lê a correr.
                Metric(RunFormat.pace(m.ritmoDoKmSecPerKm, unidades), stringResource(Res.string.run_live_pace_km))
                Metric("${m.kcal}", stringResource(Res.string.run_live_kcal))
            }

            UltimosParciais(state.parciais, unidades)

            Spacer(Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Cadeado(locked = locked, aoAlternar = { locked = !locked })

                BotaoDeCorrida(
                    texto = stringResource(
                        if (m.pausaManual) Res.string.run_live_resume else Res.string.run_live_pause,
                    ),
                    cor = MaterialTheme.colorScheme.primary,
                    corDoTexto = MaterialTheme.colorScheme.onPrimary,
                    activo = !locked,
                    modifier = Modifier.weight(1f),
                    aoTocar = { if (m.pausaManual) viewModel.retomar() else viewModel.pausar() },
                )
            }

            // **Terminar só aparece em pausa**, e é onde a decisão faz sentido: a correr, o
            // sítio mais premido do ecrã deixa de ser um botão vermelho que acaba tudo. O
            // toque longo fica: são duas fechaduras, e nenhuma delas se abre no bolso.
            if (m.pausaManual) {
                Spacer(Modifier.height(Spacing.sm))
                BotaoDeCorrida(
                    texto = stringResource(Res.string.run_live_finish),
                    cor = MaterialTheme.colorScheme.error,
                    corDoTexto = MaterialTheme.colorScheme.onError,
                    activo = !locked,
                    modifier = Modifier.fillMaxWidth(),
                    aoTocar = {},
                    aoTocarLongo = { viewModel.finish(); onFinish() },
                )
            }
        }
    }
}

/**
 * Os dois últimos quilómetros fechados.
 *
 * **Dois e não a tabela toda:** a pergunta a meio de uma corrida é «estou a acelerar ou a
 * abrandar», e para isso bastam o anterior e o de antes. A tabela inteira é do resumo, onde
 * há ecrã e tempo para a ler.
 */
@Composable
private fun UltimosParciais(parciais: List<Split>, unidades: UnitSystem) {
    if (parciais.isEmpty()) return

    Spacer(Modifier.height(Spacing.sm))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        // Do mais recente para trás, que é a ordem por que se lêem.
        for (s in parciais.takeLast(2).reversed()) {
            Metric(
                RunFormat.pace(s.paceSecPerKm, unidades),
                stringResource(Res.string.run_live_split, s.index),
            )
        }
    }
}

/**
 * O cadeado, que passa a bloquear o ecrã **todo**.
 *
 * Antes só desactivava o terminar: o resto do ecrã respondia e o recuo do sistema saía da
 * corrida com o cadeado fechado. Agora fecha os dois — e abre-se por **toque longo no próprio
 * cadeado**, que é o mesmo gesto do terminar e por isso já está nas mãos. Um toque simples
 * abria-se no bolso, que é o que o cadeado existe para evitar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Cadeado(locked: Boolean, aoAlternar: () -> Unit) {
    // Trava o gesto de voltar **enquanto está fechado**, e só então: fora disso, sair do ecrã
    // a meio de uma corrida continua a ser possível — a corrida vive no serviço, não aqui.
    TravarRecuo(activo = locked)

    Box(
        modifier = Modifier
            .size(48.dp)
            .combinedClickable(
                role = Role.Button,
                // Sem `onClickLabel`: fechado, o toque simples não faz nada, e anunciar
                // «desbloquear» num gesto que não desbloqueia é pior do que não anunciar. O
                // que o leitor de ecrã diz vem da descrição do ícone, que diz a verdade.
                onClick = { if (!locked) aoAlternar() },
                onLongClick = { if (locked) aoAlternar() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = stringResource(
                if (locked) Res.string.run_live_unlock else Res.string.run_live_lock,
            ),
            tint = if (locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BotaoDeCorrida(
    texto: String,
    cor: Color,
    corDoTexto: Color,
    activo: Boolean,
    modifier: Modifier = Modifier,
    aoTocar: () -> Unit,
    aoTocarLongo: (() -> Unit)? = null,
) {
    Surface(
        color = if (activo) cor else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(52.dp)
            .combinedClickable(
                enabled = activo,
                role = Role.Button,
                onClick = aoTocar,
                onLongClick = aoTocarLongo,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                texto,
                color = if (activo) corDoTexto else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun AcquiringGpsOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.xl),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            Text(
                stringResource(Res.string.run_acquiring_gps),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.run_acquiring_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
