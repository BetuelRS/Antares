package pt.antares.app.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * A paleta. Duas versões completas — escura e clara — em vez de uma derivada da outra: uma
 * cor aclarada por cálculo perde contraste sobre fundo branco, e é o `ThemeAwareColorsTest`
 * que garante que nenhum ecrã fixa uma cor fora daqui.
 *
 * O nome vem da estrela: os tons quentes do vermelho ao âmbar são a identidade da app.
 */
object AntaresColors {

    /**
     * O fundo de tudo, um degrau abaixo do [backgroundDark].
     *
     * É o `--ground` dos esboços. Existe para a atmosfera do [AntaresScaffold] ter de onde
     * partir: sem um preto mais fundo do que o fundo, um brilho por cima do fundo só o
     * aclara, e o que se vê é um borrão em vez de profundidade.
     */
    val groundDark = Color(0xFF07070B)

    val backgroundDark = Color(0xFF0A0A0F)
    val surfaceDark = Color(0xFF12121A)
    val surfaceVariantDark = Color(0xFF1A1A26)
    val primaryDark = Color(0xFFFF5A4A)
    val secondaryDark = Color(0xFFFFB86B)
    val tertiaryDark = Color(0xFF4CC9C0)
    val errorDark = Color(0xFFFF6B6B)

    /**
     * O contorno, e é **azulado e não cinzento**: `#2A3040` contra o `#3A4152` de antes.
     *
     * O cinzento neutro era o resto de uma paleta que não existe — os esboços declaram este,
     * e a diferença lê-se ao lado do fundo, que também puxa ao azul. Um contorno neutro
     * sobre um fundo azulado parece sujo, e é metade da razão de a app parecer cinzenta.
     */
    val outlineDark = Color(0xFF2A3040)

    /** A linha que separa sem se ver — listas, divisórias, o traço por baixo de um campo. */
    val outlineSoftDark = Color(0xFF1F2532)

    /** O branco da app é **quente**, e não o lavanda que o Material assume por omissão. */
    val inkDark = Color(0xFFECE8E4)

    /** O segundo nível de texto. Puxa ao azul, como o fundo — o `--ink-dim` dos esboços. */
    val inkDimDark = Color(0xFFA7ADBB)

    val successDark = Color(0xFF4ADE80)

    /**
     * Os contentores dos acentos: cada um é o seu acento a **catorze por cento** sobre a
     * superfície, e não a superfície outra vez.
     *
     * Parece arrumação e não é. O `contentColorFor` do Material descobre a tinta procurando
     * a cor do fundo na lista de campos do esquema e devolvendo o `on` do **primeiro que
     * casa** — e com estes quatro iguais à superfície, um cartão comum casava primeiro com
     * `primaryContainer` e ficava com o texto todo vermelho. Aconteceu, e vê-se numa
     * captura: o «2 dias seguidos» e o cartão das perguntas por responder ficaram cor de
     * acento sem ninguém lhes ter tocado.
     *
     * De caminho ganham o que deviam ter: um cartão de decisão puxa ao quente, um de aviso
     * puxa ao vermelho, e a diferença lê-se antes de se ler a palavra.
     */
    val primaryContainerDark = Color(0xFF331C21)
    val secondaryContainerDark = Color(0xFF332925)
    val tertiaryContainerDark = Color(0xFF1A2C31)
    val errorContainerDark = Color(0xFF331E25)

    // As claras não são as escuras aclaradas: escurecem-se para manter contraste sobre
    // fundo branco. O mesmo vermelho da versão escura seria ilegível aqui.
    /** O papel por baixo do papel: um degrau mais quente do que o fundo, para a atmosfera. */
    val groundLight = Color(0xFFF4EFE9)

    val backgroundLight = Color(0xFFFAF7F4)
    val surfaceLight = Color(0xFFFFFFFF)
    val surfaceVariantLight = Color(0xFFF0EBE6)
    val primaryLight = Color(0xFFD9402F)
    val secondaryLight = Color(0xFFC77E2A)
    val tertiaryLight = Color(0xFF2A8F87)
    val errorLight = Color(0xFFBA1A1A)
    val outlineLight = Color(0xFFD8D0C8)
    val outlineSoftLight = Color(0xFFEAE3DB)

    /** Tinta quente, da mesma família do fundo. O preto neutro do Material arrefece o papel. */
    val inkLight = Color(0xFF1B1A18)
    val inkDimLight = Color(0xFF5F5B55)

    val successLight = Color(0xFF1E8E4D)

    /** Os mesmos véus do lado claro, a doze por cento sobre o papel. Mesma razão. */
    val primaryContainerLight = Color(0xFFFAE8E6)
    val secondaryContainerLight = Color(0xFFF8EFE5)
    val tertiaryContainerLight = Color(0xFFE5F1F0)
    val errorContainerLight = Color(0xFFF6E3E3)

    // Os macros têm cor própria e igual nos dois temas: é um código que a pessoa aprende a
    // ler nos gráficos, e mudá-lo com o tema obrigava a reaprendê-lo.
    val macroProtein = Color(0xFFFF7A6E)
    val macroCarbs = Color(0xFFFFC15E)
    val macroFat = Color(0xFF9D7BFF)
}
