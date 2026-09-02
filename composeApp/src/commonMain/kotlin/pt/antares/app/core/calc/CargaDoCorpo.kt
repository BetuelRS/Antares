package pt.antares.app.core.calc

/**
 * A carga de uma série de peso do corpo.
 *
 * **Cento e onze exercícios do catálogo não se conseguiam registar** — flexões, dominadas,
 * fundos, prancha —, porque a validação exige um peso maior do que zero e uma flexão não tem
 * peso para escrever. Quem escrevesse `1` para contornar o botão cinzento envenenava o mapa
 * de volume e ficava com um «recorde» de 1,4 kg naquele exercício.
 *
 * O que entra na série continua a ser **a carga total em quilos**, como sempre foi: o
 * `VolumeCalc`, o `OneRepMax` e o `PrDetector` leem o mesmo campo que liam e não precisam de
 * saber de onde ele veio. É isso que evita o que a `estudo/propostas/00-o-custo-de-mudar.md`
 * nomeia como o custo desta funcionalidade — *«dois significados de `weightKg` para sempre»*.
 *
 * **A app não inventa a fração.** Os «cerca de 65 % numa flexão» do `motor/05` são uma
 * aproximação sem fonte no repositório, e cento e onze frações inventadas seriam cento e onze
 * afirmações sem prova. A percentagem é da pessoa, começa nos 100 %, e o ecrã mostra a conta.
 */
object CargaDoCorpo {

    /** O peso todo. Quem quiser uma flexão a 65 % põe-na a 65 %, e a app mostra a conta. */
    const val PERCENTAGEM_POR_OMISSAO = 100

    /** Acima de 100 % existe: uma pessoa com colete lastrado não é um caso absurdo. */
    const val PERCENTAGEM_MAXIMA = 200

    /** O valor que o catálogo de exercícios usa. Ver `WorkoutTaxonomy`. */
    const val EQUIPAMENTO_DO_CORPO = "body only"

    data class Carga(
        /** O que vai para o `weightKg` da série. */
        val totalKg: Double,
        /** O que vai para o `bodyweightKg`: a parte que veio do corpo, e não do cinto. */
        val doCorpoKg: Double,
    )

    /**
     * `null` quando não há peso registado: é uma pergunta sem resposta, e não um zero. Devolver
     * zero fazia a app gravar uma série de 0 kg — que é o que a validação recusa, e sem dizer
     * porquê. Quem nunca se pesou tem de se pesar primeiro, e o ecrã di-lo.
     */
    fun calcular(pesoDoCorpoKg: Double?, percentagem: Int, adicionalKg: Double): Carga? {
        if (pesoDoCorpoKg == null || pesoDoCorpoKg <= 0.0) return null
        if (percentagem !in 1..PERCENTAGEM_MAXIMA) return null
        val doCorpo = pesoDoCorpoKg * percentagem / 100.0
        return Carga(totalKg = doCorpo + adicionalKg, doCorpoKg = doCorpo)
    }

    /** Sem equipamento declarado não se assume nada: o catálogo tem 77 linhas assim. */
    fun eDePesoDoCorpo(equipamento: String?): Boolean = equipamento == EQUIPAMENTO_DO_CORPO
}
