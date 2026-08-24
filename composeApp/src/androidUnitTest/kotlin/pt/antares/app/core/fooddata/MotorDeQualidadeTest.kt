package pt.antares.app.core.fooddata

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pt.antares.app.feature.fooddata.Catalogo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A fila de revisão que o oleoduto escreve, vista de fora.
 *
 * **O modo de falhar de um verificador é emudecer.** Se uma verificação deixar de encontrar
 * seja o que for — porque um campo mudou de nome, porque um valor passou a ser texto, porque
 * alguém apertou um limiar sem medir — nada rebenta e nada avisa: o ficheiro fica mais
 * pequeno e lê-se como o catálogo ter melhorado. Já aconteceu uma vez, com catorze óleos a
 * somarem `NaN` gramas em cem, e `NaN > 100` é falso.
 *
 * Os mínimos são folgados de propósito: o que se quer apanhar é o motor calar-se, não uma
 * variação da fonte. Os números do dia em que foram escritos estão no companheiro.
 *
 * A outra metade do trabalho é dos testes em `tools/catalogo/qualidade.test.mjs`, que provam
 * o contrário — que ele **não** acusa o que não devia. Este prova que ele continua a falar.
 */
class MotorDeQualidadeTest {

    @Serializable
    private data class Achado(
        val id: String,
        val tipo: String,
        val gravidade: String,
        val mensagem: String,
        val campo: String? = null,
    )

    /** Quantas incoerências a coerência arrumou antes de o motor olhar. */
    @Serializable
    private data class Corrigidas(val acucares: Int, val gorduras: Int, val agua: Int)

    @Serializable
    private data class Qualidade(
        val contradicoes: List<Achado>,
        val suspeitas: List<Achado>,
        val corrigidas: Corrigidas,
    )

    private val json = Json { ignoreUnknownKeys = true }

    private val qualidade: Qualidade =
        json.decodeFromString(File("../tools/catalogo/qualidade.json").readText())

    private val idsDoCatalogo: Set<String> = run {
        val texto = File("src/commonMain/composeResources/files/catalogo.json").readText()
        json.decodeFromString<Catalogo>(texto).alimentos.map { it.id }.toSet()
    }

    @Test
    fun `nenhuma verificacao emudeceu`() {

        // Uma verificação a zero não se distingue de uma verificação apagada. Cada uma destas
        // tinha achados no dia em que entrou, e nenhuma delas tem razão nenhuma para os perder
        // sem alguém ter corrigido o catálogo de propósito.
        val porTipo = (qualidade.contradicoes + qualidade.suspeitas).groupingBy { it.tipo }
            .eachCount()

        for (tipo in TIPOS) {
            assertTrue(
                (porTipo[tipo] ?: 0) > 0,
                "a verificação «$tipo» deixou de encontrar seja o que for. " +
                    "Ou o catálogo foi corrigido de propósito, ou ela partiu-se em silêncio. " +
                    "Achados por tipo: $porTipo",
            )
        }
    }

    @Test
    fun `as tres verificacoes de contradicao nao emudeceram - foram atendidas`() {

        /*
         * As contradições estão a zero, e isso é bom — **mas só se alguém as tiver
         * corrigido.** A coerência arruma-as no oleoduto antes de o motor olhar, e a partir
         * daí «zero contradições» deixa de distinguir um catálogo são de uma verificação
         * apagada. É este número que volta a fazer essa distinção.
         *
         * Se um dia a fonte deixar de ter estes casos, este teste falha e a resposta é
         * escrever isso — não é baixar o mínimo até ele passar.
         */
        val c = qualidade.corrigidas

        assertTrue(c.acucares > 0, "a regra do açúcar dentro dos hidratos deixou de tocar em nada")
        assertTrue(c.gorduras > 0, "a regra das gorduras dentro da gordura deixou de tocar em nada")
        assertTrue(c.agua > 0, "a regra da água dentro dos 100 g deixou de tocar em nada")

        // E não pode passar a mexer no catálogo inteiro: com as folgas do motor são doze
        // alimentos; com folga zero eram duzentos e nove, todos por um décimo de grama.
        val total = c.acucares + c.gorduras + c.agua
        assertTrue(total < MAXIMO_CORRIGIDO, "$total correções — a coerência passou a escalar por arredondamento")
    }

