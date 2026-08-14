package pt.antares.app.core.fooddata

/**
 * Códigos de barras de alimentos, reduzidos a uma forma só. O leitor devolve o que o
 * símbolo tem, e o mesmo produto aparece com comprimentos diferentes conforme a origem —
 * sem esta normalização, procurar por um código lido nunca encontrava o mesmo produto.
 */
object Barcode {

    /** Devolve o código em 13 dígitos, ou null se não for um código de barras válido. */
    fun normalize(raw: String?): String? {
        val digits = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!digits.all { it.isDigit() }) return null

        // Os quatro comprimentos que existem em produtos alimentares. O de 12 é americano e
        // ganha um zero à frente; o de 14 é de embalagem de transporte e perde o primeiro
        // dígito, que só diz quantas unidades vêm na caixa.
        val canonical = when (digits.length) {
            13 -> digits
            12 -> "0$digits"
            8 -> expandUpcEorKeepEan8(digits) ?: return null
            14 -> digits.substring(1).takeIf { it.length == 13 }
            else -> null
        } ?: return null

        // O dígito de controlo é a defesa contra leituras erradas: um código mal lido tem
        // de falhar aqui em vez de ir procurar um produto que não existe.
        return canonical.takeIf { isChecksumValid(it) }
    }

    /**
     * Oito dígitos são ambíguos: podem ser um código curto europeu, que já é válido tal
     * como está, ou um americano comprimido, que precisa de ser expandido. O dígito de
     * controlo desempata — só o europeu o tem certo nesta forma.
     */
    private fun expandUpcEorKeepEan8(d: String): String? {
        if (isChecksumValid(d)) return d
        val expanded = expandUpcE(d) ?: return null
        return "0$expanded"
    }

    /**
     * Repõe os zeros que a compressão americana tira. O último dígito do corpo é que diz
     * onde eles estavam, e por isso o `when` tem de cobrir os quatro esquemas da norma.
     */
    private fun expandUpcE(code: String): String? {
        if (code.length != 8) return null
        val s = code[0]
        // Só estes dois sistemas admitem a forma comprimida.
        if (s != '0' && s != '1') return null
        val d = code.substring(1, 7)
        val check = code[7]

        val body = when (d[5]) {
            '0', '1', '2' -> "${d[0]}${d[1]}${d[5]}0000${d[2]}${d[3]}${d[4]}"
            '3' -> "${d[0]}${d[1]}${d[2]}00000${d[3]}${d[4]}"
            '4' -> "${d[0]}${d[1]}${d[2]}${d[3]}00000${d[4]}"
            else -> "${d[0]}${d[1]}${d[2]}${d[3]}${d[4]}0000${d[5]}"
        }
        return "$s$body$check"
    }

    /** Soma ponderada 1-3 a partir da direita, como manda a norma GS1. */
    fun isChecksumValid(code: String): Boolean {
        if (code.length < 8 || !code.all { it.isDigit() }) return false
        val last = code.length - 1
        var sum = 0
        for (i in 0 until last) {
            // O peso conta-se da direita para a esquerda, e não do início: é o que faz esta
            // conta funcionar tanto para códigos de 8 como de 13 dígitos.
            val weight = if ((last - 1 - i) % 2 == 0) 3 else 1
            sum += (code[i] - '0') * weight
        }
        val expected = (10 - sum % 10) % 10
        return expected == (code[last] - '0')
    }

    /**
     * As formas por que vale a pena procurar o mesmo produto. A app normaliza, mas a Open
     * Food Facts guarda o código como quem o introduziu o escreveu.
     */
    fun searchVariants(raw: String?): List<String> {
        // Código inválido continua a procurar-se tal como veio: pode ser um código interno
        // de uma cadeia de supermercados, que não segue a norma mas existe na base.
        val canonical = normalize(raw) ?: return listOfNotNull(raw?.trim()?.takeIf { it.isNotEmpty() })
        val variants = mutableListOf(canonical)

        // Um código americano guardado sem o zero à frente é o caso mais comum de todos.
        if (canonical.length == 13 && canonical.startsWith("0")) {
            variants += canonical.substring(1)
        }
        val trimmed = raw?.trim()
        if (trimmed != null && trimmed.isNotEmpty() && trimmed !in variants) variants += trimmed
        return variants
    }
}
