package pt.antares.app.core.confecao

/**
 * A nutrição por 100 g, no formato mínimo de que esta conta precisa.
 *
 * Não é a entidade da base nem o modelo do ecrã: é um saco de números por 100 g, para a conta
 * poder ser testada sem base de dados e sem Compose. Os [micros] trazem só os que são números
 * — os estados («vestígios», «abaixo do limite») não entram em contas, e é o [microsDeJson]
 * que já os deixa de fora.
 */
data class Por100g(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val sugarsG: Double? = null,
    val satFatG: Double? = null,
    val micros: Map<String, Double> = emptyMap(),
)

/**
 * O que se sabe sobre uma confeção concreta: quanto pesava cru e quanto pesa cozinhado.
 *
 * O [gramasCozinhadas] é o que quem cozinhou pôs na balança. Quando existe, **ganha à
 * tabela** — é uma medição do prato que está à frente da pessoa, e a tabela é uma mediana de
 * cortes que não são este. É esta a razão de a app perguntar em vez de assumir, e é o que a
 * torna mais certa do que qualquer tabela sozinha.
 */
data class Pesagem(val gramasCruas: Double, val gramasCozinhadas: Double? = null)

private const val GRAMAS_DE_REFERENCIA = 100.0

/**
 * Converte a nutrição de cru para cozinhado, por 100 g do resultado.
 *
 * A fórmula é a do próprio USDA, e tem duas partes que só juntas dão o número certo:
 *
 *     nutriente por 100 g cozinhado = nutriente por 100 g cru × retenção ÷ rendimento
 *
 * **A divisão pelo rendimento é a parte que se esquece.** Cozer espinafres perde vitamina C
 * para a água *e* perde água: contar só a primeira coisa dá um valor mais errado do que não
 * fazer conta nenhuma, porque o que sobra fica mais concentrado do que estava.
 *
 * As kcal e os macros **não têm factor de retenção** — a proteína e a gordura não desaparecem
 * ao lume, mudam de sítio. O que lhes acontece é só a concentração: 100 g de carne que perde
 * 23 % do peso passam a ter os mesmos nutrientes em 77 g. A gordura é a excepção conhecida
 * (escorre da grelha), e a tabela de rendimentos publica essa perda — mas por corte, e não
 * por família, o que a torna um número que não se pode aplicar aqui sem inventar.
 *
 * Devolve nulo quando não há como saber o rendimento: nem tabela nem balança. Nulo é a
 * resposta honesta, e é o que faz o ecrã pedir o peso em vez de mostrar um número.
 */
fun cozinhar(cru: Por100g, linha: LinhaDeConfecao, pesagem: Pesagem? = null): Por100g? {
    val rendimento = rendimentoDe(linha, pesagem) ?: return null
    if (rendimento <= 0.0) return null

    val concentracao = 1.0 / rendimento

    return Por100g(
        kcal = cru.kcal * concentracao,
        proteinG = cru.proteinG * concentracao,
        carbsG = cru.carbsG * concentracao,
        fatG = cru.fatG * concentracao,
        sugarsG = cru.sugarsG?.times(concentracao),
        satFatG = cru.satFatG?.times(concentracao),
        micros = cru.micros.mapValues { (chave, valor) ->
            valor * (linha.retencoes[chave] ?: 1.0) * concentracao
        },
    )
}

/**
 * O rendimento a usar: o medido, se alguém pesou; o da tabela, se não.
 *
 * Uma pesagem sem gramas cruas, ou com gramas cozinhadas maiores do que o dobro das cruas,
 * não se aceita: é quase sempre um engano de digitação, e um rendimento de 300 % dividia a
 * nutriente por três sem ninguém desconfiar do resultado.
 */
private const val RENDIMENTO_MAXIMO_CREDIVEL = 2.0

internal fun rendimentoDe(linha: LinhaDeConfecao, pesagem: Pesagem?): Double? {
    val medido = pesagem
        ?.takeIf { it.gramasCruas > 0 && (it.gramasCozinhadas ?: 0.0) > 0 }
        ?.let { it.gramasCozinhadas!! / it.gramasCruas }
        ?.takeIf { it <= RENDIMENTO_MAXIMO_CREDIVEL }

    return medido ?: linha.rendimento
}

/**
 * Quantas gramas de cozinhado saem de uma quantidade de cru — para quem regista o que pesou
 * antes de pôr ao lume, que é quando a balança está na mão.
 */
fun gramasDepoisDeCozinhar(gramasCruas: Double, linha: LinhaDeConfecao): Double? =
    linha.rendimento?.let { gramasCruas * it }

/** O mesmo ao contrário: o que está no prato pesa isto, e era isto em cru. */
fun gramasAntesDeCozinhar(gramasCozinhadas: Double, linha: LinhaDeConfecao): Double? =
    linha.rendimento?.takeIf { it > 0 }?.let { gramasCozinhadas / it }

/** A nutrição de uma porção concreta, para o ecrã não ter de repetir a regra de três. */
fun porcaoDe(por100g: Por100g, gramas: Double): Por100g {
    val f = gramas / GRAMAS_DE_REFERENCIA
    return Por100g(
        kcal = por100g.kcal * f,
        proteinG = por100g.proteinG * f,
        carbsG = por100g.carbsG * f,
        fatG = por100g.fatG * f,
        sugarsG = por100g.sugarsG?.times(f),
        satFatG = por100g.satFatG?.times(f),
        micros = por100g.micros.mapValues { it.value * f },
    )
}
