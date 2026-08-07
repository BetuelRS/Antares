package pt.antares.app.core.demo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pt.antares.app.core.database.daos.DemoDao
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.toAppError

sealed interface DemoResult {

    data class Ligado(val linhas: Int) : DemoResult

    data class Desligado(val linhas: Int) : DemoResult

    data class RecusadoPorDadosReais(val linhas: Int) : DemoResult

    data class Falhou(val error: AppError) : DemoResult
}

class DemoDataWriter(
    private val dao: DemoDao,
    private val io: CoroutineDispatcher,
) {

    private companion object {
        const val ALIMENTOS = 60
        const val EXERCICIOS = 40
    }

    suspend fun estaLigado(): Boolean = withContext(io) { dao.demoCount() > 0 }

    suspend fun quantasLinhas(): Int = withContext(io) { dao.demoCount() }

    suspend fun ligar(
        hoje: Long,
        semente: Long = DemoDataEngine.SEMENTE_PADRAO,
    ): DemoResult = withContext(io) {
        try {
            val reais = dao.realCount()
            if (reais > 0) return@withContext DemoResult.RecusadoPorDadosReais(reais)

            dao.deleteAllDemo()

            val dados = DemoDataEngine.gerar(
                semente = semente,
                diaFinal = hoje,
                catalogo = dao.catalogoParaDemo(ALIMENTOS).map { f ->
                    DemoFood(
                        id = f.id,
                        nome = f.namePt,
                        kcalPer100 = f.kcal,
                        proteinaPer100 = f.proteinG,
                        hidratosPer100 = f.carbsG,
                        gorduraPer100 = f.fatG,
                        liquido = f.isLiquid,
                    )
                },
                exercicios = dao.exerciciosParaDemo(EXERCICIOS),
                protocoloJejumId = dao.protocoloParaDemo(),
            )

            dao.insertWeights(dados.pesos)
            dao.insertMeasurements(dados.medidas)
            dao.insertFoodLogs(dados.refeicoes)
            dao.insertWater(dados.aguas)
            dao.insertSessions(dados.treinos)
            dao.insertSets(dados.series)
            dao.insertRuns(dados.corridas)
            dao.insertFasts(dados.jejuns)

            DemoResult.Ligado(dados.total)
        } catch (e: Throwable) {

            runCatching { dao.deleteAllDemo() }
            DemoResult.Falhou(e.toAppError())
        }
    }

    suspend fun desligar(): DemoResult = withContext(io) {
        try {
            val antes = dao.demoCount()
            dao.deleteAllDemo()
            DemoResult.Desligado(antes)
        } catch (e: Throwable) {
            DemoResult.Falhou(e.toAppError())
        }
    }
}
