package pt.antares.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Tira o que é da pessoa de dentro da linha do alimento.
 *
 * O favorito, a última utilização e a porção guardada viviam em `foods`, e isso custava duas
 * coisas ao mesmo tempo. **O catálogo é substituído por inteiro a cada versão** e a escrita
 * grava a linha toda por cima — a 2.4.0 teve de as transportar à mão para não as apagar. E
 * **não iam na cópia de segurança**, porque a exportação de alimentos só leva os que a pessoa
 * criou: quem restaurava uma cópia perdia os favoritos sem aviso nenhum.
 *
 * Escrita à mão e não automática porque o Room sabe criar e apagar, mas não sabe **mudar
 * dados de sítio**: uma migração automática criava a tabela nova vazia e deitava as colunas
 * fora com o que estava lá dentro.
 *
 * **Não há passo de verificação, e é de propósito.** O Room corre a migração inteira dentro
 * de uma transação: ou a cópia e a reconstrução acontecem as duas, ou não acontece nenhuma e
 * a base fica exactamente como estava, para a abertura seguinte tentar outra vez. Um `if` a
 * meio disto não teria como falhar melhor do que isso — só como falhar mais tarde. O que
 * conta as linhas dos dois lados é o [MigracaoDeMarcasTest], com dados a sério.
 *
 * Fica o número de marcas copiadas em `db_info`: não é usado por nada, e é o que permite a
 * alguém, daqui a um ano, saber se a base que tem na mão passou por aqui e com o quê.
 */
val MIGRACAO_26_PARA_27 = object : Migration(26, 27) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_marca` (" +
                "`foodId` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, " +
                "`lastUsedAt` INTEGER NOT NULL, `lastAmountG` REAL, " +
                "`updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, " +
                "PRIMARY KEY(`foodId`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_food_marca_isFavorite` ON `food_marca` (`isFavorite`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_food_marca_lastUsedAt` ON `food_marca` (`lastUsedAt`)",
        )

        // Só as linhas em que a pessoa deixou alguma coisa. Copiar as oito mil seria uma
        // tabela de marcas onde quase nada está marcado, e cada leitura pagava por isso.
        connection.execSQL(
            """
            INSERT INTO `food_marca` (foodId, isFavorite, lastUsedAt, lastAmountG, updatedAt, deleted)
            SELECT id, isFavorite, lastUsedAt, lastAmountG, updatedAt, 0 FROM `foods`
            WHERE isFavorite = 1 OR lastUsedAt != 0 OR lastAmountG IS NOT NULL
            """.trimIndent(),
        )

        // O SQLite não apaga colunas: constrói-se a tabela nova, copia-se, e troca-se o
        // nome. Os índices de `lastUsedAt` e `isFavorite` desaparecem com elas — passam a
        // viver na `food_marca`, que é onde as colunas agora estão.
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `foods_novo` (" +
                "`id` TEXT NOT NULL, `source` TEXT NOT NULL, `sourceRef` TEXT, " +
                "`namePt` TEXT NOT NULL, `nameEn` TEXT NOT NULL, `brand` TEXT, " +
                "`kcal` INTEGER NOT NULL, `proteinG` REAL NOT NULL, `carbsG` REAL NOT NULL, " +
                "`sugarsG` REAL, `fatG` REAL NOT NULL, `satFatG` REAL, `fiberG` REAL, " +
                "`sodiumMg` INTEGER, `microsJson` TEXT, `servingName` TEXT, " +
                "`servingGrams` REAL, `isLiquid` INTEGER NOT NULL DEFAULT 0, " +
                "`verified` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "`deleted` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            """
            INSERT INTO `foods_novo`
            SELECT id, source, sourceRef, namePt, nameEn, brand, kcal, proteinG, carbsG,
                   sugarsG, fatG, satFatG, fiberG, sodiumMg, microsJson, servingName,
                   servingGrams, isLiquid, verified, updatedAt, deleted
            FROM `foods`
            """.trimIndent(),
        )
        connection.execSQL("DROP TABLE `foods`")
        connection.execSQL("ALTER TABLE `foods_novo` RENAME TO `foods`")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_foods_source` ON `foods` (`source`)")

        connection.execSQL(
            """
            INSERT OR REPLACE INTO `db_info` (`key`, `value`)
            VALUES ('marcas_migradas_v27', (SELECT COUNT(*) FROM `food_marca`))
            """.trimIndent(),
        )
    }
}
