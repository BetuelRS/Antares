package pt.antares.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Dá ao sódio e à fibra uma casa só.
 *
 * Os dois têm meta diária da EFSA — são micronutrientes como os outros vinte e três — e viviam
 * ao mesmo tempo numa coluna da linha do alimento **e** no mapa de micronutrientes. Nos 1 376
 * alimentos do INSA estavam nos dois sítios, e em 29 deles os números discordavam: a coluna do
 * sódio era um inteiro arredondado e o mapa guardava as casas decimais. A app podia mostrar
 * dois sódios diferentes para o mesmo alimento, conforme o ecrã que o lia.
 *
 * A coluna sai. Antes de sair, o que lá está vai para o mapa — e vai para **todos** os
 * alimentos, e não só para os que a pessoa criou. Os do catálogo vão ser reescritos pelo
 * ficheiro logo a seguir, mas entre esta migração e essa reescrita há uma abertura da app: se
 * a semeadura falhar, o sódio tem de continuar lá.
 *
 * **O mapa ganha à coluna quando os dois têm valor.** É o mapa que tem as casas decimais.
 *
 * Escrita à mão porque o Room sabe apagar uma coluna, mas não sabe pôr o que estava lá dentro
 * noutro sítio primeiro. Corre inteira dentro de uma transação: ou os valores mudam de casa e
 * a tabela é reconstruída, ou a base fica como estava e a abertura seguinte tenta outra vez.
 */
val MIGRACAO_27_PARA_28 = object : Migration(27, 28) {

    override fun migrate(connection: SQLiteConnection) {
        moverParaOMapa(connection, coluna = "fiberG", chave = "fiber_g")
        moverParaOMapa(connection, coluna = "sodiumMg", chave = "sodium_mg")

        // O SQLite não apaga colunas: constrói-se a tabela nova, copia-se, e troca-se o nome.
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `foods_novo` (" +
                "`id` TEXT NOT NULL, `source` TEXT NOT NULL, `sourceRef` TEXT, " +
                "`namePt` TEXT NOT NULL, `nameEn` TEXT NOT NULL, `brand` TEXT, " +
                "`kcal` INTEGER NOT NULL, `proteinG` REAL NOT NULL, `carbsG` REAL NOT NULL, " +
                "`sugarsG` REAL, `fatG` REAL NOT NULL, `satFatG` REAL, `microsJson` TEXT, " +
                "`servingName` TEXT, `servingGrams` REAL, " +
                "`isLiquid` INTEGER NOT NULL DEFAULT 0, `verified` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            """
            INSERT INTO `foods_novo`
            SELECT id, source, sourceRef, namePt, nameEn, brand, kcal, proteinG, carbsG,
                   sugarsG, fatG, satFatG, microsJson, servingName, servingGrams, isLiquid,
                   verified, updatedAt, deleted
            FROM `foods`
            """.trimIndent(),
        )
        connection.execSQL("DROP TABLE `foods`")
        connection.execSQL("ALTER TABLE `foods_novo` RENAME TO `foods`")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_foods_source` ON `foods` (`source`)")
    }

    /**
     * Escreve o valor de uma coluna dentro do JSON dos micronutrientes.
     *
     * Feito com texto e não com as funções de JSON do SQLite: elas são uma extensão que pode
     * não estar compilada no motor de um telemóvel qualquer, e uma migração que rebenta lá é
     * uma app que não abre. O `microsJson` é sempre um objeto escrito pela app — nunca uma
     * lista, nunca nulo por outra razão — e por isso tirar-lhe a última chaveta e acrescentar
     * um par é seguro. O [MigracaoDeSodioEFibraTest] corre isto sobre as três formas que o
     * campo tem: nulo, vazio e com outras chaves lá dentro.
     */
    private fun moverParaOMapa(connection: SQLiteConnection, coluna: String, chave: String) {
        connection.execSQL(
            """
            UPDATE foods SET microsJson = CASE
                WHEN microsJson IS NULL OR trim(microsJson) IN ('', '{}')
                    THEN '{"$chave":' || $coluna || '}'
                ELSE substr(microsJson, 1, length(microsJson) - 1) || ',"$chave":' || $coluna || '}'
            END
            WHERE $coluna IS NOT NULL
              AND (microsJson IS NULL OR instr(microsJson, '"$chave"') = 0)
            """.trimIndent(),
        )
    }
}
