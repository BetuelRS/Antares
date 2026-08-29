package pt.antares.app.feature.templates

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.components.AntaresScaffold
import pt.antares.app.core.designsystem.components.AntaresTopBar
import pt.antares.app.core.designsystem.components.EmptyState
import pt.antares.app.core.designsystem.components.LinhaDaLista
import pt.antares.app.core.designsystem.components.ListaAdaptavel
import pt.antares.app.core.designsystem.components.linhaInteira
import pt.antares.app.core.util.TextNormalize
import pt.antares.app.feature.fooddata.RefeicaoGuardada
import pt.antares.app.feature.fooddata.subtituloDaRefeicao
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.recipe_new
import pt.antares.app.generated.resources.recipe_sem_nome
import pt.antares.app.generated.resources.refeicoes_procurar
import pt.antares.app.generated.resources.search_your_meals
import pt.antares.app.generated.resources.templates_empty_title

/**
 * As refeições já montadas, num sítio que é delas.
 *
 * **É a proposta 5 do esboço 05**, e a frase dele é a razão: «um sítio próprio, alcançável do
 * "Eu" e do diário — não enterrado dentro da pesquisa de comida». Vinte refeições guardadas
 * dentro de um separador de um ecrã que existe para outra coisa é o que a área 05 chama estar
 * mal arrumado, e foi a queixa que lhe custou a nota da forma.
 *
 * **A lista continua a aparecer também dentro do «Tudo» da pesquisa**, e isso não é a
 * duplicação que o estudo condena: lá é para registar depressa o que já se montou, e aqui é
 * para tratar delas — mudar o nome, apagar, montar uma nova. São dois verbos, e o esboço 03
 * desenha o primeiro enquanto o 05 desenha o segundo.
 */
@Composable
fun MinhasRefeicoesScreen(
    onAbrirReceita: (String) -> Unit,
    onNovaReceita: () -> Unit,
    onBack: () -> Unit,
    viewModel: MinhasRefeicoesViewModel = koinViewModel(),
) {
    val refeicoes by viewModel.refeicoes.collectAsState()
    var procura by rememberSaveable { mutableStateOf("") }

    // A caixa de procura que o esboço desenha dentro da lista. Filtra o que já está em
    // memória e não vai à base: são as refeições de uma pessoa, e nunca são muitas.
    val visiveis = remember(refeicoes, procura) {
        val termo = TextNormalize.normalize(procura.trim())
        if (termo.isEmpty()) {
            refeicoes
        } else {
            refeicoes.filter { TextNormalize.normalize(it.nome).contains(termo) }
        }
    }

    AntaresScaffold(
        topBar = {
            AntaresTopBar(
                title = stringResource(Res.string.search_your_meals),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNovaReceita) {
                        Icon(Icons.Default.Add, stringResource(Res.string.recipe_new))
                    }
                },
            )
        },
    ) { padding ->
        ListaAdaptavel(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(0.dp),
            espaco = 0.dp,
        ) {
            linhaInteira {
                OutlinedTextField(
                    value = procura,
                    onValueChange = { procura = it },
                    label = { Text(stringResource(Res.string.refeicoes_procurar)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                )
            }

            if (visiveis.isEmpty()) {
                linhaInteira {
                    EmptyState(title = stringResource(Res.string.templates_empty_title))
                }
            }

            items(visiveis, key = { it.chave }) { refeicao ->
                LinhaDaLista(
                    titulo = refeicao.nome.ifBlank { stringResource(Res.string.recipe_sem_nome) },
                    subtitulo = subtituloDaRefeicao(refeicao),
                    onClick = {
                        when (refeicao) {
                            is RefeicaoGuardada.DeIngredientes ->
                                onAbrirReceita(refeicao.resumo.recipe.id)
                            // Uma refeição guardada não tem ecrã próprio: o que se faz com
                            // ela é ver, escalar e aplicar, e isso é a folha da pesquisa.
                            is RefeicaoGuardada.DoDiario -> Unit
                        }
                    },
                    aoLado = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            // Decorativo: a linha inteira é o alvo.
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
        }
    }
}
