package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.util.dayShort

/**
 * Sete quadrados, um por dia da semana, cheios nos dias que aconteceram.
 *
 * Nasceu no relatório do treinador, que dizia «registaste 5 de 7 dias» e mais nada: cinco
 * dias seguidos e cinco dias alternados são semanas diferentes, e o número não os distingue
 * — a forma distingue.
 *
 * Está aqui, e não dentro de um ecrã, porque é o mesmo vocabulário no treinador e no centro
 * de treino, e duas cópias divergiam à primeira correcção.
 *
 * A semana começa sempre à segunda: é a semana ISO que o `weekStartEpochDay` dá, e é o que
 * faz este componente, a grelha do progresso e o orçamento semanal concordarem.
 */
@Composable
fun SemanaEmPontos(
    inicioEpochDay: Long,
    diasMarcados: List<Long>,
    modifier: Modifier = Modifier,
    titulo: String? = null,
    /** O dia de hoje, para o contornar. Nulo numa semana passada, onde «hoje» não é lá. */
    hoje: Long? = null,
) {
    val marcado = MaterialTheme.colorScheme.primary

    // O dia vazio é o contorno e não o `surfaceVariant`: desde a paleta da 2.18.2 este vale
    // `#1A1A26`, que é a própria cor do cartão — os sete quadrados desapareciam, e uma semana
    // sem nenhum dia marcado ficava a ser sete letras sozinhas. Nenhum teste vê cor.
    val vazio = MaterialTheme.colorScheme.outline

    Column(modifier = modifier) {
        titulo?.let {
            Text(it, style = MaterialTheme.typography.titleSmall)
        }
        Row(
            modifier = Modifier.fillMaxWidth().let {
                if (titulo != null) it.padding(top = Spacing.sm) else it
            },
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            for (i in 0 until DIAS_DA_SEMANA) {
                val dia = inicioEpochDay + i
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(CELULA_DP.dp)
                            .clip(RoundedCornerShape(CANTO_DP.dp))
                            .background(if (dia in diasMarcados) marcado else vazio),
                    )
                    Text(
                        dayShort(dia).take(INICIAL_DO_DIA),
                        style = MaterialTheme.typography.labelSmall,
                        // O dia de hoje escreve-se na cor da app: sem isto, sete iniciais
                        // iguais não dizem em que ponto da semana se está.
                        color = if (dia == hoje) marcado else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val DIAS_DA_SEMANA = 7
private const val CELULA_DP = 28
private const val CANTO_DP = 4
private const val INICIAL_DO_DIA = 1
