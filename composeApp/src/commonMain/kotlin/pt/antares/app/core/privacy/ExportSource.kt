package pt.antares.app.core.privacy

import kotlinx.serialization.KSerializer

/**
 * Uma tabela que sai na exportação. A lista destas fontes é a definição do que a app
 * considera dados pessoais, e o `GdprTableParityTest` compara-a com as tabelas da base —
 * uma tabela nova que fique de fora da exportação faz o teste falhar.
 */
class ExportSource<T : Any>(

    // Vira a chave no JSON e o nome do ficheiro CSV. Mudar isto parte cópias já feitas.
    val name: String,
    val serializer: KSerializer<T>,

    // Nulo para o que se exporta mas não se reimporta. Sem ele, a importação salta a
    // tabela em silêncio — ver [BackupImporter.aplicar].
    val restore: (suspend (List<T>) -> Unit)? = null,
    val rows: suspend () -> List<T>,
)
