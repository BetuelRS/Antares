package pt.antares.app.core.demo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pt.antares.app.core.database.daos.DemoDao
import pt.antares.app.core.util.AppError
import pt.antares.app.core.util.toAppError

/**
 * O que aconteceu ao ligar ou desligar a demonstração. `RecusadoPorDadosReais` é o estado
 * que interessa: a app nunca mistura ficção com registos de alguém, e prefere não fazer
 * nada a escrever por cima.
 */
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

    /**
     * Enche a app com dois anos de dados inventados. A semente é fixa por omissão para a
     * mesma chamada dar sempre a mesma demonstração — sem isso, comparar dois ecrãs
     * separados por uma regeração não provaria nada.
     */
    suspend fun ligar(
        hoje: Long,
        semente: Long = DemoDataEngine.SEMENTE_PADRAO,
    ): DemoResult = withContext(io) {
        try {
            // Primeira coisa e sem exceção: um único registo real trava tudo.
            val reais = dao.realCount()
            if (reais > 0) return@withContext DemoResult.RecusadoPorDadosReais(reais)

            // Limpa uma demonstração anterior, que também colide nos índices únicos.
            dao.deleteAllDemo()

            // As lápides ocupam o dia nos índices únicos e fazem abortar a geração inteira
            // — basta uma, e qualquer pesagem alguma vez apagada deixa uma. Só é seguro
            // porque o `realCount()` acima já garantiu que não há dados vivos.
            // Ver DemoTombstoneTest.
            dao.purgeTombstonesBlockingDemo()

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
                        microsJson = f.microsJson,
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

            // As inserções não estão numa transação só, por isso uma falha a meio deixaria
            // meia demonstração na base. Limpa-se o que entrou; se nem isso resultar, o
            // erro original é o que interessa e não o da limpeza.
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
