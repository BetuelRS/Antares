package pt.antares.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.database.entities.FoodLogEntity
import pt.antares.app.core.model.LogOrigin
import pt.antares.app.core.model.MealSlot
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class Migration2to3Test {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-2to3-test.db"

    @Before
    fun cleanStart() {
        context.deleteDatabase(dbName)
    }

    @After
    fun cleanEnd() {
        context.deleteDatabase(dbName)
    }

    private fun createV2Database() {
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val v2 = SQLiteDatabase.openOrCreateDatabase(file, null)
        v2.beginTransaction()
        try {
            v2.execSQL(
                "CREATE TABLE IF NOT EXISTS `db_info` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
            )
            v2.execSQL(
                "CREATE TABLE IF NOT EXISTS `user_profile` (`id` TEXT NOT NULL, `sex` TEXT NOT NULL, " +
                    "`birthEpochDay` INTEGER NOT NULL, `heightCm` INTEGER NOT NULL, `activityLevel` TEXT NOT NULL, " +
                    "`goalType` TEXT NOT NULL, `goalRateKcal` INTEGER NOT NULL, `macroStrategy` TEXT NOT NULL, " +
                    "`customProteinG` INTEGER, `customCarbsG` INTEGER, `customFatG` INTEGER, " +
                    "`exerciseAddBack` INTEGER NOT NULL, `unitSystem` TEXT NOT NULL, `energyUnit` TEXT NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `dirty` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            v2.execSQL(
                "CREATE TABLE IF NOT EXISTS `weight_log` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                    "`weightKg` REAL NOT NULL, `note` TEXT, `updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, " +
                    "`dirty` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            v2.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_weight_log_epochDay` ON `weight_log` (`epochDay`)",
            )
            v2.execSQL(
                "CREATE TABLE IF NOT EXISTS `daily_target_override` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                    "`kcal` INTEGER NOT NULL, `proteinG` INTEGER NOT NULL, `carbsG` INTEGER NOT NULL, `fatG` INTEGER NOT NULL, " +
                    "`source` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `dirty` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            v2.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_target_override_epochDay` ON `daily_target_override` (`epochDay`)",
            )

            v2.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            v2.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '6a803e686776f085390383c36cf81e29')",
            )

            v2.execSQL(
                "INSERT INTO user_profile VALUES ('profile','MALE',10000,178,'MODERATE','MAINTAIN',0,'BALANCED'," +
                    "NULL,NULL,NULL,1,'METRIC','KCAL',1,0,1)",
            )
            v2.execSQL("INSERT INTO weight_log VALUES ('w1',100,81.0,NULL,1,0,1)")
            v2.execSQL("INSERT INTO weight_log VALUES ('w2',105,80.2,NULL,2,0,1)")

            v2.execSQL("PRAGMA user_version = 2")
            v2.setTransactionSuccessful()
        } finally {
            v2.endTransaction()
            v2.close()
        }
    }

    @Test
    fun `migracao 2 para 3 preserva perfil e pesos e cria tabelas novas`() = runTest {
        createV2Database()

        val db = Room.databaseBuilder(context, AntaresDb::class.java, dbName)
            .addMigrations(MIGRACAO_26_PARA_27, MIGRACAO_27_PARA_28)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()

        try {

            val profile = db.userProfileDao().get()
            assertEquals(178, profile?.heightCm)
            assertEquals("profile", profile?.id)

            assertEquals(2, db.weightLogDao().observeAll().first().size)
            assertEquals(80.2, db.weightLogDao().latest()?.weightKg)

            assertEquals(0, db.foodDao().count())
            db.foodLogDao().upsert(
                FoodLogEntity(
                    id = "l1", epochDay = 200, mealSlot = MealSlot.LUNCH, foodId = null,
                    nameSnapshot = "Teste", quantityGrams = 100.0, kcalSnapshot = 300,
                    proteinSnapshot = 10.0, carbsSnapshot = 20.0, fatSnapshot = 5.0,
                    microsPer100Json = null, origin = LogOrigin.MANUAL, updatedAt = 1L,
                ),
            )
            assertEquals(300, db.foodLogDao().observeDayTotals(200).first().kcal)
        } finally {
            db.close()
        }
    }

    /**
     * A `dirty` foi apagada de 23 tabelas na v24, e apagar uma coluna no SQLite obriga o
     * Room a **recriar a tabela e copiar as linhas**. Uma migração assim ou corre bem ou
     * leva o histórico de alguém com ela, e por isso prova-se aqui: a mesma base da v2, que
     * tem a coluna, aberta com o esquema de agora.
     */
    @Test
    fun `apagar a coluna dirty recria as tabelas sem perder linhas`() = runTest {
        createV2Database()

        val db = Room.databaseBuilder(context, AntaresDb::class.java, dbName)
            .addMigrations(MIGRACAO_26_PARA_27, MIGRACAO_27_PARA_28)
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()

        try {
            assertEquals(
                2,
                db.weightLogDao().observeAll().first().size,
                "recriar a tabela perdeu pesagens pelo caminho",
            )
            assertEquals(178, db.userProfileDao().get()?.heightCm, "o perfil não sobreviveu")

            val colunas = mutableListOf<String>()
            db.openHelper.readableDatabase.query("PRAGMA table_info(`weight_log`)").use { c ->
                val nome = c.getColumnIndexOrThrow("name")
                while (c.moveToNext()) colunas += c.getString(nome)
            }
            assertEquals(
                emptyList(),
                colunas.filter { it == "dirty" },
                "a coluna continua na base: a migração correu sem fazer nada",
            )
        } finally {
            db.close()
        }
    }
}
