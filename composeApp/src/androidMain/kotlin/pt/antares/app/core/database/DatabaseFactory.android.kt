package pt.antares.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun create(): RoomDatabase.Builder<AntaresDb> {
        val dbFile = context.getDatabasePath("antares.db")
        return Room.databaseBuilder(
            context = context.applicationContext,
            klass = AntaresDb::class.java,
            name = dbFile.absolutePath,
        )
    }
}
