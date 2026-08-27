package pt.antares.app.core.confecao

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pt.antares.app.generated.resources.Res

/**
 * Lê a tabela de confeção que viaja dentro do APK, e lê-a **uma vez**.
 *
 * São uns quilobytes e não muda em execução, ao contrário do catálogo — que é grande e pode
 * descer da rede. Guardá-la em memória depois da primeira leitura poupa abrir o ficheiro cada
 * vez que alguém abre um alimento.
 *
 * **Uma tabela que não abra devolve a vazia, e não uma exceção.** A confeção é uma coisa a
 * mais que a app oferece: se o ficheiro faltar, o que tem de acontecer é a app funcionar como
 * funcionava antes de ela existir, sem um ecrã de alimento a rebentar por causa disso.
 */
class LeitorDeConfecao(
    private val io: CoroutineDispatcher,
    /**
     * Uma tabela já em memória, para os testes.
     *
     * O `Res.readBytes` não abre na máquina virtual dos testes, e sem esta porta um teste da
     * retenção nas receitas só podia afirmar que **nada** se perde — que é precisamente o
     * defeito que a retenção veio corrigir.
     */
    precarregada: TabelaDeConfecao? = null,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private var lida: TabelaDeConfecao? = precarregada

    @OptIn(ExperimentalResourceApi::class)
    suspend fun tabela(): TabelaDeConfecao = lida ?: withContext(io) {
        val t = runCatching {
            json.decodeFromString<TabelaDeConfecao>(
                Res.readBytes(TabelaDeConfecao.FICHEIRO).decodeToString(),
            )
        }.getOrDefault(TabelaDeConfecao.VAZIA)
        lida = t
        t
    }
}
