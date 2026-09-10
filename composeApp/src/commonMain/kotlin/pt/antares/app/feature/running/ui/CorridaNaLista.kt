package pt.antares.app.feature.running.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.database.daos.CorridaNaListaRow
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.distanceUnitLabel
import pt.antares.app.core.designsystem.paceUnitLabel
import pt.antares.app.core.designsystem.virgulaDecimal
import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.dayShortDated
import pt.antares.app.core.util.epochMillisToLocalDate
import pt.antares.app.core.util.toEpochDay
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.run_hub_run_line

/**
 * Uma corrida numa lista — a do hub e a do histórico são a mesma linha.
 *
 * Eram duas até à 2.31.0, e diziam coisas diferentes da mesma corrida: o histórico
 * escrevia a data em ISO — «2026-09-01» — e não mostrava o ritmo. Uma linha só não deixa as
 * duas listas voltarem a divergir.
 */
@Composable
fun CorridaNaLista(
    run: CorridaNaListaRow,
    unidades: UnitSystem,
    hoje: Long,
    onRun: (String) -> Unit,
) {
    val virgula = virgulaDecimal()
    val dia = epochMillisToLocalDate(run.startedAt).toEpochDay()
    val ritmo = RunFormat.pace(run.avgPaceSecPerKm, unidades) + stringResource(paceUnitLabel(unidades))

    AntaresCard(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onRun(run.id) },
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f, fill = false).padding(end = Spacing.md)) {
                Text(
                    run.name.ifBlank { stringResource(activityLabel(run.type)) },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(
                        Res.string.run_hub_run_line,
                        dayShortDated(dia, hoje),
                        RunFormat.clock(run.movingS * MS_POR_SEGUNDO),
                        ritmo,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${RunFormat.distance(run.distanceM, unidades, virgula)} " +
                    stringResource(distanceUnitLabel(unidades)),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private const val MS_POR_SEGUNDO = 1000L
