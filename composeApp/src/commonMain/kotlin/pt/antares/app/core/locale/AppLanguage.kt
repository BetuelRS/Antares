package pt.antares.app.core.locale

enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    PT("pt"),
    EN("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}