    @Test
    fun `a fila nao encolheu de repente`() {
        val total = qualidade.contradicoes.size + qualidade.suspeitas.size

        assertTrue(
            total >= MINIMO_DE_ACHADOS,
            "só $total achados na fila — eram 128 no dia em que isto foi escrito",
        )
    }

    @Test
    fun `as contradicoes sao poucas e estao todas declaradas`() {

        // São números impossíveis, e a construção chumba com uma nova. Este número sobe quando
        // alguém decidir aceitar mais uma, e nunca sozinho.
        assertTrue(
            qualidade.contradicoes.size <= MAXIMO_DE_CONTRADICOES,
            "${qualidade.contradicoes.size} contradições aceites — eram 12. " +
                "Aceitar mais é uma decisão, não um acidente.",
        )
        assertEquals(
            emptyList(),
            qualidade.contradicoes.filter { it.gravidade != "contradicao" },
            "há suspeitas guardadas como contradições, e essas chumbam a construção",
        )
    }

    @Test
    fun `todo o achado aponta para um alimento que existe`() {

        // A oficina abre o alimento pelo identificador. Um achado sobre um alimento podado é
        // uma linha na fila que ninguém consegue fechar.
        val orfaos = (qualidade.contradicoes + qualidade.suspeitas)
            .map { it.id }
            .filterNot { it in idsDoCatalogo }
            .distinct()

        assertEquals(emptyList(), orfaos, "achados sobre alimentos que não estão no catálogo")
    }

    @Test
    fun `todo o achado diz o que esta mal, e nao so que esta mal`() {
        val mudos = (qualidade.contradicoes + qualidade.suspeitas)
            .filter { it.mensagem.isBlank() || it.campo.isNullOrBlank() }
            .map { "${it.id}:${it.tipo}" }

        assertEquals(emptyList(), mudos, "achados sem mensagem ou sem campo")
    }

    private companion object {
        // As tres de contradicao — massa, gorduras, acucares — sairam desta lista quando a
        // coerencia passou a corrigi-las no oleoduto. O que as guarda agora e o teste das
        // correcoes, mais abaixo.
        val TIPOS = listOf("atwater", "escala", "discordancia")

        /*
         * Contados a 2026-08-24: **zero contradições e 128 suspeitas**, em 7 964 alimentos —
         * 66 Atwater, 60 fora de escala, 2 discordâncias.
         *
         * Eram 264 na véspera, e a fila encolheu por três razões escritas, nenhuma delas «o
         * motor calou-se»:
         *
         * - as 12 contradições passaram a ser corrigidas no oleoduto, e o teste das correções
         *   aqui ao lado é que as guarda;
         * - a discordância entre fontes deixou de comparar arroz cru com arroz cozido, e caiu
         *   de 16 para 2;
         * - a comparação de escala passou a ser dentro do sub-subgrupo da CIQUAL, e um pato
         *   deixou de ser acusado de ser gordo por comparação com as aves todas: 130 → 60;
         * - a leitura completa da USDA trouxe o álcool e os polióis, e as bebidas e os doces
         *   sem açúcar deixaram de falhar a conta de Atwater: 102 → 66.
         *
         * Se este número voltar a cair, a resposta é a mesma: escrever porquê. **Baixar o
         * mínimo até passar é o que este teste existe para tornar difícil.**
         */
        const val MINIMO_DE_ACHADOS = 90
        const val MAXIMO_DE_CONTRADICOES = 12

        // Doze com as folgas do motor. Duzentas e nove com folga zero, e nenhuma delas
        // valia o diff que produzia.
        const val MAXIMO_CORRIGIDO = 40
    }
}
