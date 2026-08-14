package pt.antares.app.core.exercise

data class MetActivity(
    val id: String,
    val namePt: String,
    val nameEn: String,
    val met: Double,
    val category: String,
)

/**
 * A tabela de equivalentes metabólicos, lida de um CSV empacotado com a app. É o que
 * permite contar calorias de exercício sem rede e sem AI, e por isso é a origem por
 * omissão; a análise por texto só entra quando a atividade não está na tabela.
 */
class MetCatalog(val activities: List<MetActivity>) {

    fun byId(id: String): MetActivity? = activities.firstOrNull { it.id == id }

    // `distinct` sobre a lista preserva a ordem do ficheiro, que é a ordem em que as
    // categorias aparecem no seletor. Um conjunto ordenaria por outro critério qualquer.
    fun categories(): List<String> = activities.map { it.category }.distinct()

    fun inCategory(category: String): List<MetActivity> = activities.filter { it.category == category }

    companion object {

        // Linha malformada é saltada em silêncio, como na tabela da EFSA: perder uma
        // atividade tira uma opção da lista, rebentar aqui impede a app de arrancar.
        fun parse(csv: String): MetCatalog {
            val activities = csv.trim().lines()
                // Cabeçalho.
                .drop(1)
                .mapNotNull { line ->
                    val c = line.split(",")
                    if (c.size < 5) return@mapNotNull null
                    val met = c[3].trim().toDoubleOrNull() ?: return@mapNotNull null
                    val id = c[0].trim().ifBlank { return@mapNotNull null }
                    MetActivity(
                        id = id,
                        namePt = c[1].trim(),
                        nameEn = c[2].trim(),
                        met = met,
                        category = c[4].trim(),
                    )
                }
            return MetCatalog(activities)
        }
    }
}
