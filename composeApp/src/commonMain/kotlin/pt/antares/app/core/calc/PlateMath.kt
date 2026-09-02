package pt.antares.app.core.calc

import pt.antares.app.core.model.UnitSystem
import kotlin.math.roundToLong

/**
 * Que discos pôr de cada lado da barra.
 *
 * **Trabalha na unidade da pessoa, e não em quilos.** Um disco é um objeto físico com um
 * número pintado: quem levanta em libras tem discos de 45, e não discos de 20 kg a valerem
 * 44,09 lb. Converter o conjunto métrico daria uma lista de números que não existem em
 * ginásio nenhum — é o mesmo erro que as metas da corrida já evitam ao oferecerem 3/5/10 km
 * ou 1/3/5 mi, e não a conversão de umas nas outras.
 *
 * Os conjuntos são fixos nesta versão, e é decisão: uma calculadora que obriga a configurar
 * discos antes de servir para alguma coisa não serve ao primeiro toque, que é quando se
 * precisa dela — entre séries, com o telemóvel numa mão. Quem tem uma barra de 15 kg fica
 * para a versão que abrir as preferências de treino.
 */
object PlateMath {

    /** Barra olímpica métrica. */
    const val BARRA_KG = 20.0

    /** Barra olímpica imperial: 45 lb, que **não** são os 20 kg convertidos (seriam 44,09). */
    const val BARRA_LB = 45.0

    /** O conjunto de um ginásio comum, do maior para o menor. */
    val DISCOS_KG = listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    val DISCOS_LB = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    /**
     * @param porLado os discos de **um** dos lados, do maior para o menor — que é a ordem por
     *   que se enfiam.
     * @param sobra o que ficou por montar de cada lado, na unidade da pessoa. Zero na maior
     *   parte dos pesos; quando não é, o ecrã diz «≈» em vez de mentir que a conta fecha.
     */
    data class Carga(
        val barra: Double,
        val porLado: List<Double>,
        val sobra: Double,
    )

    /**
     * `null` quando o peso não chega para a barra: não é uma carga de zero discos, é uma
     * pergunta sem resposta — um exercício de 10 kg não se faz com uma barra de 20, e
     * desenhar a barra vazia dizia que sim.
     */
    fun paraOPeso(peso: Double, sistema: UnitSystem): Carga? {
        val barra = if (sistema == UnitSystem.IMPERIAL) BARRA_LB else BARRA_KG
        val discos = if (sistema == UnitSystem.IMPERIAL) DISCOS_LB else DISCOS_KG
        if (peso < barra) return null

        // Em centésimos e em inteiros: 2,5 + 1,25 em vírgula flutuante não dá 3,75 exacto, e
        // o resto acumulado passava a decidir se ainda cabe um disco de 1,25.
        //
        // E conta-se **o que falta nos dois lados**, não metade dele: a metade de um número
        // ímpar de centésimos não é um número de centésimos, e arredondá-la fazia a conta
        // fechar em 20,26 kg quando se pediram 20,25. Cada disco custa o dobro porque entra
        // aos pares — a barra tem dois lados, e carregar um só era outra coisa.
        var restaNosDoisLados = ((peso - barra) * CENTESIMOS).roundToLong()
        val postos = mutableListOf<Double>()

        for (disco in discos) {
            val par = (disco * CENTESIMOS * 2).roundToLong()
            while (restaNosDoisLados >= par) {
                postos += disco
                restaNosDoisLados -= par
            }
        }

        return Carga(barra = barra, porLado = postos, sobra = restaNosDoisLados / (CENTESIMOS * 2))
    }

    private const val CENTESIMOS = 100.0
}
