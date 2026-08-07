package pt.antares.app.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<AntaresDb>
}

fun RoomDatabase.Builder<AntaresDb>.buildAntaresDb(): AntaresDb = this
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
