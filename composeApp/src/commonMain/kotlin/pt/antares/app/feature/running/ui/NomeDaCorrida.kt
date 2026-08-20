package pt.antares.app.feature.running.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.util.currentHour
import pt.antares.app.feature.running.domain.ActivityType

/**
 * O nome que a app propõe para uma corrida acabada de fazer: «Corrida da manhã».
 *
 * É uma sugestão e não um nome imposto — fica escrito no campo e apaga-se. Sem ela, o campo
 * abria vazio e quase ninguém escrevia nada: o detalhe de todas as corridas chamava-se
 * «Resumo» e o GPX exportado saía sem nome nenhum.
 *
 * A hora é a de agora e não a do início: o resumo aparece logo a seguir a acabar, e uma
 * corrida que atravesse a fronteira de dois períodos é mais rara do que o custo de arrastar
 * o instante do arranque até aqui.
 */
@Composable
fun nomeSugeridoDaCorrida(tipo: ActivityType, hora: Int = currentHour()): String =
    stringResource(rotuloDoPeriodo(periodoDoDia(hora)), stringResource(activityLabel(tipo)))
