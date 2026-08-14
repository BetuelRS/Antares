package pt.antares.app.core.locale

/**
 * O idioma da app. `SYSTEM` é o que segue o telemóvel e é o valor por omissão; os outros
 * dois forçam, para quem tem o aparelho num idioma e quer a app noutro.
 */
enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    PT("pt"),
    EN("en");

    companion object {
        // Tudo o que não reconhece cai em `SYSTEM`: uma preferência gravada por uma versão
        // com mais idiomas não pode deixar a app sem nenhum.
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}
