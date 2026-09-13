package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.util.dayShort
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.diary_week_day_empty
import pt.antares.app.generated.resources.diary_week_day_logged

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
 *
 * O `onDiaClick` é opcional e por omissão nulo: o treinador e o centro de treino só mostram a
 * semana, e ganhar um toque que não faz nada seria pior do que não ter toque. Com ele — só no
 * diário —, cada dia passa a alvo de 48 dp e diz ao leitor de ecrã se tem registo e se está
 * aberto; as duas frases de estado são por isso as do diário.
 */
@Composable
fun SemanaEmPontos(
    inicioEpochDay: Long,
    diasMarcados: List<Long>,
    modifier: Modifier = Modifier,
    titulo: String? = null,
    /** O dia de hoje, cuja inicial se escreve na cor da app. Nulo numa semana passada. */
    hoje: Long? = null,
    /** O dia que está aberto, quando os dias se tocam — o leitor de ecrã diz qual é. */
    aberto: Long? = null,
    onDiaClick: ((Long) -> Unit)? = null,
) {
    val marcado = MaterialTheme.colorScheme.primary

    // O dia vazio é o contorno e não o `surfaceVariant`: desde a paleta da 2.18.2 este vale
    // `#1A1A26`, que é a própria cor do cartão — os sete quadrados desapareciam, e uma semana
    // sem nenhum dia marcado ficava a ser sete letras sozinhas. Nenhum teste vê cor.
    val vazio = MaterialTheme.colorScheme.outline
    val comRegisto = stringResource(Res.string.diary_week_day_logged)
    val semRegisto = stringResource(Res.string.diary_week_day_empty)

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
                val rotulo = dayShort(dia)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = if (onDiaClick != null) {
                        // Quando os dias se tocam, cada um é um alvo de dedo com a largura que
                        // lhe cabe, e não os 28 dp do quadrado encostados à esquerda. E o leitor
                        // de ecrã ouve o estado — que dia tem registo dizia-se só pela cor — e
                        // qual está aberto.
                        Modifier
                            .weight(1f)
                            .heightIn(min = ALVO_MINIMO_DP.dp)
                            .clickable(role = Role.Button) { onDiaClick(dia) }
                            .semantics {
                                contentDescription = rotulo
                                stateDescription = if (dia in diasMarcados) comRegisto else semRegisto
                                selected = dia == aberto
                            }
                    } else {
                        Modifier
                    },
                ) {
                    Box(
                        Modifier
                            .size(CELULA_DP.dp)
                            .clip(RoundedCornerShape(CANTO_DP.dp))
                            .background(if (dia in diasMarcados) marcado else vazio),
                    )
                    Text(
                        rotulo.take(INICIAL_DO_DIA),
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

// O mínimo de um alvo de toque no Android, e o que o `estudo/transversal/03` §3.2 pede.
private const val ALVO_MINIMO_DP = 48
