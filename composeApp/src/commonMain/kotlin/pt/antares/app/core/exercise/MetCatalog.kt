package pt.antares.app.core.exercise

data class MetActivity(
    val id: String,
    val namePt: String,
    val nameEn: String,
    val met: Double,
    val category: String,
)

class MetCatalog(val activities: List<MetActivity>) {

    fun byId(id: String): MetActivity? = activities.firstOrNull { it.id == id }

    fun categories(): List<String> = activities.map { it.category }.distinct()

    fun inCategory(category: String): List<MetActivity> = activities.filter { it.category == category }

    companion object {

        fun parse(csv: String): MetCatalog {
            val activities = csv.trim().lines()
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
