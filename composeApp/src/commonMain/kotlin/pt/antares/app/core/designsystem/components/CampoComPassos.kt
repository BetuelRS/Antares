package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.stepper_less
import pt.antares.app.generated.resources.stepper_more

/**
 * Um número que se acerta com `−` e `+`, e que também se pode escrever.
 *
 * **É a forma que o campo de duração do exercício já tinha**, generalizada: rótulo em linha
 * própria, campo a crescer com o que sobra, e os dois botões nas pontas. Lado a lado com
 * larguras fixas, a 200 % de escala de letra o `+` sai do ecrã — é o caso que o
 * `estudo/transversal/03-acessibilidade.md` §3.1 nomeia.
 *
 * O campo guarda **texto e não um número**: a meio de escrever «45» passa-se por «4», e um
 * estado numérico já teria corrigido o «4» para o mínimo antes do segundo algarismo.
 *
 * As descrições dos botões levam o nome do campo. «Menos um» sem dizer um de quê foi um
 * defeito a sério, apanhado na 2.19.0 nos botões da duração.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampoComPassos(
    rotulo: String,
    valor: Int,
    passo: Int,
    intervalo: IntRange,
    onValor: (Int) -> Unit,
    modifier: Modifier = Modifier,
    sufixo: String? = null,
    atalhos: List<Int> = emptyList(),
) {
    var texto by remember { mutableStateOf(valor.toString()) }

    // O que vem de fora — os atalhos, o `−` e o `+` — reescreve o campo. O que se escreve no
    // campo não: o `!=` impede o ciclo de a escrita se corrigir a si própria.
    LaunchedEffect(valor) {
        if (texto.toIntOrNull() != valor) texto = valor.toString()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyLarge)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onValor((valor - passo).coerceIn(intervalo)) },
                enabled = valor > intervalo.first,
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = stringResource(Res.string.stepper_less, rotulo, passo),
                )
            }
            OutlinedTextField(
                value = texto,
                onValueChange = { escrito ->
                    texto = escrito.filter { it.isDigit() }.take(DIGITOS)
                    // Campo vazio não é zero: é uma frase a meio. Fica assim até haver um
                    // número, e o valor de fora continua a ser o último válido.
                    texto.toIntOrNull()?.let { onValor(it.coerceIn(intervalo)) }
                },
                suffix = sufixo?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onValor((valor + passo).coerceIn(intervalo)) },
                enabled = valor < intervalo.last,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(Res.string.stepper_more, rotulo, passo),
                )
            }
        }

        // Em fila que quebra, e não numa linha só: a 200 % um número de dois algarismos ficava
        // partido em dois, um por linha, encostado à margem.
        if (atalhos.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                for (atalho in atalhos) {
                    FilterChip(
                        selected = valor == atalho,
                        onClick = { onValor(atalho) },
                        label = { Text("$atalho") },
                    )
                }
            }
        }
    }
}

private const val DIGITOS = 4
