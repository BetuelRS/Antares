package pt.antares.app.feature.fooddata

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.antares.app.generated.resources.Res
import pt.antares.app.generated.resources.confecao_metodo_assado
import pt.antares.app.generated.resources.confecao_metodo_cozido
import pt.antares.app.generated.resources.confecao_metodo_estufado
import pt.antares.app.generated.resources.confecao_metodo_frito
import pt.antares.app.generated.resources.confecao_metodo_grelhado
import pt.antares.app.generated.resources.confecao_metodo_reaquecido
import pt.antares.app.generated.resources.confecao_metodo_salteado

/**
 * Os métodos de confeção traduzidos.
 *
 * O oleoduto escreve os nomes em português dentro da tabela, mas quem lê o ecrã pode tê-lo
 * em inglês — e **um nome de método não é conteúdo do catálogo, é palavra da interface**.
 *
 * Vive aqui, e não dentro de um cartão, porque a mesma pergunta se faz em dois sítios: num
 * alimento que se cozinha e numa receita que foi ao lume. Duas cópias desta tabela eram duas
 * traduções a divergir.
 */
private val NOMES: Map<String, StringResource> = mapOf(
    "estufado" to Res.string.confecao_metodo_estufado,
    "grelhado" to Res.string.confecao_metodo_grelhado,
    "frito" to Res.string.confecao_metodo_frito,
    "salteado" to Res.string.confecao_metodo_salteado,
    "assado" to Res.string.confecao_metodo_assado,
    "cozido" to Res.string.confecao_metodo_cozido,
    "reaquecido" to Res.string.confecao_metodo_reaquecido,
)

/**
 * O nome do método na língua do ecrã, com o da tabela como recurso.
 *
 * Um método novo no oleoduto aparece com o nome português que a tabela traz, em vez de
 * desaparecer do ecrã — que é o que acontecia com um mapa que devolvesse nulo.
 */
@Composable
fun nomeDoMetodo(id: String, nomeDaTabela: String): String =
    NOMES[id]?.let { stringResource(it) } ?: nomeDaTabela
