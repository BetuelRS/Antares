package pt.antares.app.core.demo

internal class DemoRandom(seed: Long) {

    private var estado: Long = (seed xor 0x5DEECE66DL) or 1L

    private fun proximo(): Long {
        var x = estado
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        estado = x
        return x * -0x61c8864680b583ebL
    }

    fun fracao(): Double = (proximo() ushr 11).toDouble() / (1L shl 53).toDouble()

    fun entre(min: Double, max: Double): Double = min + fracao() * (max - min)

    fun ate(limite: Int): Int {
        require(limite > 0) { "limite tem de ser positivo" }
        return (fracao() * limite).toInt().coerceAtMost(limite - 1)
    }

    fun inteiroEntre(min: Int, max: Int): Int = min + ate(max - min + 1)

    fun chance(probabilidade: Double): Boolean = fracao() < probabilidade

    fun <T> um(de: List<T>): T? = if (de.isEmpty()) null else de[ate(de.size)]
}
