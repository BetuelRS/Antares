package pt.antares.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import pt.antares.app.core.designsystem.Spacing
import pt.antares.app.core.designsystem.larguraDeLeitura
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.common_back

@Composable
fun AntaresScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},

    // Aqui porque trinta e dois ecrãs usavam o `Scaffold` cru e dois deles tinham botão
    // flutuante: sem este parâmetro, migrá-los obrigava a deixá-los de fora — e um andaime
    // partilhado com exceções não é partilhado.
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        // O `imePadding` está aqui e não em cada ecrã: é o que faz o conteúdo subir com o
        // teclado em vez de ficar escondido por baixo dele. O `KeyboardInsetsTest` falha se
        // um ecrã com campos de texto usar outro scaffold que não este.
        modifier = modifier.imePadding().atmosfera(),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,

        // Transparente para a atmosfera se ver. O andaime pintava por cima dela com o fundo
        // opaco, e o brilho ficava escondido debaixo do próprio ecrã.
        //
        // **A tinta tem de vir escrita a seguir**, e isso não é detalhe: o `Scaffold` deriva
        // a cor do conteúdo da cor do contentor, e de uma cor transparente não se deriva
        // nenhuma. Sem esta linha o número grande do dia saía preto sobre preto — o que
        // aconteceu, e só se viu no aparelho.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = content,
    )
}

/**
 * O ar por trás de todos os ecrãs: dois brilhos muito fracos sobre o preto mais fundo da
 * paleta — a primária em cima à esquerda, o âmbar em cima à direita.
 *
 * **É o fundo dos próprios esboços**, e é a diferença entre um preto chapado e um preto com
 * profundidade. Nos esboços ele vive na página à volta do telemóvel desenhado; aqui não há
 * página à volta — a app é a superfície toda —, e por isso entra atrás dela.
 *
 * As opacidades são as do `estilo.css` e não umas escolhidas agora: 10 % e 7 %. Acima disso
 * deixa de ser atmosfera e passa a ser uma cor, e uma cor no fundo de todos os ecrãs compete
 * com a única coisa que nesta app tem direito a cor forte — o que a pessoa tem de decidir.
 */
private fun Modifier.atmosfera(): Modifier = composed {
    val ground = MaterialTheme.colorScheme.surfaceContainerLowest
    val quente = MaterialTheme.colorScheme.primary
    val morno = MaterialTheme.colorScheme.secondary

    drawBehind {
        drawRect(ground)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(quente.copy(alpha = BRILHO_QUENTE), Color.Transparent),
                center = Offset(size.width * 0.15f, -size.height * 0.02f),
                radius = size.width * RAIO,
            ),
            radius = size.width * RAIO,
            center = Offset(size.width * 0.15f, -size.height * 0.02f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(morno.copy(alpha = BRILHO_MORNO), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.04f),
                radius = size.width * RAIO_MORNO,
            ),
            radius = size.width * RAIO_MORNO,
            center = Offset(size.width * 0.92f, size.height * 0.04f),
        )
    }
}

private const val BRILHO_QUENTE = 0.10f
private const val BRILHO_MORNO = 0.07f
private const val RAIO = 1.1f
private const val RAIO_MORNO = 0.9f

/**
 * Um ecrã inteiro: andaime, rolagem, largura de leitura e margem, por omissão.
 *
 * Nasceu porque **trinta e dois ecrãs usavam o `Scaffold` cru** — e o `imePadding` vive no
 * [AntaresScaffold], o que quer dizer que nesses trinta e dois o teclado tapava o conteúdo em
 * vez de o empurrar. E porque **trinta e sete escreviam `larguraDeLeitura()` à mão**: um ecrã
 * novo que se esquecesse esticava-se por 1200 dp num tablet, e ninguém dava por isso até
 * alguém abrir a app num.
 *
 * Isto é para os ecrãs que são uma coluna de conteúdo. Um ecrã que seja uma lista continua a
 * usar o [AntaresScaffold] e a pôr a largura na própria lista — numa lista preguiçosa a
 * largura tem de ir no conteúdo, não à volta dela, senão a rolagem fica presa a uma coluna
 * estreita com fundo aos lados.
 */
@Composable
fun AntaresScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},

    // O espaço entre filhos e a margem à volta entram por parâmetro porque há ecrãs que os
    // querem a zero — um que desenhe as suas próprias secções coladas, por exemplo. O que
    // não entra por parâmetro é a largura de leitura: essa é a regra.
    espaco: Dp = Spacing.md,
    // `PaddingValues` e não `Dp`: há ecrãs com margem só nos lados, e um parâmetro que só
    // sabe pôr a mesma margem nos quatro obrigava-os a ficar de fora.
    margem: PaddingValues = PaddingValues(Spacing.lg),
    rolavel: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    AntaresScaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (rolavel) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .larguraDeLeitura()
                .padding(margem),
            verticalArrangement = Arrangement.spacedBy(espaco),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntaresTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {

                    // Versão espelhada da seta: em idiomas escritos da direita para a
                    // esquerda ela tem de apontar ao contrário.
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.common_back),
                    )
                }
            }
        },
        actions = actions,

        // Transparente, para a atmosfera atravessar a barra em vez de bater nela. É onde o
        // brilho é mais forte — nasce em cima —, e uma faixa opaca no topo cortava-o
        // exactamente onde ele existe.
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}
