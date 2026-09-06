package pt.antares.app.feature.diary

import pt.antares.app.core.calc.AguaDaComida
import androidx.compose.foundation.layout.Arrangement
import pt.antares.app.core.designsystem.Spacing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import pt.antares.app.core.database.entities.ExerciseLogEntity
import pt.antares.app.core.model.ExerciseOrigin
import pt.antares.app.core.designsystem.success
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.components.AntaresCard
import pt.antares.app.core.designsystem.components.rememberApagarComDesfazer
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

/**
 * O exercício do dia: o cabeçalho com o total, e uma linha por entrada.
 *
 * As calorias aqui são o que se gastou **a mais** do que estar sentado — ver o `MetCalc`.
 */
internal fun LazyListScope.exerciseSection(
    entries: List<ExerciseLogEntity>,
    kcal: Int,
    onAdd: () -> Unit,
    onEdit: (ExerciseLogEntity) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    item(key = "exercise-header") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.exercise_section_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (kcal > 0) {
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "$kcal ${stringResource(Res.string.common_kcal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.exercise_add_cta))
            }
        }
    }
    if (entries.isEmpty()) {
        item(key = "exercise-empty") {
            Text(
                stringResource(Res.string.exercise_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(entries, key = { "ex-${it.id}" }) { entry ->
        val apagar = rememberApagarComDesfazer()
        ExerciseRow(
            entry = entry,
            onEdit = if (entry.origin == ExerciseOrigin.MANUAL) ({ onEdit(entry) }) else null,
            onDelete = { apagar({ onDelete(entry.id) }, { onRestore(entry.id) }) },
        )
    }
}

/**
 * A água do dia, bebida e da comida.
 *
 * A meta é de **água total** desde que passou a sair da referência da EFSA, e por isso a
 * parcela da comida conta para ela. Quando não dá para saber essa parcela — menos de metade
 * do prato com teor de água medido —, só se conta a bebida e o texto di-lo.
 */
@Composable
internal fun WaterCard(
    bebidaMl: Int,
    daComida: AguaDaComida.Resultado,
    metaMl: Int,
    onAdd: (Int) -> Unit,
) {
    AntaresCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(Res.string.diary_water), style = MaterialTheme.typography.titleMedium)
                val medida = (daComida as? AguaDaComida.Resultado.Medida)?.ml
                val total = bebidaMl + (medida ?: 0)
                Text(
                    "$total / $metaMl ml",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (total >= metaMl) {
                        MaterialTheme.success
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    // As duas ausências não se dizem com a mesma frase — ver o
                    // `AguaDaComida`. Num dia sem registo não há comida por medir.
                    when (daComida) {
                        is AguaDaComida.Resultado.Medida ->
                            stringResource(Res.string.today_water_parts, bebidaMl, daComida.ml)
                        AguaDaComida.Resultado.SemCobertura ->
                            stringResource(Res.string.today_water_food_unknown)
                        AguaDaComida.Resultado.SemRegisto ->
                            stringResource(Res.string.today_water_food_no_log)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                TextButton(onClick = { onAdd(-COPO_ML) }) { Text("−$COPO_ML") }
                TextButton(onClick = { onAdd(COPO_ML) }) { Text("+$COPO_ML") }
                TextButton(onClick = { onAdd(GARRAFA_ML) }) { Text("+$GARRAFA_ML") }
            }
        }
    }
}

// Um copo e uma garrafa pequena. São as duas medidas que se reconhecem sem pensar.
private const val COPO_ML = 250
private const val GARRAFA_ML = 500
