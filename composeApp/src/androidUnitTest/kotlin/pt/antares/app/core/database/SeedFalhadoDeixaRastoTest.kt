package pt.antares.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pt.antares.app.core.crash.CrashStore
import pt.antares.app.core.crash.ENGOLIDA
import pt.antares.app.feature.workout.data.ExerciseSeeder
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Falhar a ler o ficheiro do catálogo deixa a app **sem exercícios nenhuns** e a funcionar:
 * a marca não é posta, e a próxima abertura tenta outra vez. Essa recuperação está certa —
 * o que não pode é acontecer em silêncio, porque no ecrã um catálogo por semear não se
 * distingue de um catálogo que falhou.
 */
@RunWith(RobolectricTestRunner::class)
class SeedFalhadoDeixaRastoTest {

    private class StoreEmMemoria : CrashStore {
        var texto: String? = null
        override fun write(report: String) { texto = report }
        override fun read(): String? = texto
        override fun clear() { texto = null }
    }

    private val db: AntaresDb = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AntaresDb::class.java,
    ).setQueryCoroutineContext(Dispatchers.Default).build()

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun `nao conseguir ler o seed dos exercicios fica registado`() = runTest {
        val crashes = StoreEmMemoria()

        // A falha é pedida, e não esperada: contar com que os recursos do Compose não
        // existam num teste unitário fazia isto passar ou falhar conforme a ordem em que os
        // testes corriam — o fornecedor de recursos é do processo inteiro.
        ExerciseSeeder(db, Dispatchers.Default, crashes, ler = { error("ficheiro ausente") })
            .seedIfNeeded()

        val relatorio = assertNotNull(
            crashes.texto,
            "o seed falhou a ler e não deixou rasto nenhum: no ecrã isto é um catálogo vazio " +
                "sem explicação, e a app é offline — não há mais nada que o preencha",
        )
        assertTrue(
            relatorio.contains("seed_exercises.json"),
            "o relatório não diz que ficheiro falhou:\n$relatorio",
        )
        assertTrue(
            relatorio.contains(ENGOLIDA),
            "o relatório tem de dizer que a app continuou, senão lê-se como se tivesse " +
                "rebentado:\n$relatorio",
        )
    }

    @Test
    fun `falhar a ler nao poe a marca, para a proxima abertura tentar outra vez`() = runTest {
        ExerciseSeeder(
            db,
            Dispatchers.Default,
            StoreEmMemoria(),
            ler = { error("ficheiro ausente") },
        ).seedIfNeeded()

        assertTrue(
            db.dbInfoDao().get("seed_exercises_imported") == null,
            "marcou como semeado sem ter semeado — o catálogo ficaria vazio para sempre",
        )
    }
}
