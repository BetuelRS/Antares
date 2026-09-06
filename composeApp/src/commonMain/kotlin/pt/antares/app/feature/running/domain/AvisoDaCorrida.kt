package pt.antares.app.feature.running.domain

/**
 * O que a voz tem para dizer, e quando.
 *
 * Puro e sem uma palavra escrita lá dentro: devolve **números**, e quem os transforma numa
 * frase é quem tem os recursos do idioma à mão — o mesmo par que o `TargetBreakdown` e o
 * `TargetBreakdownText` já formam. Um aviso é lido em voz alta, e uma frase montada aqui
 * ficava presa a uma língua.
 */
object AvisoDaCorrida {

    /**
     * O parcial que falta anunciar, ou nulo.
     *
     * **Anuncia o último e salta os do meio.** Um telemóvel que perca o GPS numa passagem
     * inferior fecha dois quilómetros de uma vez, e dizer os dois seguidos é ruído a quem
     * já vai no terceiro. O que interessa é onde se está agora.
     */
    fun porAnunciar(parciais: List<Split>, jaAnunciados: Int): Split? =
        if (parciais.size > jaAnunciados) parciais.last() else null

    /**
     * O ritmo, partido em minutos e segundos, **por quilómetro e em qualquer caso**.
     *
     * É a mesma decisão que a tabela de parciais já tinha tomado, e pela mesma razão: os
     * parciais são medidos e fechados por quilómetro, e passá-los a milhas era recalculá-los
     * a partir do percurso, não mudar um rótulo. A tabela resolve-o dizendo-o no título; uma
     * frase falada resolve-o dizendo **«por quilómetro»** em voz alta, e é o que ela diz.
     *
     * A alternativa — anunciar «quilómetro 3» com o ritmo por milha — punha duas unidades na
     * mesma frase, que é pior do que uma unidade que não é a escolhida. O dia em que a
     * corrida tiver parciais por milha, a voz herda-os sem mudar aqui nada.
     */
    fun ritmo(secPorKm: Int): Ritmo? {
        if (secPorKm <= 0) return null
        return Ritmo(
            minutos = secPorKm / SEGUNDOS_POR_MINUTO,
            segundos = secPorKm % SEGUNDOS_POR_MINUTO,
        )
    }

    /**
     * O tempo em movimento, partido em horas e minutos.
     *
     * Sem segundos, de propósito: são a parte que menos interessa a correr e a que mais
     * alonga a frase — e uma frase longa acaba depois de a pessoa já ter deixado de ouvir.
     */
    fun tempo(movingMs: Long): Tempo {
        val minutosTotais = (movingMs / MS_POR_MINUTO).toInt()
        return Tempo(horas = minutosTotais / MINUTOS_POR_HORA, minutos = minutosTotais % MINUTOS_POR_HORA)
    }

    private const val MS_POR_MINUTO = 60_000L
    private const val MINUTOS_POR_HORA = 60
    private const val SEGUNDOS_POR_MINUTO = 60

    data class Ritmo(val minutos: Int, val segundos: Int)

    data class Tempo(val horas: Int, val minutos: Int)
}
