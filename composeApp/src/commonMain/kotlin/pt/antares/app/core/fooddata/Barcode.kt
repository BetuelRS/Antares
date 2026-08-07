package pt.antares.app.core.fooddata

object Barcode {

    fun normalize(raw: String?): String? {
        val digits = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!digits.all { it.isDigit() }) return null

        val canonical = when (digits.length) {
            13 -> digits
            12 -> "0$digits"
            8 -> expandUpcEorKeepEan8(digits) ?: return null
            14 -> digits.substring(1).takeIf { it.length == 13 }
            else -> null
        } ?: return null

        return canonical.takeIf { isChecksumValid(it) }
    }

    private fun expandUpcEorKeepEan8(d: String): String? {
        if (isChecksumValid(d)) return d
        val expanded = expandUpcE(d) ?: return null
        return "0$expanded"
    }

    private fun expandUpcE(code: String): String? {
        if (code.length != 8) return null
        val s = code[0]
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

    fun isChecksumValid(code: String): Boolean {
        if (code.length < 8 || !code.all { it.isDigit() }) return false
        val last = code.length - 1
        var sum = 0
        for (i in 0 until last) {
            val weight = if ((last - 1 - i) % 2 == 0) 3 else 1
            sum += (code[i] - '0') * weight
        }
        val expected = (10 - sum % 10) % 10
        return expected == (code[last] - '0')
    }

    fun searchVariants(raw: String?): List<String> {
        val canonical = normalize(raw) ?: return listOfNotNull(raw?.trim()?.takeIf { it.isNotEmpty() })
        val variants = mutableListOf(canonical)

        if (canonical.length == 13 && canonical.startsWith("0")) {
            variants += canonical.substring(1)
        }
        val trimmed = raw?.trim()
        if (trimmed != null && trimmed.isNotEmpty() && trimmed !in variants) variants += trimmed
        return variants
    }
}
