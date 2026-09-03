package pt.antares.app.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.larguraDeLeitura
import pt.antares.app.core.designsystem.loadWithUnit
import pt.antares.app.core.designsystem.rememberUnitSystem
import pt.antares.app.core.designsystem.trimmedDecimal
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.designsystem.weightWithUnit
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.AntaresHeroCard
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.LoadingState
import pt.antares.app.core.database.entities.WorkoutSetEntity
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.dayShort
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.formatDurationMin
import pt.antares.app.core.util.formatMinuteOfDay
import pt.antares.app.core.util.minuteOfDayAt
import pt.antares.app.feature.workout.data.SessionBreakdown
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.*

@Composable
fun WorkoutDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = koinViewModel(),
) {
    val breakdown by viewModel.breakdown.collectAsState()
    val unidades = rememberUnitSystem()
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    val b = breakdown
    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                // O título era `workout_detail_title` — «Detalhe do treino» —, genérico, e
                // abrir um treino de há três meses não dizia qual era nem quando foi. Passa
                // a ser o nome da rotina, que é o que distingue um treino de outro.
                title = b?.let {
                    it.nomeDaRotina ?: stringResource(Res.string.workout_hub_free_workout)
                } ?: stringResource(Res.string.workout_detail_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        if (b == null) {
            LoadingState(Modifier.padding(padding))
            return@AntaresScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).larguraDeLeitura().padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { Cabecalho(b, unidades) }

            items(b.exercises, key = { it.id }) { ex ->
                AntaresCard(modifier = Modifier.fillMaxWidth()) {
                    Text(ex.name, style = MaterialTheme.typography.titleSmall)
                    ex.sets.forEachIndexed { i, set ->
                        Text(
                            linhaDaSerie(i, set, unidades),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A data, a hora e as três métricas.
 *
 * O cartão dizia `45 min · 9 338 kg` e mais nada: sem data, sem hora e sem rotina. A hora
 * está aqui porque duas sessões do mesmo dia deixavam de se distinguir — e a app guarda o
 * instante em que o treino começou desde sempre.
 */
@Composable
private fun Cabecalho(b: SessionBreakdown, unidades: UnitSystem) {
    AntaresHeroCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(
                Res.string.workout_detail_when,
                dayShort(epochMillisToLocalDate(b.startedAt)),
                formatMinuteOfDay(minuteOfDayAt(b.startedAt)),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            stringResource(
                Res.string.workout_detail_metrics,
                formatDurationMin(b.durationMin),
                weightWithUnit(b.volume, unidades),
                pluralStringResource(Res.plurals.workout_hub_series, b.series, b.series),
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

/**
 * Uma série do detalhe, com o RPE quando ele existe.
 *
 * **O RPE era gravado e nunca lido em lado nenhum da app** — o `estudo/areas/10` chama-lhe
 * «a informação entra e não volta a sair». Aparece só onde foi escrito: uma série sem RPE
 * não ganha um traço nem um zero, porque não o ter é o caso normal.
 */
@Composable
private fun linhaDaSerie(indice: Int, set: WorkoutSetEntity, unidades: UnitSystem): String {
    val base = "${indice + 1}. ${loadWithUnit(set.weightKg, unidades)} × ${set.reps}"
    val aquecimento = if (set.isWarmup) SEPARADOR + stringResource(Res.string.session_warmup) else ""
    val corpo = if (set.bodyweightKg != null) {
        SEPARADOR + stringResource(Res.string.session_set_bodyweight)
    } else {
        ""
    }
    // Escrito como a sessão o escreve — «RPE 8» —, e não com um formato só deste ecrã.
    val rpe = set.rpe?.let {
        SEPARADOR + stringResource(Res.string.session_rpe) + " " +
            trimmedDecimal(it, comma = virgulaDecimal())
    } ?: ""
    return base + corpo + aquecimento + rpe
}

// O ponto médio separa factos na mesma linha em toda a app.
private const val SEPARADOR = " · "
