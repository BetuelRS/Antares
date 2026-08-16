package pt.antares.app.core.calc

/**
 * Porque é que uma data de ciclo foi recusada.
 *
 * O ecrã só deixava marcar «hoje», e por isso não havia nada que recusar. Com datas à
 * escolha há: e a razão tem de chegar ao ecrã, porque um botão que não faz nada é pior do
 * que um botão que explica.
 */
enum class CycleDateError {
    /** Um período que ainda não aconteceu não se regista. */
    NO_FUTURO,

    /** Já existe um período que cobre este dia. */
    SOBREPOE,

    /** O fim é antes do início. */
    FIM_ANTES_DO_INICIO,
}

/**
 * A validação das datas do ciclo, sem base de dados nem relógio: recebe o que existe e o
 * que se quer marcar, e devolve a razão da recusa ou `null`.
 */
object CycleEdit {

    /**
     * Um período, reduzido ao que a validação precisa de saber. O fim a `null` é o período
     * a decorrer — que ocupa desde o início até hoje, e não um dia só.
     */
    data class Periodo(val id: String, val start: Long, val end: Long?)

    fun validateStart(
        novoInicio: Long,
        hoje: Long,
        existentes: List<Periodo>,
        aIgnorar: String? = null,
    ): CycleDateError? {
        if (novoInicio > hoje) return CycleDateError.NO_FUTURO
        val choca = existentes
            .filter { it.id != aIgnorar }
            .any { novoInicio in it.start..(it.end ?: hoje) }
        return if (choca) CycleDateError.SOBREPOE else null
    }

    fun validateEnd(
        inicio: Long,
        novoFim: Long,
        hoje: Long,
        existentes: List<Periodo>,
        aIgnorar: String? = null,
    ): CycleDateError? {
        if (novoFim > hoje) return CycleDateError.NO_FUTURO
        if (novoFim < inicio) return CycleDateError.FIM_ANTES_DO_INICIO

        // Um fim que passe por cima do início seguinte engolia o ciclo a seguir, e a
        // duração média passava a contar um ciclo que deixou de existir.
        val seguinte = existentes
            .filter { it.id != aIgnorar && it.start > inicio }
            .minByOrNull { it.start }
        if (seguinte != null && novoFim >= seguinte.start) return CycleDateError.SOBREPOE
        return null
    }
}
