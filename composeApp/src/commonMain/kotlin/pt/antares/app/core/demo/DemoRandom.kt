package pt.antares.app.core.demo

/**
 * Gerador pseudoaleatório próprio, e não o `kotlin.random`. A demonstração tem de ser
 * reproduzível entre plataformas e entre versões da linguagem: um gerador da biblioteca
 * pode mudar de implementação, e a mesma semente deixaria de dar os mesmos dados.
 */
internal class DemoRandom(seed: Long) {

    // O `or 1` garante estado não nulo: com zero, este gerador fica preso em zero para
    // sempre. A constante é a mesma que a biblioteca padrão usa para espalhar a semente.
    private var estado: Long = (seed xor 0x5DEECE66DL) or 1L

    // Xorshift de 64 bits, seguido de uma multiplicação por uma constante ímpar grande que
    // mistura os bits altos — sozinho, o xorshift deixa padrões visíveis nos bits baixos.
    private fun proximo(): Long {
        var x = estado
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        estado = x
        return x * -0x61c8864680b583ebL
    }

    // Descarta os 11 bits baixos e usa os 53 restantes, que é a precisão exata da mantissa
    // de um Double. Assim a fração fica sempre em [0, 1) sem nunca chegar a 1.
    fun fracao(): Double = (proximo() ushr 11).toDouble() / (1L shl 53).toDouble()

    fun entre(min: Double, max: Double): Double = min + fracao() * (max - min)

    /** Inteiro em `0 until limite`. */
    fun ate(limite: Int): Int {
        require(limite > 0) { "limite tem de ser positivo" }
        // O `coerceAtMost` protege do arredondamento em vírgula flutuante devolver o
        // próprio limite, o que daria um índice fora da lista.
        return (fracao() * limite).toInt().coerceAtMost(limite - 1)
    }

    fun inteiroEntre(min: Int, max: Int): Int = min + ate(max - min + 1)

    fun chance(probabilidade: Double): Boolean = fracao() < probabilidade

    // Devolve null em lista vazia em vez de lançar: sem catálogo semeado, a demonstração
    // salta o alimento em vez de rebentar a geração.
    fun <T> um(de: List<T>): T? = if (de.isEmpty()) null else de[ate(de.size)]
}
