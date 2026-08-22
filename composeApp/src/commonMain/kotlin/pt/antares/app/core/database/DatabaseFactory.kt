package pt.antares.app.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

// Só o caminho do ficheiro depende da plataforma; a configuração fica toda no comum, para
// não haver duas bases configuradas de maneiras diferentes.
expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<AntaresDb>
}

fun RoomDatabase.Builder<AntaresDb>.buildAntaresDb(): AntaresDb = this
    // A única migração escrita à mão. O Room sabe criar e apagar, mas não sabe mudar dados
    // de sítio — e foi isso que a v27 fez com o que a pessoa tinha marcado.
    .addMigrations(MIGRACAO_26_PARA_27, MIGRACAO_27_PARA_28)
    // SQLite empacotado com a app em vez do que vem no sistema: o mesmo comportamento em
    // todas as versões de Android, incluindo o FTS4 de que a pesquisa depende.
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
