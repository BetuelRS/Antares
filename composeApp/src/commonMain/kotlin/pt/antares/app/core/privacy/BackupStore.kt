package pt.antares.app.core.privacy

/**
 * O sítio onde as cópias automáticas ficam gravadas. Fora do armazenamento da app de
 * propósito: uma cópia que a desinstalação apaga não protege do caso mais comum de se
 * perderem dados, que é o telemóvel ser trocado ou a app ser reinstalada.
 *
 * Não sabe nada sobre o que vai lá dentro nem sobre quando correr — só escreve, lista e
 * apaga. Quem decide é o [AutoBackup].
 */
expect class BackupStore {

    /** Escreve o ZIP. Devolve o nome com que ficou gravado, ou nulo se não conseguiu. */
    suspend fun write(name: String, entries: Map<String, ByteArray>): String?

    /** As cópias que lá estão, da mais antiga para a mais recente. */
    suspend fun list(): List<String>

    suspend fun delete(name: String)

    /** O caminho a mostrar a quem pergunta onde é que a cópia ficou. */
    fun describe(): String

    /**
     * Falso quando falta a permissão de escrita. Só acontece no Android 9 e anteriores: a
     * partir do 10 a app escreve pelo MediaStore, que não pede permissão nenhuma.
     */
    fun canWrite(): Boolean
}
