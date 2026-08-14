package pt.antares.app.core.privacy

import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import pt.antares.app.core.database.AntaresDb

/**
 * O que a importação precisa da base: apagar e voltar a escrever **numa transação só**.
 *
 * É uma interface e não a própria [AntaresDb] para o [BackupImporter] se poder testar sem
 * Room. A implementação verdadeira é a [RoomBackupDb].
 */
fun interface BackupDb {

    /**
     * Esvazia as tabelas de [aTruncar] e corre [bloco], tudo na mesma transação. Uma
     * exceção lançada lá dentro desfaz também o que já tinha sido escrito: ou a cópia
     * entra inteira, ou o telemóvel fica exatamente como estava.
     */
    suspend fun emTransacao(aTruncar: List<String>, bloco: suspend () -> Unit)
}

/**
 * A transação real. Os DAOs chamados dentro de [bloco] reaproveitam esta ligação — o Room
 * guarda-a no contexto da corrotina — e por isso as escritas deles caem com ela.
 */
class RoomBackupDb(private val db: AntaresDb) : BackupDb {

    override suspend fun emTransacao(aTruncar: List<String>, bloco: suspend () -> Unit) {
        db.useWriterConnection { ligacao ->
            // Imediata e não diferida: a escrita começa já, e duas importações ao mesmo
            // tempo falham à partida em vez de falharem a meio.
            ligacao.immediateTransaction {
                // Os nomes são os das fontes de exportação, escritos no código; o
                // `GdprTableParityTest` prova que todos existem mesmo na base. Nunca vem
                // daqui texto que tenha entrado do ficheiro.
                aTruncar.forEach { execSQL("DELETE FROM `$it`") }
                bloco()
            }
        }
    }
}
