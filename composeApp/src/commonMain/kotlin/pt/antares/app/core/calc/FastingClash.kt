package pt.antares.app.core.calc

/**
 * O cruzamento entre o jejum e o diário.
 *
 * A app tinha as duas coisas e elas não se conheciam: era possível ter um jejum a correr no
 * ecrã e três refeições registadas por baixo dele, sem que nada o dissesse. Não é para
 * repreender ninguém — é para o contador de horas não continuar a subir como se nada
 * tivesse acontecido.
 */
object FastingClash {

    /**
     * Os registos que caem entre o início do jejum e agora.
     *
     * Recebe instantes já resolvidos, e não linhas da base, porque converter dia e minuto
     * local em instante depende do fuso — e essa conversão é do lado de quem lê a base, não
     * desta conta. Registos sem hora não entram: não se sabe se foram antes ou depois, e
     * chamar-lhes quebra seria acusar sem prova.
     */
    fun dentroDoJejum(instantesMs: List<Long>, inicioMs: Long, agoraMs: Long): List<Long> =
        instantesMs.filter { it in inicioMs..agoraMs }.sorted()
}
