package pt.antares.app.core.privacy

import kotlinx.serialization.KSerializer

class ExportSource<T : Any>(

    val name: String,
    val serializer: KSerializer<T>,

    val restore: (suspend (List<T>) -> Unit)? = null,
    val rows: suspend () -> List<T>,
)
