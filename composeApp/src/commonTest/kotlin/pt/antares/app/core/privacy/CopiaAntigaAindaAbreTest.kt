package pt.antares.app.core.privacy

import kotlinx.serialization.json.Json
import pt.antares.app.core.database.entities.UserProfileEntity
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Uma cópia de segurança é um ficheiro que a pessoa guarda **fora** da app, e que pode
 * restaurar meses depois, numa versão que já não tem os mesmos campos. A coluna `energyUnit`
 * saiu na v26, e todas as cópias feitas até lá trazem-na escrita.
 *
 * O que este teste guarda é a tolerância que faz isso funcionar: o importador ignora os
 * campos que já não conhece. Sem ela, apagar uma coluna transformava as cópias antigas em
 * ficheiros ilegíveis — e o dono só descobria no dia em que precisasse delas.
 */
class CopiaAntigaAindaAbreTest {

    // A mesma configuração do `BackupImporter`, repetida aqui porque ele vive no
    // `androidUnitTest` e isto corre em comum. O `ImportadorTolerante` abaixo garante que as
    // duas não descolam — sem isso, alguém tirava o `ignoreUnknownKeys` de lá e este teste
    // continuava verde a afirmar uma coisa que já não era verdade.
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `um perfil gravado antes da v26 ainda se le`() {
        val copiaDe2025 = """
            {
              "id": "profile",
              "sex": "MALE",
              "birthEpochDay": 9000,
              "heightCm": 178,
              "activityLevel": "MODERATE",
              "goalType": "LOSE",
              "goalRateKcal": -500,
              "macroStrategy": "BALANCED",
              "customProteinG": null,
              "customCarbsG": null,
              "customFatG": null,
              "unitSystem": "METRIC",
              "energyUnit": "KJ",
              "updatedAt": 1700000000000
            }
        """.trimIndent()

        val perfil = json.decodeFromString<UserProfileEntity>(copiaDe2025)

        assertEquals("profile", perfil.id)
        assertEquals(178, perfil.heightCm)
    }
}
