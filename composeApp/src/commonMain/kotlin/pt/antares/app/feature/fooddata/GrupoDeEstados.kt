package pt.antares.app.feature.fooddata

import pt.antares.app.core.database.entities.FoodEntity

/**
 * Um alimento e os seus estados, como a lista de pesquisa os mostra.
 *
 * O [principal] é a linha que se vê; os [outros] estão por baixo, e só aparecem se alguém
 * tocar. É o grupo inteiro que conta como **um** resultado.
 */
data class GrupoDeEstados(
    val principal: FoodEntity,
    val outros: List<FoodEntity> = emptyList(),
) {
    val todos: List<FoodEntity> get() = listOf(principal) + outros

    /** O que a linha diz quando há mais: «cru» na principal, «+ 2 estados» ao lado. */
    val quantosOutros: Int get() = outros.size
}

/**
 * Os estados que um nome de alimento pode trazer no fim, por ordem de preferência.
 *
 * **A ordem é a decisão.** O cru vem primeiro por ser a forma de que as outras derivam, e é
 * também a que a confeção sabe transformar — abrir no cru dá acesso a todos os métodos, e
 * abrir no assado dá acesso a nenhum. A seguir vêm os cozinhados por ordem de quão comum é
 * cada método na cozinha portuguesa.
 *
 * O inglês está aqui porque 2 909 alimentos ainda o têm no nome. À medida que a tradução
 * avança, estas entradas vão deixando de encontrar nada — e não fazem mal nenhum entretanto.
 */
private val ESTADOS: List<String> = listOf(
    "cru", "crua", "crus", "cruas", "raw",
    "cozido", "cozida", "cozidos", "cozidas", "boiled",
    "cozinhado", "cozinhada", "cozinhados", "cozinhadas", "cooked",
    "grelhado", "grelhada", "grelhados", "grelhadas", "grilled",
    "assado no forno", "assada no forno", "assados no forno", "assadas no forno",
    "assado", "assada", "assados", "assadas", "roasted", "baked",
    "cozido a vapor", "cozido ao vapor", "steamed",
    "estufado", "estufada", "estufados", "estufadas",
    "salteado", "salteada", "salteados", "salteadas",
    "frito", "frita", "fritos", "fritas", "fried",
    "reaquecido", "reaquecida",
)

/**
 * As expressões que reconhecem cada estado no fim de um nome.
 *
 * Ordenadas da mais longa para a mais curta, porque «assado no forno» tem de ganhar a
 * «assado» — sem isso a base ficava «Frango, carne, no forno», que não é base nenhuma.
 */
private val RECONHECEDORES: List<Pair<String, Regex>> =
    ESTADOS.sortedByDescending { it.length }.map { estado ->
        estado to Regex(",\\s*" + estado.replace(" ", "\\s+") + "$", RegexOption.IGNORE_CASE)
    }

/** A posição do estado na ordem de preferência, ou o fim da lista para quem não tem estado. */
private fun ordemDe(estado: String?): Int =
    estado?.let { ESTADOS.indexOf(it) }.takeIf { it != null && it >= 0 } ?: ESTADOS.size

/**
 * Parte um nome em base e estado. Devolve o nome inteiro e nulo quando não há estado.
 *
 * A base é o que fica **sem a última vírgula**, e não uma normalização: dois alimentos só se
 * agrupam se o texto antes do estado for exactamente o mesmo. É deliberado — «Frango, carne»
 * e «Frango, carne e pele» são dois alimentos com gorduras diferentes, e juntá-los escondia
 * um deles atrás do outro.
 */
internal fun partirEstado(nome: String): Pair<String, String?> {
    for ((estado, re) in RECONHECEDORES) {
        val m = re.find(nome) ?: continue
        return nome.removeRange(m.range) to estado
    }
    return nome to null
}

/**
 * Agrupa os resultados que só diferem no estado.
 *
 * Uma pesquisa por «frango» devolvia sete linhas quase iguais — cru, assado, com pele, sem
 * pele — e a pessoa lia-as todas para escolher uma. Agora é uma linha por alimento, com os
 * estados por baixo.
 *
 * **A ordem de chegada manda.** O primeiro do grupo a aparecer nos resultados é o principal,
 * seja qual for o estado: a ordenação da pesquisa já pôs à frente o que a pessoa marcou, o
 * que usou há pouco e o que é português, e reordenar aqui por estado deitava fora esse
 * trabalho. A ordem dos estados só decide **entre os outros**, que é onde não há mais nada
 * a distinguir.
 *
 * Alimentos sem estado no nome ficam sozinhos, cada um no seu grupo, e é o caso da esmagadora
 * maioria do catálogo.
 */
fun agruparEstados(resultados: List<FoodEntity>): List<GrupoDeEstados> {
    val porBase = LinkedHashMap<String, MutableList<Pair<FoodEntity, String?>>>()
    val soltos = ArrayList<GrupoDeEstados>()

    // Uma chave que junta a base ao que distingue dois alimentos com o mesmo nome: a marca
    // e a origem. Sem isso, o «Frango, cru» da CIQUAL e o da TCA — que discordam em 20 kcal —
    // apareciam como estados um do outro, e um deles desaparecia do ecrã.
    fun chave(base: String, food: FoodEntity) = "$base|${food.brand.orEmpty()}|${origemDe(food.id)}"

    val ordem = ArrayList<String>()

    for (f in resultados) {
        val (base, estado) = partirEstado(f.namePt)
        if (estado == null) {
            soltos += GrupoDeEstados(f)
            ordem += "solto:${f.id}"
            continue
        }
        val k = chave(base, f)
        if (k !in porBase) ordem += k
        porBase.getOrPut(k) { ArrayList() } += f to estado
    }

    // Reconstrói pela ordem de chegada, misturando soltos e grupos como vinham.
    val soltosPorId = soltos.associateBy { "solto:${it.principal.id}" }
    return ordem.mapNotNull { k ->
        soltosPorId[k] ?: porBase[k]?.let { membros ->
            val principal = membros.first().first
            val outros = membros.drop(1)
                .sortedBy { (_, estado) -> ordemDe(estado) }
                .map { it.first }
            GrupoDeEstados(principal, outros)
        }
    }
}

/**
 * A origem, lida do identificador — é ele que a fixa, e é o mesmo critério do oleoduto.
 *
 * Dois alimentos da mesma fonte com a mesma base são estados um do outro; de fontes
 * diferentes são duas medições, e essas arbitram-se no oleoduto e não no ecrã.
 */
private fun origemDe(id: String): String =
    id.substringBefore('-', missingDelimiterValue = id).take(LETRAS_DA_ORIGEM)

// «ciqual», «usda», «tca», «ptx» — seis letras chegam para os distinguir, e cortar aqui
// junta o «ptx2» e o «ptx3» na mesma origem, que e o que eles sao.
private const val LETRAS_DA_ORIGEM = 6
