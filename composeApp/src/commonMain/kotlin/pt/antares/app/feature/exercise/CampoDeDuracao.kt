package pt.antares.app.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.exercise_duration
import pt.antares.app.generated.resources.exercise_duration_less
import pt.antares.app.generated.resources.exercise_duration_more
import pt.antares.app.generated.resources.exercise_min

/**
 * A duração de uma atividade, escrevível.
 *
 * Antes eram só um `−5` e um `+5`: uma aula de 50 minutos custava dez toques a partir dos
 * trinta iniciais, e um treino de 22 não se registava de todo. O campo é a peça principal;
 * o `−`/`+` fica como acessório para acertar, e os quatro atalhos cobrem as durações que se
 * repetem.
 *
 * Vive fora do ecrã de registar porque o diálogo de corrigir usa o mesmo controlo — e um
 * segundo controlo de duração com outras regras era a forma de os dois divergirem.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampoDeDuracao(
    durationMin: Int,
    onDuration: (Int) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // O campo guarda texto e não um número: a meio de escrever «45» passa-se por «4», e um
    // estado numérico já teria corrigido o «4» para o mínimo antes do segundo algarismo.
    var texto by remember { mutableStateOf(durationMin.toString()) }

    // O que vem de fora — os atalhos, o `−` e o `+` — reescreve o campo. O que se escreve no
    // campo não: o `!=` impede o ciclo de a escrita se corrigir a si própria.
    LaunchedEffect(durationMin) {
        if (texto.toIntOrNull() != durationMin) texto = durationMin.toString()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // O rótulo tem linha própria e o campo cresce com o que sobra, em vez de os três
        // ficarem lado a lado com uma largura fixa. A 200 % de escala de letra a versão
        // lado-a-lado empurrava o `+` para fora do ecrã — é o risco que o
        // `transversal/03-acessibilidade.md` nomeia para o `NewSetRow`, e visto no emulador.
        Text(stringResource(Res.string.exercise_duration), style = MaterialTheme.typography.bodyLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onStep(-PASSO) }) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = stringResource(Res.string.exercise_duration_less, PASSO),
                )
            }
            OutlinedTextField(
                value = texto,
                onValueChange = { escrito ->
                    texto = escrito.filter { it.isDigit() }.take(DIGITOS)
                    val min = texto.toIntOrNull()
                    // Campo vazio não é zero minutos: é uma frase a meio. Fica sem
                    // avisar ninguém até haver um número, e o botão de gravar continua
                    // a olhar para o último válido.
                    if (min != null && min > 0) {
                        val travado = min.coerceAtMost(ExerciseRepository.MAX_DURATION_MIN)
                        if (travado != min) texto = travado.toString()
                        onDuration(travado)
                    }
                },
                suffix = { Text(stringResource(Res.string.exercise_min)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onStep(PASSO) }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(Res.string.exercise_duration_more, PASSO),
                )
            }
        }

        // Em fila que quebra, e não numa linha só: a 200 % o «60» ficava partido em dois
        // algarismos, um por linha, encostado à margem do diálogo.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            for (atalho in ATALHOS) {
                FilterChip(
                    selected = durationMin == atalho,
                    onClick = { onDuration(atalho) },
                    label = { Text("$atalho") },
                )
            }
        }
    }
}

// Quinze em quinze é o que as aulas e os treinos duram; o `−`/`+` de cinco fica para o resto.
private val ATALHOS = listOf(15, 30, 45, 60)

private const val PASSO = 5

// Quatro algarismos chegam para os 600 minutos que o repositório aceita, e travam quem cole
// um número de telefone no campo.
private const val DIGITOS = 4

