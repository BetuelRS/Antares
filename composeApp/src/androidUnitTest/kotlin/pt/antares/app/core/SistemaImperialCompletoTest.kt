package pt.antares.app.core

import pt.antares.app.core.model.UnitSystem
import pt.antares.app.core.util.UnitConversions
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O sistema imperial era meio sistema.
 *
 * Estava no perfil, no arranque, no progresso e no peso — e o treino continuava em quilos, a
 * corrida em quilómetros e as porções em gramas. Ninguém repara a escrever o ecrã seguinte:
 * escreve-se `"$peso kg"` e está feito, e a preferência que a pessoa escolheu não protesta.
 *
 * Este teste não sabe desenhar ecrãs. O que ele guarda é a **causa**: uma unidade escrita à
 * mão dentro de um texto de interface, e um formatador de corrida que aceite ser chamado sem
 * dizer em que unidades está.
 */
class SistemaImperialCompletoTest {

    private val ecras: List<File> =
        File("src/commonMain/kotlin/pt/antares/app/feature")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    /**
     * Onde é legítimo escrever a unidade à mão, com a razão.
     *
     * Não são dispensas de estilo: são sítios onde a unidade **não** depende da preferência.
     */
    private val excecoes = mapOf(
        "RunComponents.kt" to
            "os parciais são medidos e gravados por quilómetro pelo motor da corrida; o " +
                "título da tabela diz «por km» e o ritmo acompanha-o",
        "FoodDetailScreen.kt" to
            "«kcal / 100 g» é a densidade da tabela de composição, e não uma porção: em " +
                "onças seria «por 3,5 oz», que não é como nenhuma tabela publica os valores",
        "FoodSearchScreen.kt" to
            "o mesmo «kcal / 100 g» da pesquisa, pela mesma razão",
        "RecipeEditScreen.kt" to
            "o «por 100 g» do resumo da receita, pela mesma razão",
    )

    @Test
    fun `nenhum ecra escreve a unidade a mao dentro do texto`() {
        // Uma unidade colada a um `}` de interpolação, ou logo a seguir a um número dentro de
        // aspas: é assim que um «kg» fica preso a um ecrã.
        val padrao = Regex("""["}]\s?(kg|km|/km)\b""")

        val infratores = ecras
            .filter { it.name !in excecoes }
            .filter { padrao.containsMatchIn(it.readText()) }
            .map { it.name }

        assertEquals(
            emptyList(),
            infratores,
            "unidade métrica escrita à mão — usa os rótulos do `UnitLabels.kt`, que sabem " +
                "o que a pessoa escolheu: $infratores",
        )
    }

    @Test
    fun `o formatador da corrida nao tem unidade por omissao`() {
        val fonte = File(
            "src/commonMain/kotlin/pt/antares/app/feature/running/ui/RunFormat.kt",
        ).readText()

        assertTrue(
            "UnitSystem = UnitSystem" !in fonte,
            "um valor por omissão aqui deixa cada ecrã esquecido a mostrar quilómetros a " +
                "quem escolheu milhas, e sem erro nenhum a avisar",
        )
    }

    @Test
    fun `a volta e meia devolve o mesmo numero`() {
        // Ida e volta em cada par de unidades: é isto que impede um peso de encolher a cada
        // gravação de quem escolheu libras.
        val casos = listOf(100.0, 2.5, 0.1, 1234.5)
        for (v in casos) {
            assertTrue(abs(UnitConversions.lbToKg(UnitConversions.kgToLb(v)) - v) < TOLERANCIA)
            assertTrue(abs(UnitConversions.miToKm(UnitConversions.kmToMi(v)) - v) < TOLERANCIA)
            assertTrue(abs(UnitConversions.ozToG(UnitConversions.gToOz(v)) - v) < TOLERANCIA)
            assertTrue(abs(UnitConversions.flOzToMl(UnitConversions.mlToFlOz(v)) - v) < TOLERANCIA)
        }
    }

    @Test
    fun `o ritmo por milha e maior do que o ritmo por quilometro`() {
        // Uma milha é mais longa: o mesmo esforço leva **mais** segundos. Dividir em vez de
        // multiplicar aqui daria um número plausível e errado.
        val porKm = 300
        val porMilha = UnitConversions.paceToDisplay(porKm, UnitSystem.IMPERIAL)

        assertTrue(porMilha > porKm, "5:00/km deu $porMilha s por milha, e tem de dar mais")
        assertEquals(483, porMilha)
        assertEquals(porKm, UnitConversions.paceToDisplay(porKm, UnitSystem.METRIC))
    }

    @Test
    fun `a onca liquida nao e a onca de massa`() {
        // Têm o mesmo nome e fatores diferentes. Trocá-las dá um erro de 4% que nunca rebenta.
        assertTrue(UnitConversions.ML_PER_FLOZ != UnitConversions.G_PER_OZ)
    }

    private companion object {
        const val TOLERANCIA = 1e-9
    }
}
