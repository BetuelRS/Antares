package pt.antares.app.core.catalogo

/**
 * O sítio onde o catálogo descarregado fica, dentro do armazenamento privado da app.
 *
 * Três ficheiros e nunca mais do que três: o instalado, o provisório enquanto desce, e o
 * anterior enquanto o novo ainda não provou que abre. Não sabe nada sobre versões, resumos
 * nem rede — quem decide é o [ActualizadorDoCatalogo].
 *
 * **A regra que governa esta classe:** nenhum caminho de falha pode deixar a app sem
 * catálogo. É por isso que a troca é feita por mudança de nome — que o sistema de ficheiros
 * faz de uma vez, ou não faz — e que o [ler] repara uma troca interrompida em vez de
 * devolver nada.
 */
expect class ArmazemDoCatalogo {

    /**
     * O catálogo instalado, ou nulo se ainda não desceu nenhum. Se encontrar o instalado em
     * falta e o anterior no sítio, repõe o anterior: é o estado que fica se a app morrer
     * entre as duas mudanças de nome da [trocar].
     */
    suspend fun ler(): ByteArray?

    /** Escreve o provisório. Falso se não coube ou não deu — e aí nada mudou. */
    suspend fun guardarProvisorio(bytes: ByteArray): Boolean

    /**
     * Põe o provisório no lugar do instalado e guarda o que lá estava. Falso se não deu, e
     * nesse caso o instalado continua a ser o que era.
     */
    suspend fun trocar(): Boolean

    /** Apaga o provisório. Chama-se sempre que a verificação recusa o que desceu. */
    suspend fun descartarProvisorio()

    /**
     * Apaga o anterior. Só depois de a app ter aberto uma vez com o novo e o ter semeado —
     * até lá, é o que torna a troca reversível sem código de reversão.
     */
    suspend fun esquecerAnterior()

    /** O caminho a mostrar a quem pergunta onde é que o catálogo ficou. */
    fun caminho(): String
}
